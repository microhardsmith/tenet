package cn.zorcc.common.exception;

import cn.zorcc.common.ReadBuffer;

public class JsonParseException extends RuntimeException {
    public JsonParseException(ReadBuffer readBuffer) {
        super("Failed to parse json, content : %s, index : %d".formatted(readBuffer, readBuffer.readIndex()));
    }

    public JsonParseException(String errMsg) {
        super(errMsg);
    }
}
