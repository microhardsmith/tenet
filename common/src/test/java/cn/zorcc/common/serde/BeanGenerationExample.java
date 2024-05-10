package cn.zorcc.common.serde;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.function.Supplier;

public final class BeanGenerationExample implements Handle<Bean> {

    static {
        try{
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(Bean.class, MethodHandles.lookup());

            MethodHandle aGetHandle = lookup.findGetter(Bean.class, "a", int.class);
            MethodHandle aSetHandle = lookup.findSetter(Bean.class, "a", int.class);
            Column<Bean> aColumn = new Column<>("a", new RecurClass(int.class, List.of()), BeanGenerationExample::aTag, BeanGenerationExample::aAssign, BeanGenerationExample::aGet);
            aAccessor = new Accessor<>(aGetHandle, aSetHandle, aColumn);

            MethodHandle bGetHandle = lookup.findGetter(Bean.class, "b", String.class);
            MethodHandle bSetHandle = lookup.findSetter(Bean.class, "b", String.class);
            Column<Bean> bColumn = new Column<>("b", new RecurClass(String.class, List.of()), i -> i, BeanGenerationExample::bAssign, BeanGenerationExample::bGet);
            bAccessor = new Accessor<>(bGetHandle, bSetHandle, bColumn);

            GenerationContext.registerHandle(Bean.class, new BeanGenerationExample());
        } catch (Throwable throwable) {
            throw new ExceptionInInitializerError(throwable);
        }
    }

    private static final Accessor<Bean> aAccessor;
    private static final Accessor<Bean> bAccessor;

    private static void aAssign(Supplier<Bean> supplier, Object o) {
        if(supplier instanceof Wrapper wrapper) {
            aSet(wrapper.instance(), (int) o);
        } else {
            throw new RuntimeException("Should never be reached");
        }
    }

    private static void bAssign(Supplier<Bean> supplier, Object o) {
        if(supplier instanceof Wrapper wrapper) {
            bSet(wrapper.instance(), (String) o);
        } else {
            throw new RuntimeException("Should never be reached");
        }
    }

    private static int aGet(Bean instance) {
        try{
            return (int) aAccessor.getter().invokeExact(instance);
        }catch (Throwable throwable) {
            throw new RuntimeException("Should never be reached", throwable);
        }
    }

    private static void aSet(Bean instance, int value) {
        try{
            aAccessor.setter().invokeExact(instance, value);
        }catch (Throwable throwable) {
            throw new RuntimeException("Should never be reached", throwable);
        }
    }

    private static String bGet(Bean instance) {
        try{
            return (String) bAccessor.getter().invokeExact(instance);
        }catch (Throwable throwable) {
            throw new RuntimeException("Should never be reached", throwable);
        }
    }

    private static void bSet(Bean instance, String value) {
        try{
            bAccessor.setter().invokeExact(instance, value);
        }catch (Throwable throwable) {
            throw new RuntimeException("Should never be reached", throwable);
        }
    }

    private static String aTag(String key) {
        return switch (key) {
            case "default" -> "1";
            case "json" -> "str";
            case null, default -> null;
        };
    }

    record Wrapper(Bean instance) implements Supplier<Bean> {
        @Override
        public Bean get() {
            return instance;
        }
    }

    @Override
    public Supplier<Bean> createAssigner() {
        return new Wrapper(new Bean());
    }

    @Override
    public Column<Bean> col(String columnName) {
        return switch (columnName) {
            case "a" -> aAccessor.column();
            case "b" -> bAccessor.column();
            case null, default -> null;
        };
    }

    @Override
    public Bean byName(String name) {
        throw new UnsupportedOperationException();
    }
}
