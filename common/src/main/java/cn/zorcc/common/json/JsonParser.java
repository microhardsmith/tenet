package cn.zorcc.common.json;

import cn.zorcc.common.*;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *   JsonParser implementation, primitive types and primitive arrays are not supported
 */
public final class JsonParser {
    private JsonParser() {
        throw new UnsupportedOperationException();
    }
    private static final byte[] TRUE = "true".getBytes(StandardCharsets.UTF_8);
    private static final byte[] FALSE = "false".getBytes(StandardCharsets.UTF_8);
    private static final byte[] INFINITY = "Infinity".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NAN = "NaN".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NULL = "null".getBytes(StandardCharsets.UTF_8);

    private static final Map<Class<?>, JsonSerializer<?>> serializerMap = new ConcurrentHashMap<>();
    private static final Map<Class<?>, JsonDeserializer<?>> deserializerMap = new ConcurrentHashMap<>();

    public static <T> void registerJsonSerializer(Class<T> clazz, JsonSerializer<T> serializer) {
        serializerMap.putIfAbsent(clazz, serializer);
    }

    public static <T> void registerJsonDeserializer(Class<T> clazz, JsonDeserializer<?> deserializer) {
        deserializerMap.putIfAbsent(clazz, deserializer);
    }

    @SuppressWarnings("unchecked")
    public static <T> JsonSerializer<T> getJsonSerializer(Class<T> clazz) {
       return (JsonSerializer<T>) serializerMap.get(clazz);
    }

    @SuppressWarnings("unchecked")
    public static <T> JsonDeserializer<T> getJsonDeserializer(Class<T> clazz) {
        return (JsonDeserializer<T>) deserializerMap.get(clazz);
    }

    public static void serializeAsObject(Writer writer, Object obj) {
        serializeAsObject(writer, obj, obj.getClass());
    }

    public static <T> void serializeAsObject(Writer writer, Object obj, Class<T> objClass) {
        if(objClass.isPrimitive() || objClass.isAnnotation() || objClass.isRecord() || objClass.isMemberClass()) {
            throw new FrameworkException(ExceptionType.JSON, "Unsupported type");
        }
        new JsonWriter().serializeAsObject(writer, obj, objClass);



        Gt<T> gt = Gt.of(objClass);
        List<MetaInfo> metaInfoList = gt.metaInfoList();
        try(ResizableByteArray resizableByteArray = new ResizableByteArray()) {
            resizableByteArray.writeByte(Constants.LCB);
            boolean notFirst = false;
            for (MetaInfo metaInfo : metaInfoList) {
                Object field = metaInfo.getter().apply(obj);
                if(field != null) {
                    writeKey(resizableByteArray, metaInfo.name(), notFirst);
                    writeValue(resizableByteArray, field);
                    notFirst = true;
                }
            }
            resizableByteArray.writeByte(Constants.RCB);
            writer.writeBytes(resizableByteArray.rawArray(), Constants.ZERO, resizableByteArray.writeIndex());
        }
    }

    /**
     *   Deserialize a object from the data
     */
    public static <T> T deserializeAsObject(ReadBuffer readBuffer, Class<T> type) {
        JsonReader<T> jsonReader = new JsonReader<>(readBuffer, type);
        return jsonReader.deserializeAsObject();
    }
    /**
     * Deserialize a list from the data
     */
    public static <T> List<T> deserializeAsList(ReadBuffer readBuffer, Class<T> type) {
        JsonReader<T> jsonReader = new JsonReader<>(readBuffer, type);
        return jsonReader.deserializeAsList();
    }



}
