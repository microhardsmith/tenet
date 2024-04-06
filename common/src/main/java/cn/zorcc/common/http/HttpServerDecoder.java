package cn.zorcc.common.http;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.Decoder;
import cn.zorcc.common.network.Poller;
import cn.zorcc.common.structure.ReadBuffer;
import cn.zorcc.common.structure.WriteBuffer;
import cn.zorcc.common.util.CompressUtil;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class HttpServerDecoder implements Decoder {
    private enum DecodingStatus {
        INITIAL,
        DECODING_URI,
        DECODING_VERSION,
        DECODING_HEADER,
        DECODING_FIXED_DATA,
        DECODING_CHUNKED_DATA_LENGTH,
        DECODING_CHUNKED_DATA,
        DECODING_CHUNKED_FINAL
    }

    private enum ResultStatus {
        CONTINUE,
        FINISHED,
        INCOMPLETE
    }

    private static final long SPACE_PATTERN = ReadBuffer.compilePattern(Constants.SPACE);
    private static final long CR_PATTERN = ReadBuffer.compilePattern(Constants.CR);
    private static final long COLON_PATTERN = ReadBuffer.compilePattern(Constants.COLON);
    private static final long CHUNKED_DATA_INITIAL_SIZE = 4 * Constants.KB;
    private DecodingStatus decodingStatus = DecodingStatus.INITIAL;
    private int len;
    private WriteBuffer tempBuffer;
    private HttpRequest current;
    @Override
    public void decode(ReadBuffer readBuffer, List<Object> entityList) {
        for( ; ; ) {
            switch (tryDecode(readBuffer)) {
                case FINISHED -> {
                    entityList.add(current);
                    current = null; // help GC
                    if(readBuffer.available() > 0L) {
                        throw new FrameworkException(ExceptionType.HTTP, "Http pipeline was not supported");
                    }
                    return ;
                }
                case INCOMPLETE -> {
                    return ;
                }
            }
        }
    }

    private ResultStatus tryDecode(ReadBuffer readBuffer) {
        return switch (decodingStatus) {
            case INITIAL -> tryDecodeInitial(readBuffer);
            case DECODING_URI -> tryDecodeUri(readBuffer);
            case DECODING_VERSION -> tryDecodeVersion(readBuffer);
            case DECODING_HEADER -> tryDecodeHeader(readBuffer);
            case DECODING_FIXED_DATA -> tryDecodeFixedData(readBuffer);
            case DECODING_CHUNKED_DATA -> tryDecodeChunkedData(readBuffer);
            case DECODING_CHUNKED_DATA_LENGTH -> tryDecodeChunkedDataLen(readBuffer);
            case DECODING_CHUNKED_FINAL -> tryDecodeChunkedDataFinal(readBuffer);
        };
    }

    private ResultStatus tryDecodeInitial(ReadBuffer readBuffer) {
        current = new HttpRequest();
        MemorySegment segment = readBuffer.readPattern(SPACE_PATTERN, Constants.SPACE);
        if (segment == null) {
            return ResultStatus.INCOMPLETE;
        } else if(segment == MemorySegment.NULL) {
            throw new FrameworkException(ExceptionType.HTTP, "Unresolved http method");
        }
        String methodStr = new String(segment.toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8);
        switch (methodStr) {
            case "GET" -> current.setMethod(HttpMethod.Get);
            case "POST" -> current.setMethod(HttpMethod.Post);
            case "PUT" -> current.setMethod(HttpMethod.Put);
            case "DELETE" -> current.setMethod(HttpMethod.Delete);
            case "PATCH" -> current.setMethod(HttpMethod.Patch);
            case "OPTIONS" -> current.setMethod(HttpMethod.Options);
            default -> throw new FrameworkException(ExceptionType.HTTP, STR."Unknown method : \{methodStr}");
        }
        decodingStatus = DecodingStatus.DECODING_URI;
        return ResultStatus.CONTINUE;
    }

    private ResultStatus tryDecodeUri(ReadBuffer readBuffer) {
        MemorySegment segment = readBuffer.readPattern(SPACE_PATTERN, Constants.SPACE);
        if (segment == null) {
            return ResultStatus.INCOMPLETE;
        } else if (segment == MemorySegment.NULL) {
            throw new FrameworkException(ExceptionType.HTTP, "Unresolved http uri");
        }
        current.setUri(new String(segment.toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8));
        decodingStatus = DecodingStatus.DECODING_VERSION;
        return ResultStatus.CONTINUE;
    }

    private ResultStatus tryDecodeVersion(ReadBuffer readBuffer) {
        MemorySegment segment = readBuffer.readPattern(CR_PATTERN, Constants.CR, Constants.LF);
        if (segment == null) {
            return ResultStatus.INCOMPLETE;
        } else if (segment == MemorySegment.NULL) {
            throw new FrameworkException(ExceptionType.HTTP, "Unresolved http version");
        }
        current.setVersion(new String(segment.toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8));
        decodingStatus = DecodingStatus.DECODING_HEADER;
        return ResultStatus.CONTINUE;
    }

    private ResultStatus tryDecodeHeader(ReadBuffer readBuffer) {
        MemorySegment segment = readBuffer.readPattern(CR_PATTERN, Constants.CR, Constants.LF);
        HttpHeader httpHeader = current.getHttpHeader();
        if (segment == null) {
            return ResultStatus.INCOMPLETE;
        } else if (segment == MemorySegment.NULL) {
            String contentLength = httpHeader.get(HttpHeader.K_CONTENT_LENGTH);
            if(contentLength != null) {
                len = Integer.parseInt(contentLength);
                decodingStatus = DecodingStatus.DECODING_FIXED_DATA;
                return ResultStatus.CONTINUE;
            }
            String transferEncoding = httpHeader.get(HttpHeader.K_TRANSFER_ENCODING);
            if(HttpHeader.V_CHUNKED.equals(transferEncoding)) {
                // creating a temp buffer area to store the chunked data would be wise
                tempBuffer = WriteBuffer.newNativeWriteBuffer(Poller.localMemApi(), CHUNKED_DATA_INITIAL_SIZE);
                decodingStatus = DecodingStatus.DECODING_CHUNKED_DATA_LENGTH;
                return ResultStatus.CONTINUE;
            }
            decodingStatus = DecodingStatus.INITIAL;
            return ResultStatus.FINISHED;
        }
        long splitIndex = ReadBuffer.patternSearch(segment, 0L, segment.byteSize(), COLON_PATTERN, Constants.COLON, Constants.SPACE);
        if(splitIndex < 0) {
            throw new FrameworkException(ExceptionType.HTTP, "Http Header wrong format");
        }
        String key = new String(segment.asSlice(0, splitIndex).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8);
        String value = new String(segment.asSlice(splitIndex + 2, segment.byteSize() - splitIndex - 2).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8);
        httpHeader.put(key, value);
        return ResultStatus.CONTINUE;
    }

    private ResultStatus tryDecodeFixedData(ReadBuffer readBuffer) {
        long available = readBuffer.available();
        if (available < len) {
            return ResultStatus.INCOMPLETE;
        }
        current.setData(assignData(readBuffer.readSegment(len)));
        decodingStatus = DecodingStatus.INITIAL;
        return ResultStatus.FINISHED;
    }


    private ResultStatus tryDecodeChunkedDataFinal(ReadBuffer readBuffer) {
        MemorySegment segment = readBuffer.readPattern(CR_PATTERN, Constants.CR, Constants.LF);
        if(segment == null) {
            return ResultStatus.INCOMPLETE;
        }else if (segment == MemorySegment.NULL) {
            current.setData(assignData(tempBuffer.asSegment()));
            tempBuffer.close();
            tempBuffer = null; // help GC
            decodingStatus = DecodingStatus.INITIAL;
            return ResultStatus.FINISHED;
        }else {
            throw new FrameworkException(ExceptionType.HTTP, "Unresolved http chunked data termination");
        }
    }

    private ResultStatus tryDecodeChunkedDataLen(ReadBuffer readBuffer) {
        MemorySegment segment = readBuffer.readPattern(CR_PATTERN, Constants.CR, Constants.LF);
        if(segment == null) {
            return ResultStatus.INCOMPLETE;
        }else if (segment == MemorySegment.NULL) {
            throw new FrameworkException(ExceptionType.HTTP, "Unresolved http chunked data length");
        }
        len = getTransferredLength(segment);
        decodingStatus = len == 0 ? DecodingStatus.DECODING_CHUNKED_FINAL : DecodingStatus.DECODING_CHUNKED_DATA;
        return ResultStatus.CONTINUE;
    }

    private ResultStatus tryDecodeChunkedData(ReadBuffer readBuffer) {
        if (readBuffer.size() - readBuffer.readIndex() < len) {
            return ResultStatus.INCOMPLETE;
        }
        MemorySegment data = readBuffer.readSegment(len);
        if(readBuffer.readPattern(CR_PATTERN, Constants.CR, Constants.LF) != MemorySegment.NULL) {
            throw new FrameworkException(ExceptionType.HTTP, "Unresolved http chunked data");
        }
        tempBuffer.writeSegment(data);
        decodingStatus = DecodingStatus.DECODING_CHUNKED_DATA_LENGTH;
        return ResultStatus.CONTINUE;
    }

    private static int getTransferredLength(MemorySegment segment) {
        int ret = 0;
        int len = Math.toIntExact(segment.byteSize());
        for (int i = 0; i < len; i++) {
            byte b = NativeUtil.getByte(segment, i);
            if(b >= Constants.B_ZERO && b <= Constants.B_NINE) {
                ret = (ret << 4) + b - Constants.B_ZERO;
            }else if(b >= Constants.B_a && b <= Constants.B_f) {
                ret = (ret << 4) + b - Constants.B_a + 10;
            }else if(b >= Constants.B_A && b <= Constants.B_F) {
                ret = (ret << 4) + b - Constants.B_A + 10;
            }else {
                throw new FrameworkException(ExceptionType.HTTP, "Unresolved http chunked data length");
            }
        }
        return ret;
    }

    /**
     *   Assigning data to current HttpRequest, the rawData and compression data would be required as Native memory, and the returned MemorySegment would be guaranteed to be on-heap memory
     */
    private MemorySegment assignData(MemorySegment rawData) {
        assert rawData.isNative();
        return switch (current.getHttpHeader().get(HttpHeader.K_CONTENT_ENCODING)) {
            case null -> NativeUtil.toHeap(rawData);
            case HttpHeader.V_GZIP ->
                    CompressUtil.decompressUsingGzip(rawData, Poller.localMemApi());
            case HttpHeader.V_DEFLATE ->
                    CompressUtil.decompressUsingDeflate(rawData, Poller.localMemApi());
            case HttpHeader.V_BR ->
                    CompressUtil.decompressUsingBrotli(rawData, Poller.localMemApi());
            default -> throw new FrameworkException(ExceptionType.HTTP, "Unsupported compression type detected");
        };
    }
}
