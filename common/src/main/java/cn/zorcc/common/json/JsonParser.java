package cn.zorcc.common.json;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.anno.Format;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.exception.JsonParseException;

import java.util.Collection;
import java.util.List;

public final class JsonParser {
    private JsonParser() {
        throw new UnsupportedOperationException();
    }

    /**
     *   Serialize a record or a plain object into the target writeBuffer
     */
    public static void serializeAsObject(WriteBuffer writeBuffer, Object target) {
        Class<?> targetClass = target.getClass();
        if(targetClass.isPrimitive() || targetClass.isAnnotation() || targetClass.isMemberClass()) {
            throw new FrameworkException(ExceptionType.JSON, "Unsupported type");
        }
        JsonWriterNode jsonWriterNode = targetClass.isRecord() ? new JsonWriterRecordNode(writeBuffer, target, targetClass) : new JsonWriterObjectNode(writeBuffer, target, targetClass);
        jsonWriterNode.serialize();
    }

    public static <T> void serializeAsCollection(WriteBuffer writeBuffer, Collection<T> collection) {
        serializeAsCollection(writeBuffer, collection, null);
    }

    public static <T> void serializeAsCollection(WriteBuffer writeBuffer, Collection<T> collection, Format format) {
        JsonWriterNode jsonWriterNode = new JsonWriterCollectionNode(writeBuffer, collection.iterator(), format);
        jsonWriterNode.serialize();
    }

    /**
     *   Deserialize a record or a plain object from the readBuffer
     */
    @SuppressWarnings("unchecked")
    public static <T> T deserializeAsObject(ReadBuffer readBuffer, Class<T> type) {
        if(readBuffer.readByte() != Constants.LCB) {
            throw new JsonParseException(readBuffer);
        }
        JsonReaderNode jsonReaderNode = type.isRecord() ? new JsonReaderRecordNode(readBuffer, type) : new JsonReaderObjectNode(readBuffer, type);
        jsonReaderNode.deserialize();
        return (T) jsonReaderNode.getJsonObject();
    }

    /**
     * Deserialize a list from the data
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> deserializeAsList(ReadBuffer readBuffer, JsonTypeRef<T> typeRef) {
        if(readBuffer.readByte() != Constants.LSB) {
            throw new JsonParseException(readBuffer);
        }
        JsonReaderNode jsonReaderNode = new JsonReaderCollectionNode(readBuffer, List.class, typeRef.type());
        jsonReaderNode.deserialize();
        return (List<T>) jsonReaderNode.getJsonObject();
    }
}
