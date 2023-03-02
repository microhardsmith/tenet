package cn.zorcc.common.serializer;

import cn.zorcc.common.Pool;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * jackson序列化,用池化技术保证多线程下的使用效率
 */
public enum JsonPoolSerializer implements Serializer {
    INSTANCE;
    private final Pool<ObjectMapper> objectMapperPool;

    JsonPoolSerializer() {
        int capacity = NativeUtil.getCpuCores() << 2;
        this.objectMapperPool = new Pool<>(capacity);
        for(int i = 0; i < capacity; i++) {
            ObjectMapper objectMapper = JsonSerializer.createObjectMapper();
            objectMapperPool.offer(objectMapper);
        }
    }

    @Override
    public <T> byte[] serialize(T obj) {
        try {
            ObjectMapper mapper = objectMapperPool.obtain();
            byte[] result = mapper.writeValueAsBytes(obj);
            objectMapperPool.free(mapper);
            return result;
        } catch (JsonProcessingException e) {
            throw new FrameworkException(ExceptionType.JSON, "Can't serialize obj into bytes : " + obj.getClass().getSimpleName(), e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try {
            ObjectMapper mapper = objectMapperPool.obtain();
            T result = mapper.readValue(bytes, clazz);
            objectMapperPool.free(mapper);
            return result;
        } catch (IOException e) {
            throw new FrameworkException(ExceptionType.JSON, "Can't deserialize bytes into obj, class : " + clazz.getSimpleName(), e);
        }
    }
}
