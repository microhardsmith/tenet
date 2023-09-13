package cn.zorcc.common.json;

import cn.zorcc.common.Constants;
import cn.zorcc.common.WriteBuffer;

import java.util.Iterator;
import java.util.Map;

/**
 *   Represent a Map structure when writing into target writer
 *   Note that using Map structure for serialization won't be able to assign the target Format for built-in types like Integer or String
 */
public final class JsonWriterMapNode extends JsonWriterNode {
    private final WriteBuffer writeBuffer;
    private final Iterator<? extends Map.Entry<?, ?>> iterator;

    public JsonWriterMapNode(WriteBuffer writeBuffer, Map<?, ?> map) {
        this.writeBuffer = writeBuffer;
        this.iterator = map.entrySet().iterator();
        writeBuffer.writeByte(Constants.LCB);
    }

    @Override
    protected JsonWriterNode trySerialize() {
        while (iterator.hasNext()) {
            Map.Entry<?, ?> entry = iterator.next();
            writeKey(writeBuffer, entry.getKey().toString());
            JsonWriterNode appended = writeValue(writeBuffer, entry.getValue());
            if(appended != null) {
                return appended;
            }
        }
        writeBuffer.writeByte(Constants.RCB);
        return toPrev();
    }
}
