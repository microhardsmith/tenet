package cn.zorcc.common.json;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Writer;

import java.util.Iterator;
import java.util.Map;

/**
 *   Represent a Map structure when writing into target writer
 *   Note that using Map structure for serialization won't be able to assign the target Format for built-in types like Integer or String
 */
public final class JsonWriterMapNode extends JsonNode {
    private final Writer writer;
    private final Iterator<? extends Map.Entry<?, ?>> iterator;
    private boolean notFirst = false;

    public JsonWriterMapNode(Writer writer, Map<?, ?> map) {
        this.writer = writer;
        this.iterator = map.entrySet().iterator();
    }

    @Override
    public JsonNode process() {
        if(iterator.hasNext()) {
            return processCurrent();
        }else {
            return processPrev();
        }
    }

    private JsonNode processCurrent() {
        while (iterator.hasNext()) {
            Map.Entry<?, ?> entry = iterator.next();
            JsonParser.writeKey(writer, entry.getKey().toString(), notFirst);
            notFirst = true;
            JsonNode appended = JsonParser.writeValue(this, writer, entry.getValue());
            if(appended != null) {
                return appended;
            }
        }
        return processPrev();
    }

    private JsonNode processPrev() {
        writer.writeByte(Constants.RCB);
        return toPrev();
    }
}
