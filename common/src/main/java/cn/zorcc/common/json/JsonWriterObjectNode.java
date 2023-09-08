package cn.zorcc.common.json;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Meta;
import cn.zorcc.common.MetaInfo;
import cn.zorcc.common.Writer;

import java.util.List;

public final class JsonWriterObjectNode extends JsonNode {
    private final Writer writer;
    private final Meta<?> meta;
    private final Object obj;
    private int index = Constants.ZERO;
    private boolean notFirst = false;

    public JsonWriterObjectNode(Writer writer, Object obj, Class<?> type) {
        this.writer = writer;
        this.meta = Meta.of(type);
        this.obj = obj;
        writer.writeByte(Constants.LCB);
    }

    @Override
    public JsonNode process() {
        if(index == meta.metaInfoList().size()) {
            return processPrev();
        }else {
            return processCurrent();
        }
    }

    private JsonNode processCurrent() {
        final List<MetaInfo> metaInfoList = meta.metaInfoList();
        while (index < metaInfoList.size()) {
            MetaInfo metaInfo = metaInfoList.get(index);
            index = index + 1;
            Object value = metaInfo.getter().apply(obj);
            if(value != null) {
                JsonParser.writeKey(writer, metaInfo.fieldName(), notFirst);
                notFirst = true;
                JsonNode appended = JsonParser.writeValue(this, writer, value, metaInfo.format());
                if(appended != null) {
                    return appended;
                }
            }
        }
        return processPrev();
    }

    private JsonNode processPrev() {
        writer.writeByte(Constants.RCB);
        return toPrev();
    }
}
