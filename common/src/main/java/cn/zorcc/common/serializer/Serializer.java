package cn.zorcc.common.serializer;

/**
 * 对象转字节数组序列化方法接口
 */
public interface Serializer {
    <T> byte[] serialize(T obj);

    <T> T deserialize(byte[] bytes, Class<T> clazz);
}
