package cn.zorcc.common.json;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Format;
import cn.zorcc.common.structure.WriteBuffer;

import java.util.Iterator;

public class JsonWriterCollectionNode extends JsonWriterNode {
    private final WriteBuffer writeBuffer;
    private final Iterator<?> iterator;
    private final Format format;

    public JsonWriterCollectionNode(WriteBuffer writeBuffer, Iterator<?> iterator, Format format) {
        this.writeBuffer = writeBuffer;
        this.iterator = iterator;
        this.format = format;
        writeBuffer.writeByte(Constants.LSB);
    }

    @Override
    protected JsonWriterNode trySerialize() {
        while (iterator.hasNext()) {
            writeSep(writeBuffer);
            Object element = iterator.next();
            JsonWriterNode jsonNode = writeValue(writeBuffer, element, format);
            if(jsonNode != null) {
                return jsonNode;
            }
        }
        writeBuffer.writeByte(Constants.RSB);
        return toPrev();
    }
}
