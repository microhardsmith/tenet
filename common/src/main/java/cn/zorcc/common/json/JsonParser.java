package cn.zorcc.common.json;

import cn.zorcc.common.Record;
import cn.zorcc.common.*;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.exception.JsonParseException;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *   JsonParser is a commonly used tool for transforming UTF-8 json data into Java object, or otherwise
 *   It's designed to be mostly used in parsing json data of small or medium size, like HTTP json body
 */
public final class JsonParser {
    private static final ScopedValue<Map<Class<?>, Object>> localCache = ScopedValue.newInstance();
    private JsonParser() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    public static <T> Meta<T> getMeta(Class<T> clazz) {
        Map<Class<?>, Object> localMap = localCache.get();
        if(localMap == null) {
            return Meta.of(clazz);
        }else {
            return (Meta<T>) localMap.computeIfAbsent(clazz, Meta::of);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Record<T> getRecord(Class<T> clazz) {
        Map<Class<?>, Object> localMap = localCache.get();
        if(localMap == null) {
            return Record.of(clazz);
        }else {
            return (Record<T>) localMap.computeIfAbsent(clazz, Record::of);
        }
    }

    /**
     *   Serialize a record or a plain object into the target writeBuffer in json format
     */
    public static void writeObject(WriteBuffer writeBuffer, Object target) {
        Class<?> targetClass = target.getClass();
        if(targetClass.isPrimitive() || targetClass.isAnnotation() || targetClass.isMemberClass()) {
            throw new FrameworkException(ExceptionType.JSON, "Unsupported type");
        }
        ScopedValue.runWhere(localCache, new HashMap<>(), () -> {
            JsonWriterNode jsonWriterNode = targetClass.isRecord() ? new JsonWriterRecordNode(writeBuffer, target, targetClass) : new JsonWriterObjectNode(writeBuffer, target, targetClass);
            jsonWriterNode.serialize();
        });
    }

    public static <T> void writeCollection(WriteBuffer writeBuffer, Collection<T> collection) {
        writeCollection(writeBuffer, collection, null);
    }

    /**
     *   Serialize a collection into the target writeBuffer in json format
     *   The format annotation will be applied to all its elements
     */
    public static <T> void writeCollection(WriteBuffer writeBuffer, Collection<T> collection, Format format) {
        ScopedValue.runWhere(localCache, new HashMap<>(), () -> {
            JsonWriterNode jsonWriterNode = new JsonWriterCollectionNode(writeBuffer, collection.iterator(), format);
            jsonWriterNode.serialize();
        });
    }

    /**
     *   Deserialize a record or a plain object from the readBuffer
     */
    @SuppressWarnings("unchecked")
    public static <T> T readObject(ReadBuffer readBuffer, Class<T> type) {
        if(JsonReaderNode.readNextByte(readBuffer) != Constants.LCB) {
            throw new JsonParseException(readBuffer);
        }
        return (T) ScopedValue.getWhere(localCache, new HashMap<>(), () -> {
            JsonReaderNode jsonReaderNode = type.isRecord() ? new JsonReaderRecordNode(readBuffer, type) : new JsonReaderObjectNode(readBuffer, type);
            jsonReaderNode.deserialize();
            return jsonReaderNode.getJsonObject();
        });
    }

    /**
     *   Deserialize a list from the data
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> readList(ReadBuffer readBuffer, JsonTypeRef<T> typeRef) {
        if(JsonReaderNode.readNextByte(readBuffer) != Constants.LSB) {
            throw new JsonParseException(readBuffer);
        }
        return (List<T>) ScopedValue.getWhere(localCache, new HashMap<>(), () -> {
            JsonReaderNode jsonReaderNode = new JsonReaderCollectionNode(readBuffer, List.class, typeRef.type());
            jsonReaderNode.deserialize();
            return jsonReaderNode.getJsonObject();
        });
    }
}
