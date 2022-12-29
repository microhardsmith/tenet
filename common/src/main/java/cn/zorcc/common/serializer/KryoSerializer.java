package cn.zorcc.common.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * 非线程安全的kryo序列化
 */
public class KryoSerializer implements Serializer {
    private static final Kryo kryo = createKryo();

    private static Kryo createKryo() {
        Kryo kryo = new Kryo();
        // 启用引用特性,可以支持循环引用,牺牲一部分性能
        kryo.setReferences(true);
        // 不要求强制注册类
        kryo.setRegistrationRequired(false);
        // 对象没有无参构造方法时,采用Objenesis来实例化对象
        kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
        return kryo;
    }

    @Override
    public <T> byte[] serialize(T obj) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);
        kryo.writeClassAndObject(output, obj);
        output.flush();
        return byteArrayOutputStream.toByteArray();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        Input input = new Input(byteArrayInputStream);
        return (T) kryo.readClassAndObject(input);
    }
}
