package cn.zorcc.common.json;

import cn.zorcc.common.*;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

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

    @SuppressWarnings("unchecked")
    public static <T> void serializeAsObject(Writer writer, T obj) {
        Class<T> type = (Class<T>) obj.getClass();
        serializeAsObject(writer, obj, type);
    }

    public static <T> void serializeAsObject(Writer writer, T obj, Class<T> type) {
        if(type.isPrimitive() || type.isAnnotation() || type.isRecord() || type.isMemberClass()) {
            throw new FrameworkException(ExceptionType.JSON, "Unsupported type");
        }
        new JsonWriter().serializeAsObject(writer, obj, type);



        Gt<T> gt = Gt.of(type);
        List<GtInfo> gtInfoList = gt.gtInfoList();
        try(ResizableByteArray resizableByteArray = new ResizableByteArray()) {
            resizableByteArray.writeByte(Constants.LCB);
            boolean notFirst = false;
            for (GtInfo gtInfo : gtInfoList) {
                Object field = gtInfo.getter().apply(obj);
                if(field != null) {
                    writeKey(resizableByteArray, gtInfo.fieldName(), notFirst);
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
