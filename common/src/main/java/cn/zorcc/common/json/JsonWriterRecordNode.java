package cn.zorcc.common.json;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Record;
import cn.zorcc.common.RecordInfo;
import cn.zorcc.common.WriteBuffer;

import java.util.List;

public final class JsonWriterRecordNode extends JsonWriterNode {
    private final WriteBuffer writeBuffer;
    private final Record<?> record;
    private final Object obj;
    private int index = 0;

    public JsonWriterRecordNode(WriteBuffer writeBuffer, Object obj, Class<?> type) {
        this.writeBuffer = writeBuffer;
        this.record = Record.of(type);
        this.obj = obj;
        writeBuffer.writeByte(Constants.LCB);
    }

    @Override
    protected JsonWriterNode trySerialize() {
        final List<RecordInfo> recordInfoList = record.recordInfoList();
        while (index < recordInfoList.size()) {
            RecordInfo recordInfo = recordInfoList.get(index);
            index = index + 1;
            Object value = recordInfo.getter().apply(obj);
            if(value != null) {
                writeSep(writeBuffer);
                writeKey(writeBuffer, recordInfo.fieldName());
                JsonWriterNode appended = writeValue(writeBuffer, value, recordInfo.format());
                if(appended != null) {
                    return appended;
                }
            }
        }
        writeBuffer.writeByte(Constants.RCB);
        return toPrev();
    }
}
