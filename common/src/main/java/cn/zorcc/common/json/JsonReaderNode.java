package cn.zorcc.common.json;

import cn.zorcc.common.Constants;
import cn.zorcc.common.exception.JsonParseException;

public abstract class JsonReaderNode extends JsonNode {
    protected abstract void assign(Object value);

    protected abstract Object construct();

    @Override
    protected JsonNode toPrev() {
        JsonNode prevNode = super.toPrev();
        if(prevNode instanceof JsonReaderNode readerNode) {
            readerNode.assign(construct());
            return readerNode;
        }else {
            throw new JsonParseException(Constants.UNREACHED);
        }
    }

    @Override
    public JsonNode process() {
        return null;
    }
}
