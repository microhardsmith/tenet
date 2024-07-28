package cn.zorcc.common.serde;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.function.Supplier;

public final class RecordGenerationExample implements Handle<RecordBean> {
    private static final Accessor<RecordBean> aAccessor;
    private static final Accessor<RecordBean> bAccessor;

    static {
        try{
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(RecordBean.class, MethodHandles.lookup());

            MethodHandle aGetHandle = lookup.findGetter(RecordBean.class, "a", int.class);
            Column<RecordBean> aColumn = new Column<>("a", new RecursiveType(int.class, List.of()), RecordGenerationExample::aTag, RecordGenerationExample::aAssign, RecordGenerationExample::aGet);
            aAccessor = new Accessor<>(aGetHandle, aColumn);

            MethodHandle bGetHandle = lookup.findGetter(RecordBean.class, "b", String.class);
            Column<RecordBean> bColumn = new Column<>("b", new RecursiveType(String.class, List.of()), i -> i, RecordGenerationExample::bAssign, RecordGenerationExample::bGet);
            bAccessor = new Accessor<>(bGetHandle, bColumn);

            GenerationContext.registerHandle(RecordBean.class, new RecordGenerationExample());
        }catch (Throwable throwable) {
            throw new RuntimeException("Should never be reached", throwable);
        }
    }

    private static void aAssign(Supplier<RecordBean> supplier, Object o) {
        if(supplier instanceof Wrapper wrapper) {
            wrapper.a = (int) o;
        } else {
            throw new RuntimeException("Should never be reached");
        }
    }

    private static void bAssign(Supplier<RecordBean> supplier, Object o) {
        if(supplier instanceof Wrapper wrapper) {
            wrapper.b = (String) o;
        } else {
            throw new RuntimeException("Should never be reached");
        }
    }

    private static int aGet(RecordBean instance) {
        try{
            return (int) aAccessor.getter().invokeExact(instance);
        }catch (Throwable throwable) {
            throw new RuntimeException("Should never be reached", throwable);
        }
    }

    private static String bGet(RecordBean instance) {
        try{
            return (String) bAccessor.getter().invokeExact(instance);
        }catch (Throwable throwable) {
            throw new RuntimeException("Should never be reached", throwable);
        }
    }

    private static final class Wrapper implements Supplier<RecordBean> {
        private int a;
        private String b;

        @Override
        public RecordBean get() {
            return new RecordBean(a, b);
        }
    }

    @Override
    public Supplier<RecordBean> createAssigner() {
        return new Wrapper();
    }

    @Override
    public Column<RecordBean> col(String columnName) {
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
    public RecordBean byName(String name) {
        throw new UnsupportedOperationException();
    }
}
