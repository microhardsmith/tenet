package cn.zorcc.common.json;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Meta;
import cn.zorcc.common.MetaInfo;
import cn.zorcc.common.structure.WriteBuffer;

import java.util.List;

public final class JsonWriterObjectNode extends JsonWriterNode {
    private final WriteBuffer writeBuffer;
    private final Meta<?> meta;
    private final Object obj;
    private int index = 0;

    public JsonWriterObjectNode(WriteBuffer writeBuffer, Object obj, Class<?> type) {
        this.writeBuffer = writeBuffer;
        this.meta = JsonParser.getMeta(type);
        this.obj = obj;
        writeBuffer.writeByte(Constants.LCB);
    }

    @Override
    protected JsonWriterNode trySerialize() {
        final List<MetaInfo> metaInfoList = meta.metaInfoList();
        while (index < metaInfoList.size()) {
            MetaInfo metaInfo = metaInfoList.get(index);
            index = index + 1;
            Object value = metaInfo.getter().apply(obj);
            if(value != null) {
                writeSep(writeBuffer);
                writeKey(writeBuffer, metaInfo.fieldName());
                JsonWriterNode appended = writeValue(writeBuffer, value, metaInfo.format());
                if(appended != null) {
                    return appended;
                }
            }
        }
        writeBuffer.writeByte(Constants.RCB);
        return toPrev();
    }
}
