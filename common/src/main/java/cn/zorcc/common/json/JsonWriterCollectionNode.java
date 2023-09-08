package cn.zorcc.common.json;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Writer;
import cn.zorcc.common.anno.Format;

import java.util.Iterator;

public class JsonWriterCollectionNode extends JsonNode {
    private final Writer writer;
    private final Iterator<?> iterator;
    private final Format format;
    private boolean notFirst = false;

    public JsonWriterCollectionNode(Writer writer, Iterator<?> iterator, Format format) {
        this.writer = writer;
        this.iterator = iterator;
        this.format = format;
        writer.writeByte(Constants.LSB);
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
            if(notFirst) {
                writer.writeByte(Constants.COMMA);
            }
            Object element = iterator.next();
            JsonNode jsonNode = JsonParser.writeValue(this, writer, element, format);
            notFirst = true;
            if(jsonNode != null) {
                return jsonNode;
            }
        }
        return processPrev();
    }

    private JsonNode processPrev() {
        writer.writeByte(Constants.RSB);
        return toPrev();
    }
}
