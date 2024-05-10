package cn.zorcc.common.exception;

import cn.zorcc.common.Format;
import cn.zorcc.common.structure.ReadBuffer;

public final class JsonParseException extends RuntimeException {
    public JsonParseException(String errMsg) {
        super(errMsg);
    }

    public JsonParseException(ReadBuffer readBuffer) {
        super(STR."Failed to parse json, content : \{readBuffer}, index : \{readBuffer.currentIndex()}");
    }

    public JsonParseException(Format format, Object value) {
        super(STR."Failed to parse target format : \{format}, value : \{value}");
    }
}
