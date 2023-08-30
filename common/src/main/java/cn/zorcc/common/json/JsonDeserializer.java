package cn.zorcc.common.json;

@FunctionalInterface
public interface JsonDeserializer<T> {
    T deserialize(byte[] data);
}
