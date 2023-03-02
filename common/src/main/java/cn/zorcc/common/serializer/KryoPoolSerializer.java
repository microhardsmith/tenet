package cn.zorcc.common.serializer;

import cn.zorcc.common.Pool;
import cn.zorcc.common.util.NativeUtil;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import lombok.extern.slf4j.Slf4j;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * 线程安全的kryo序列化,使用对象池分配
 */
@Slf4j
public enum KryoPoolSerializer implements Serializer {
    INSTANCE;
    private final Pool<Kryo> kryoPool;

    KryoPoolSerializer() {
        int capacity = NativeUtil.getCpuCores() << 2;
        this.kryoPool = new Pool<>(capacity);
        for(int i = 0; i < capacity; i++) {
            Kryo kryo = new Kryo();
            // 启用引用特性,可以支持循环引用,牺牲一部分性能
            kryo.setReferences(true);
            // 不要求强制注册类
            kryo.setRegistrationRequired(false);
            // 对象没有无参构造方法时,采用Objenesis来实例化对象
            kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
            kryoPool.offer(kryo);
        }
    }

    @Override
    public <T> byte[] serialize(T obj) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);
        Kryo kryo = kryoPool.obtain();
        kryo.writeClassAndObject(output, obj);
        kryoPool.free(kryo);
        output.flush();
        return byteArrayOutputStream.toByteArray();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        Input input = new Input(byteArrayInputStream);
        Kryo kryo = kryoPool.obtain();
        T result = (T) kryo.readClassAndObject(input);
        kryoPool.free(kryo);
        return result;
    }
}
