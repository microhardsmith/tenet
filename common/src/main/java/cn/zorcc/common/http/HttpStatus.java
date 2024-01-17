package cn.zorcc.common.http;

import cn.zorcc.common.Constants;
import cn.zorcc.common.structure.WriteBuffer;

import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *   Http status codes
 */
public enum HttpStatus {
    OK("200", "OK"),
    BAD_REQUEST("400", "Bad Request"),
    UNAUTHORIZED("401", "Unauthorized"),
    FORBIDDEN("403", "Forbidden"),
    NOT_FOUND("404", "Not Found"),
    METHOD_NOT_ALLOWED("405", "Method Not Allowed"),
    NOT_ACCEPTABLE("406", "Not Found"),
    INTERNAL_SERVER_ERR("500", "Internal Server Error");

    private static final Map<HttpStatus, MemorySegment> statusMap;

    static {
        statusMap = Arrays.stream(HttpStatus.values()).collect(Collectors.toMap(a -> a, b -> {
            try(WriteBuffer writeBuffer = WriteBuffer.newHeapWriteBuffer()) {
                writeBuffer.writeBytes(b.code.getBytes(StandardCharsets.UTF_8));
                writeBuffer.writeByte(Constants.SPACE);
                writeBuffer.writeBytes(b.description.getBytes(StandardCharsets.UTF_8));
                return writeBuffer.toSegment();
            }
        }));
    }

    public static MemorySegment getHttpStatusSegment(HttpStatus httpStatus) {
        return statusMap.get(httpStatus);
    }

    private final String code;
    private final String description;

    HttpStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String code() {
        return code;
    }

    public String description() {
        return description;
    }
}
