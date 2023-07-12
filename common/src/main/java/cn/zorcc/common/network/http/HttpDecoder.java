package cn.zorcc.common.network.http;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.Decoder;

import java.nio.charset.StandardCharsets;

public class HttpDecoder implements Decoder {
    private enum DecodingStatus {
        INITIAL,
        DECODING_URI,
        DECODING_VERSION,
        DECODING_HEADER,
        DECODING_DATA
    }

    private enum ResultStatus {
        CONTINUE,
        FINISHED,
        INCOMPLETE
    }
    private static final String CONTENT_LENGTH = "Content-Length";
    private DecodingStatus decodingStatus;
    private HttpReq current;
    @Override
    public Object decode(ReadBuffer readBuffer) {
        for( ; ; ) {
            switch (tryDecode(readBuffer)) {
                case FINISHED -> {
                    return current;
                }
                case INCOMPLETE -> {
                    return null;
                }
            }
        }
    }

    /**
     *   decode http buffer, using state machine mechanism
     *   if content is not complete, return INCOMPLETE
     *   if needs to decode again, return CONTINUE
     *   if current request if fully decoded, return FINISHED
     */
    private ResultStatus tryDecode(ReadBuffer readBuffer) {
        return switch (decodingStatus) {
            case INITIAL -> tryDecodeInitial(readBuffer);
            case DECODING_URI -> tryDecodeUri(readBuffer);
            case DECODING_VERSION -> tryDecodeVersion(readBuffer);
            case DECODING_HEADER -> tryDecodeHeader(readBuffer);
            case DECODING_DATA -> tryDecodeData(readBuffer);
        };
    }

    private ResultStatus tryDecodeInitial(ReadBuffer readBuffer) {
        current = new HttpReq();
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
            default -> throw new FrameworkException(ExceptionType.HTTP, "Unknown method");
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
        if (bytes == null) {
            return ResultStatus.INCOMPLETE;
        } else if (bytes == Constants.EMPTY_BYTES) {
            decodingStatus = DecodingStatus.DECODING_DATA;
            return ResultStatus.CONTINUE;
        }
        for (int i = 0; i < bytes.length - 1; i++) {
            if (bytes[i] == Constants.COLON && bytes[i + 1] == Constants.SPACE) {
                String key = new String(bytes, 0, i);
                String value = new String(bytes, i + 2, bytes.length - i - 2);
                current.getHeaders().put(key, value);
                return ResultStatus.CONTINUE;
            }
        }
        throw new FrameworkException(ExceptionType.HTTP, "Http Header wrong format");
    }

    private ResultStatus tryDecodeData(ReadBuffer readBuffer) {
        String contentLength = current.getHeaders().get(CONTENT_LENGTH);
        if (contentLength != null) {
            int len = Integer.parseInt(contentLength);
            if (readBuffer.size() - readBuffer.readIndex() < len) {
                return ResultStatus.INCOMPLETE;
            }
            current.setData(readBuffer.readBytes(len));
        }
        decodingStatus = DecodingStatus.INITIAL;
        return ResultStatus.FINISHED;
    }
}
