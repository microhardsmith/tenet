package cc.zorcc.tenet.serdeproc.example;

import cc.zorcc.tenet.serde.*;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.List;
import java.util.Objects;

public final class EBeanExample implements Refer<EBean> {
    private static final EBeanExample SINGLETON = new EBeanExample();

    private EBeanExample() {

    }

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(EBean.class, MethodHandles.lookup());
            intValueHandle = lookup.findVarHandle(EBean.class, "intValue", Integer.class);
            intValueCol = new Col<>("intValue", new RecursiveType(Integer.class, List.of()), EBeanExample::intValueTagMapping, EBeanExample::intValueAssign, EBeanExample::intValueGet);
            SerdeContext.registerRefer(EBean.class, SINGLETON);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final VarHandle intValueHandle;
    private static final Col<EBean> intValueCol;

    private static String intValueTagMapping(String key) {
        return switch (key) {
            case "json" -> "str";
            case null, default -> null;
        };
    }

    private static void intValueAssign(Builder<EBean> builder, Object value) {
        if(builder instanceof Wrapper wrapper && value instanceof Integer v) {
            wrapper.intValue = v;
        } else {
            throw new SerdeException("Type mismatch");
        }
    }

    private static Integer intValueGet(EBean instance) {
        return (Integer) intValueHandle.get(instance);
    }

    private static final class Wrapper implements Builder<EBean> {
        private static final EBean[] values = EBean.values();
        private Integer intValue;

        @Override
        public EBean build() {
            for (EBean e : values) {
                if(Objects.equals(intValueGet(e), intValue)) {
                    return e;
                }
            }
            return null;
        }
    }

    @Override
    public Builder<EBean> builder() {
        return new Wrapper();
    }

    @Override
    public Col<EBean> col(String colName) {
        return switch (colName) {
            case "intValue" -> intValueCol;
            case null, default -> null;
        };
    }

    @Override
    public EBean byName(String name) {
        return switch (name) {
            case "Test1" -> EBean.Test1;
            case "Test2" -> EBean.Test2;
            case null, default -> null;
        };
    }
}
