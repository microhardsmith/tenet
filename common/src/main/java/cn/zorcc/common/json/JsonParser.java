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

    public static void serializeAsObject(WriteBuffer writeBuffer, Object target) {
        Class<?> targetClass = target.getClass();
        if(targetClass.isPrimitive() || targetClass.isAnnotation() || targetClass.isRecord() || targetClass.isMemberClass()) {
            throw new FrameworkException(ExceptionType.JSON, "Unsupported type");
        }
        JsonWriterObjectNode jsonWriterObjectNode = new JsonWriterObjectNode(writeBuffer, target, targetClass);
        jsonWriterObjectNode.serialize();
    }

    public static <T> void serializeAsCollection(WriteBuffer writeBuffer, Collection<T> collection) {
        serializeAsCollection(writeBuffer, collection, null);
    }

    public static <T> void serializeAsCollection(WriteBuffer writeBuffer, Collection<T> collection, Format format) {
        new JsonWriterCollectionNode(writeBuffer, collection.iterator(), format).serialize();
    }

    /**
     *   Deserialize a object from the data
     */
    @SuppressWarnings("unchecked")
    public static <T> T deserializeAsObject(ReadBuffer readBuffer, Class<T> type) {
        if(readBuffer.readByte() != Constants.LCB) {
            throw new JsonParseException(readBuffer);
        }
        JsonReaderObjectNode jsonReaderObjectNode = new JsonReaderObjectNode(readBuffer, type);
        jsonReaderObjectNode.deserialize();
        return (T) jsonReaderObjectNode.getJsonObject();
    }

    /**
     * Deserialize a list from the data
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> deserializeAsList(ReadBuffer readBuffer, JsonTypeRef<T> typeRef) {
        if(readBuffer.readByte() != Constants.LSB) {
            throw new JsonParseException(readBuffer);
        }
        JsonReaderCollectionNode jsonReaderCollectionNode = new JsonReaderCollectionNode(readBuffer, List.class, typeRef.type());
        jsonReaderCollectionNode.deserialize();
        return (List<T>) jsonReaderCollectionNode.getJsonObject();
    }
}
