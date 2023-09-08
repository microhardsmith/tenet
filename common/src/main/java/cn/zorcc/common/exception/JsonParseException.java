package cn.zorcc.common.exception;

import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.anno.Format;

public class JsonParseException extends RuntimeException {
    public JsonParseException(String errMsg) {
        super(errMsg);
    }

    public JsonParseException(ReadBuffer readBuffer) {
        super("Failed to parse json, content : %s, index : %d".formatted(readBuffer, readBuffer.readIndex()));
    }

    public JsonParseException(Format format, Object value) {
        super("Failed to parse target format : %s, value : %s".formatted(format, value));
    }
}
