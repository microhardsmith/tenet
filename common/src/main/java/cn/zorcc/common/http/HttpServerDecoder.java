package cn.zorcc.common.http;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.Decoder;
import cn.zorcc.common.network.Poller;
import cn.zorcc.common.structure.Allocator;
import cn.zorcc.common.structure.ReadBuffer;
import cn.zorcc.common.structure.WriteBuffer;
import cn.zorcc.common.util.CompressUtil;

import java.lang.foreign.MemorySegment;
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
        byte[] bytes = readBuffer.readUntil(Constants.SPACE);
        if (bytes == null) {
            return ResultStatus.INCOMPLETE;
        }
        String methodStr = new String(bytes, StandardCharsets.UTF_8);
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
        byte[] bytes = readBuffer.readUntil(Constants.SPACE);
        if (bytes == null) {
            return ResultStatus.INCOMPLETE;
        } else if (bytes == Constants.EMPTY_BYTES) {
            throw new FrameworkException(ExceptionType.HTTP, "Unresolved http uri");
        }
        current.setUri(new String(bytes, StandardCharsets.UTF_8));
        decodingStatus = DecodingStatus.DECODING_VERSION;
        return ResultStatus.CONTINUE;
    }

    private ResultStatus tryDecodeVersion(ReadBuffer readBuffer) {
        byte[] bytes = readBuffer.readUntil(Constants.CR, Constants.LF);
        if (bytes == null) {
            return ResultStatus.INCOMPLETE;
        } else if (bytes == Constants.EMPTY_BYTES) {
            throw new FrameworkException(ExceptionType.HTTP, "Unresolved http version");
        }
        current.setVersion(new String(bytes, StandardCharsets.UTF_8));
        decodingStatus = DecodingStatus.DECODING_HEADER;
        return ResultStatus.CONTINUE;
    }

    private ResultStatus tryDecodeHeader(ReadBuffer readBuffer) {
        byte[] bytes = readBuffer.readUntil(Constants.CR, Constants.LF);
        HttpHeader httpHeader = current.getHttpHeader();
        if (bytes == null) {
            return ResultStatus.INCOMPLETE;
        } else if (bytes == Constants.EMPTY_BYTES) {
            String contentLength = httpHeader.get(HttpHeader.K_CONTENT_LENGTH);
            if(contentLength != null) {
                len = Integer.parseInt(contentLength);
                decodingStatus = DecodingStatus.DECODING_FIXED_DATA;
                return ResultStatus.CONTINUE;
            }
            String transferEncoding = httpHeader.get(HttpHeader.K_TRANSFER_ENCODING);
            if(HttpHeader.V_CHUNKED.equals(transferEncoding)){
                tempBuffer = WriteBuffer.newHeapWriteBuffer(4 * Constants.KB);
                decodingStatus = DecodingStatus.DECODING_CHUNKED_DATA_LENGTH;
                return ResultStatus.CONTINUE;
            }
            decodingStatus = DecodingStatus.INITIAL;
            return ResultStatus.FINISHED;
        }
        for (int i = 0; i < bytes.length - 1; i++) {
            if (bytes[i] == Constants.COLON && bytes[i + 1] == Constants.SPACE) {
                String key = new String(bytes, 0, i);
                String value = new String(bytes, i + 2, bytes.length - i - 2);
                httpHeader.put(key, value);
                return ResultStatus.CONTINUE;
            }
        }
        throw new FrameworkException(ExceptionType.HTTP, "Http Header wrong format");
    }

    private ResultStatus tryDecodeFixedData(ReadBuffer readBuffer) {
        if (readBuffer.size() - readBuffer.readIndex() < len) {
            return ResultStatus.INCOMPLETE;
        }
        current.setData(tryDecompress(readBuffer.readHeapSegment(len)));
        decodingStatus = DecodingStatus.INITIAL;
        return ResultStatus.FINISHED;
    }


    private ResultStatus tryDecodeChunkedDataFinal(ReadBuffer readBuffer) {
        byte[] bytes = readBuffer.readUntil(Constants.CR, Constants.LF);
        if(bytes == null) {
            return ResultStatus.INCOMPLETE;
        }else if (bytes == Constants.EMPTY_BYTES) {
            current.setData(tryDecompress(tempBuffer.asSegment()));
            decodingStatus = DecodingStatus.INITIAL;
            return ResultStatus.FINISHED;
        }else {
            throw new FrameworkException(ExceptionType.HTTP, "Unresolved http chunked data termination");
        }
    }

    private ResultStatus tryDecodeChunkedDataLen(ReadBuffer readBuffer) {
        byte[] bytes = readBuffer.readUntil(Constants.CR, Constants.LF);
        if(bytes == null) {
            return ResultStatus.INCOMPLETE;
        }else if (bytes == Constants.EMPTY_BYTES) {
            throw new FrameworkException(ExceptionType.HTTP, "Unresolved http chunked data length");
        }
        len = getTransferredLength(bytes);
        decodingStatus = len == 0 ? DecodingStatus.DECODING_CHUNKED_FINAL : DecodingStatus.DECODING_CHUNKED_DATA;
        return ResultStatus.CONTINUE;
    }

    private ResultStatus tryDecodeChunkedData(ReadBuffer readBuffer) {
        if (readBuffer.size() - readBuffer.readIndex() < len) {
            return ResultStatus.INCOMPLETE;
        }
        MemorySegment data = readBuffer.readSegment(len);
        if(readBuffer.readUntil(Constants.CR, Constants.LF) != Constants.EMPTY_BYTES) {
            throw new FrameworkException(ExceptionType.HTTP, "Unresolved http chunked data");
        }
        tempBuffer.writeSegment(data);
        decodingStatus = DecodingStatus.DECODING_CHUNKED_DATA_LENGTH;
        return ResultStatus.CONTINUE;
    }

    private static int getTransferredLength(byte[] bytes) {
        int ret = 0;
        for (byte b : bytes) {
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

    private MemorySegment tryDecompress(MemorySegment rawData) {
        return switch (current.getHttpHeader().get(HttpHeader.K_CONTENT_ENCODING)) {
            case null -> rawData;
            case HttpHeader.V_GZIP -> {
                try(Allocator allocator = Poller.localAllocator()) {
                    yield CompressUtil.decompressUsingGzip(rawData, allocator);
                }
            }
            case HttpHeader.V_DEFLATE -> {
                try(Allocator allocator = Poller.localAllocator()) {
                    yield CompressUtil.decompressUsingDeflate(rawData, allocator);
                }
            }
            default -> throw new FrameworkException(ExceptionType.HTTP, "Unsupported compression type detected");
        };
    }
}
