package cn.zorcc.common.network.http;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.Decoder;

import java.nio.charset.StandardCharsets;

public class HttpDecoder implements Decoder {
    private static final int INITIAL = 0;
    private static final int URI = 1;
    private static final int VERSION = 2;
    private static final int HEADER = 3;
    private static final int DATA = 4;
    private static final String CONTENT_LENGTH = "Content-Length";
    private HttpReq current;
    private int state = INITIAL;
    @Override
    public Object decode(ReadBuffer readBuffer) {
        int i;
        do{
            i = tryDecode(readBuffer);
        }while (i == 0);
        return i == 1 ? current : null;
    }

    /**
     *   decode http buffer, using state machine mechanism
     *   if content is not complete, return -1
     *   if needs to decode again, return 0
     *   if current request if fully decoded, return 1
     */
    private int tryDecode(ReadBuffer readBuffer) {
        switch (state) {
            case INITIAL -> {
                current = new HttpReq();
                byte[] bytes = readBuffer.readUntil(Constants.SPACE);
                if (bytes == null) {
                    return -1;
                } else if (bytes == Constants.EMPTY_BYTES) {
                    throw new FrameworkException(ExceptionType.HTTP, "Unresolved http method");
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
                state = URI;
                return 0;
            }
            case URI -> {
                byte[] bytes = readBuffer.readUntil(Constants.SPACE);
                if (bytes == null) {
                    return -1;
                } else if (bytes == Constants.EMPTY_BYTES) {
                    throw new FrameworkException(ExceptionType.HTTP, "Unresolved http uri");
                }
                current.setUri(new String(bytes, StandardCharsets.UTF_8));
                state = VERSION;
                return 0;
            }
            case VERSION -> {
                byte[] bytes = readBuffer.readUntil(Constants.CR, Constants.LF);
                if (bytes == null) {
                    return -1;
                } else if (bytes == Constants.EMPTY_BYTES) {
                    throw new FrameworkException(ExceptionType.HTTP, "Unresolved http version");
                }
                current.setVersion(new String(bytes, StandardCharsets.UTF_8));
                state = HEADER;
                return 0;
            }
            case HEADER -> {
                byte[] bytes = readBuffer.readUntil(Constants.CR, Constants.LF);
                if (bytes == null) {
                    return -1;
                } else if (bytes == Constants.EMPTY_BYTES) {
                    state = DATA;
                    return 0;
                }
                for (int i = 0; i < bytes.length; i++) {
                    if (bytes[i] == Constants.COLON && i + 1 < bytes.length && bytes[i + 1] == Constants.SPACE) {
                        String key = new String(bytes, 0, i);
                        String value = new String(bytes, i + 2, bytes.length - i - 2);
                        current.getHeaders().put(key, value);
                        return 0;
                    }
                }
                throw new FrameworkException(ExceptionType.HTTP, "Http Header wrong format");
            }
            case DATA -> {
                String contentLength = current.getHeaders().get(CONTENT_LENGTH);
                if (contentLength != null) {
                    int len = Integer.parseInt(contentLength);
                    if (readBuffer.size() - readBuffer.readIndex() < len) {
                        return -1;
                    }
                    current.setData(readBuffer.readBytes(len));
                }
                state = INITIAL;
                return 1;
            }
            default -> throw new FrameworkException(ExceptionType.HTTP, Constants.UNREACHED);
        }
    }
}
