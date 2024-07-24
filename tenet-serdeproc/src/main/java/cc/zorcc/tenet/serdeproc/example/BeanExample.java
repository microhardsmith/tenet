package cc.zorcc.tenet.serdeproc.example;

import cc.zorcc.tenet.serde.*;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.List;

public final class BeanExample implements Refer<Bean> {

    private static final BeanExample SINGLETON = new BeanExample();

    private BeanExample() {

    }

    static {
        try{
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(Bean.class, MethodHandles.lookup());
            intValueHandle = lookup.findVarHandle(Bean.class, "intValue", Integer.class);
            intValueCol = new Col<>("intValue", new RecursiveType(int.class, List.of()), BeanExample::intValueTagMapping, BeanExample::intValueAssign, BeanExample::intValueGet);
            SerdeContext.registerRefer(Bean.class, SINGLETON);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final VarHandle intValueHandle;
    private static final Col<Bean> intValueCol;

    private static String intValueTagMapping(String key) {
        return switch (key) {
            case "json" -> "str";
            case null, default -> null;
        };
    }

    private static void intValueAssign(Builder<Bean> builder, Object value) {
        if(builder instanceof Wrapper wrapper && value instanceof Integer v) {
            intValueSet(wrapper.instance(), v);
        } else {
            throw new SerdeException("Type mismatch");
        }
    }

    private static void intValueSet(Bean instance, Integer value) {
        intValueHandle.set(instance, value);
    }

    private static Integer intValueGet(Bean instance) {
        return (Integer) intValueHandle.get(instance);
    }

    record Wrapper(Bean instance) implements Builder<Bean> {

        Wrapper() {
            this(new Bean());
        }

        @Override
        public Bean build() {
            return instance;
        }
    }

    @Override
    public Builder<Bean> builder() {
        return new Wrapper();
    }

    @Override
    public Col<Bean> col(String colName) {
        return switch (colName) {
            case "intValue" -> intValueCol;
            case null, default -> null;
        };
    }

    @Override
    public Bean byName(String name) {
        return null;
    }
}
