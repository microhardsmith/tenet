package cc.zorcc.tenet.serdeproc.example;

import cc.zorcc.tenet.serde.*;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.List;

public final class RBeanExample implements Refer<RBean> {
    private static final RBeanExample SINGLETON = new RBeanExample();

    private RBeanExample() {

    }

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(RBean.class, MethodHandles.lookup());
            intValueHandle = lookup.findVarHandle(RBean.class, "intValue", Integer.class);
            intValueCol = new Col<>("intValue", new RecursiveType(Integer.class, List.of()), RBeanExample::intValueTagMapping, RBeanExample::intValueAssign, RBeanExample::intValueGet);
            SerdeContext.registerRefer(RBean.class, SINGLETON);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final VarHandle intValueHandle;
    private static final Col<RBean> intValueCol;

    private static String intValueTagMapping(String key) {
        return switch (key) {
            case "json" -> "str";
            case null, default -> null;
        };
    }

    private static void intValueAssign(Builder<RBean> builder, Object value) {
        if(builder instanceof Wrapper wrapper && value instanceof Integer v) {
            wrapper.intValue = v;
        } else {
            throw new SerdeException("Type mismatch");
        }
    }

    private static Integer intValueGet(RBean instance) {
        return (Integer) intValueHandle.get(instance);
    }

    private static final class Wrapper implements Builder<RBean> {
        private Integer intValue;

        @Override
        public RBean build() {
            return new RBean(intValue);
        }
    }

    @Override
    public Builder<RBean> builder() {
        return new Wrapper();
    }

    @Override
    public Col<RBean> col(String colName) {
        return switch (colName) {
            case "intValue" -> intValueCol;
            case null, default -> null;
        };
    }

    @Override
    public RBean byName(String name) {
        return null;
    }
}
