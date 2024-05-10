package cn.zorcc.common.serde;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class EnumGenerationExample implements Handle<EnumBean> {
    private static final Accessor<EnumBean> aAccessor;
    private static final Accessor<EnumBean> bAccessor;

    static {
        try{
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(EnumBean.class, MethodHandles.lookup());

            MethodHandle aGetHandle = lookup.findGetter(EnumBean.class, "a", int.class);
            Column<EnumBean> aColumn = new Column<>("a", new RecurClass(int.class, List.of()), EnumGenerationExample::aTag, EnumGenerationExample::aAssign, EnumGenerationExample::aGet);
            aAccessor = new Accessor<>(aGetHandle, aColumn);

            MethodHandle bGetHandle = lookup.findGetter(EnumBean.class, "b", String.class);
            Column<EnumBean> bColumn = new Column<>("b", new RecurClass(String.class, List.of()), i -> i, EnumGenerationExample::bAssign, EnumGenerationExample::bGet);
            bAccessor = new Accessor<>(bGetHandle, bColumn);

            GenerationContext.registerHandle(EnumBean.class, new EnumGenerationExample());
        }catch (Throwable throwable) {
            throw new RuntimeException("Should never be reached", throwable);
        }
    }

    private static void aAssign(Supplier<EnumBean> supplier, Object o) {
        if(supplier instanceof Wrapper wrapper) {
            wrapper.a = (int) o;
        } else {
            throw new RuntimeException("Should never be reached");
        }
    }

    private static void bAssign(Supplier<EnumBean> supplier, Object o) {
        if(supplier instanceof Wrapper wrapper) {
            wrapper.b = (String) o;
        } else {
            throw new RuntimeException("Should never be reached");
        }
    }

    private static final class Wrapper implements Supplier<EnumBean> {
        private static final EnumBean[] values = EnumBean.values();
        private int a;
        private String b;

        @Override
        public EnumBean get() {
            for (EnumBean e : values) {
                if(Objects.equals(aGet(e), a) && Objects.equals(bGet(e), b)) {
                    return e;
                }
            }
            return null;
        }
    }

    private static int aGet(EnumBean bean) {
        try{
            return (int) aAccessor.getter().invokeExact(bean);
        }catch (Throwable throwable) {
            throw new RuntimeException("Should never be reached when invoking aGet()", throwable);
        }
    }

    private static String bGet(EnumBean bean) {
        try{
            return (String) bAccessor.getter().invokeExact(bean);
        }catch (Throwable throwable) {
            throw new RuntimeException("Should never be reached when invoking bGet()", throwable);
        }
    }

    @Override
    public Supplier<EnumBean> createAssigner() {
        return new Wrapper();
    }

    @Override
    public Column<EnumBean> col(String columnName) {
        return switch (columnName) {
            case "a" -> aAccessor.column();
            case "b" -> bAccessor.column();
            case null, default -> null;
        };
    }

    static String aTag(String attr) {
        return switch (attr) {
            case "default" -> "2";
            case "json" -> "str";
            case null, default -> null;
        };
    }

    @Override
    public EnumBean byName(String name) {
        return switch (name) {
            case "Test1" -> EnumBean.Test1;
            case "Test2" -> EnumBean.Test2;
            case null, default -> null;
        };
    }
}
