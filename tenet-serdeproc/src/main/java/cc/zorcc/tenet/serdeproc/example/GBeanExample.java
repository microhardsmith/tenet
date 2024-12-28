package cc.zorcc.tenet.serdeproc.example;

import cc.zorcc.tenet.serde.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@AimAt(target = "cc.zorcc.tenet.serdeproc.example.GBean")
public final class GBeanExample<A extends Number, B> implements Refer<GBean<A, B>> {
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final GBeanExample<?, ?> SINGLETON = new GBeanExample<>();

    private GBeanExample() {
        if(!initialized.compareAndSet(false, true)) {
            throw new IllegalStateException("GBeanExample already initialized");
        }
    }

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(GBean.class, MethodHandles.lookup());
            intValueHandle = lookup.findVarHandle(GBean.class, "intValue", Integer.class);
            listHandle = lookup.findVarHandle(GBean.class, "list", List.class);
            aHandle = lookup.findVarHandle(GBean.class, "a", Number.class);
            bHandle = lookup.findVarHandle(GBean.class, "b", Object.class);
            cHandle = lookup.findVarHandle(GBean.class, "c", List.class);
            dHandle = lookup.findVarHandle(GBean.class, "d", Map.class);
            SerdeContext.registerRefer(GBean.class, SINGLETON);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final VarHandle intValueHandle;
    private static final VarHandle listHandle;
    private static final VarHandle aHandle;
    private static final VarHandle bHandle;
    private static final VarHandle cHandle;
    private static final VarHandle dHandle;

    private final Col<GBean<A, B>> intValueCol = new Col<>("intValue", Form.of(int.class),
            GBeanExample::intValueTagMapping, GBeanExample::intValueAssign, GBeanExample::intValueGet);
    private final Col<GBean<A, B>> listCol = new Col<>("list", Form.of(List.class, Form.L, String.class, Form.R),
            GBeanExample::listTagMapping, GBeanExample::listAssign, GBeanExample::listGet);
    private final Col<GBean<A, B>> aCol = new Col<>("a", Form.of(Number.class),
            TagMappingFunc.NULLIFY, GBeanExample::aAssign, GBeanExample::aGet);
    private final Col<GBean<A, B>> bCol = new Col<>("b", Form.of(Object.class),
            TagMappingFunc.NULLIFY, GBeanExample::bAssign, GBeanExample::bGet);
    private final Col<GBean<A, B>> cCol = new Col<>("c", Form.of(List.class, Form.L, Object.class, Form.R),
            TagMappingFunc.NULLIFY, GBeanExample::cAssign, GBeanExample::cGet);
    private final Col<GBean<A, B>> dCol = new Col<>("d", Form.of(Map.class, Form.L, Number.class, Object.class, Form.R),
            TagMappingFunc.NULLIFY, GBeanExample::dAssign, GBeanExample::dGet);

    private static @Nullable String intValueTagMapping(@NonNull String key) {
        return switch (key) {
            case "json" -> "str";
            default -> null;
        };
    }

    private static @Nullable String listTagMapping(@NonNull String key) {
        return switch (key) {
            case "json" -> "raw";
            default -> null;
        };
    }

    private static <A extends Number, B> void intValueAssign(Builder<GBean<A, B>> builder, Object value) {
        if(builder instanceof Wrapper<A, B> wrapper) {
            intValueSet(wrapper.instance(), (Integer) value);
        } else {
            throw new ClassCastException("class %s cannot be cast to class wrapper".formatted(builder.getClass()));
        }
    }

    @SuppressWarnings("unchecked")
    private static <A extends Number, B> void listAssign(Builder<GBean<A, B>> builder, Object value) {
        if(builder instanceof Wrapper<A, B> wrapper) {
            listSet(wrapper.instance(), (List<String>) value);
        } else {
            throw new ClassCastException("class %s cannot be cast to class wrapper".formatted(builder.getClass()));
        }
    }

    @SuppressWarnings("unchecked")
    private static <A extends Number, B> void aAssign(Builder<GBean<A, B>> builder, Object value) {
        if(builder instanceof Wrapper<A, B> wrapper) {
            aSet(wrapper.instance(), (A) value);
        } else {
            throw new ClassCastException("class %s cannot be cast to class wrapper".formatted(builder.getClass()));
        }
    }

    @SuppressWarnings("unchecked")
    private static <A extends Number, B> void bAssign(Builder<GBean<A, B>> builder, Object value) {
        if(builder instanceof Wrapper<A, B> wrapper) {
            bSet(wrapper.instance(), (B) value);
        } else {
            throw new ClassCastException("class %s cannot be cast to class wrapper".formatted(builder.getClass()));
        }
    }

    @SuppressWarnings("unchecked")
    private static <A extends Number, B> void cAssign(Builder<GBean<A, B>> builder, Object value) {
        if(builder instanceof Wrapper<A, B> wrapper) {
            cSet(wrapper.instance(), (List<B>) value);
        } else {
            throw new ClassCastException("class %s cannot be cast to class wrapper".formatted(builder.getClass()));
        }
    }

    @SuppressWarnings("unchecked")
    private static <A extends Number, B> void dAssign(Builder<GBean<A, B>> builder, Object value) {
        if(builder instanceof Wrapper<A, B> wrapper) {
            dSet(wrapper.instance(), (Map<A, B>) value);
        } else {
            throw new ClassCastException("class %s cannot be cast to class wrapper".formatted(builder.getClass()));
        }
    }

    private static <A extends Number, B> void intValueSet(GBean<A, B> instance, Integer value) {
        intValueHandle.set(instance, value);
    }

    private static <A extends Number, B> Integer intValueGet(GBean<A, B> instance) {
        return (Integer) intValueHandle.get(instance);
    }

    private static <A extends Number, B> void listSet(GBean<A, B> instance, List<String> value) {
        listHandle.set(instance, value);
    }

    @SuppressWarnings("unchecked")
    private static <A extends Number, B> List<String> listGet(GBean<A, B> instance) {
        return (List<String>) listHandle.get(instance);
    }

    private static <A extends Number, B> void aSet(GBean<A, B> instance, A value) {
        aHandle.set(instance, value);
    }

    @SuppressWarnings("unchecked")
    private static <A extends Number, B> A aGet(GBean<A, B> instance) {
        return (A) aHandle.get(instance);
    }

    private static <A extends Number, B> void bSet(GBean<A, B> instance, B value) {
        bHandle.set(instance, value);
    }

    @SuppressWarnings("unchecked")
    private static <B> B bGet(GBean<?, B> instance) {
        return (B) bHandle.get(instance);
    }

    private static <B> void cSet(GBean<?, B> instance, List<B> value) {
        cHandle.set(instance, value);
    }

    @SuppressWarnings("unchecked")
    private static <B> List<B> cGet(GBean<?, B> instance) {
        return (List<B>) cHandle.get(instance);
    }

    private static <A extends Number, B> void dSet(GBean<A, B> instance, Map<A, B> value) {
        dHandle.set(instance, value);
    }

    @SuppressWarnings("unchecked")
    private static <A extends Number, B> Map<A, B> dGet(GBean<A, B> instance) {
        return (Map<A, B>) dHandle.get(instance);
    }

    private record Wrapper<A extends Number, B>(GBean<A, B> instance, AtomicBoolean flag) implements Builder<GBean<A, B>> {
        Wrapper() {
            this(new GBean<>(), new AtomicBoolean(false));
        }

        @Override
        public @NonNull GBean<A, B> build() {
            if(flag.compareAndSet(false, true)) return instance;
            else throw new IllegalCallerException("build() should only be invoked once");
        }
    }

    @Override
    public @NonNull List<String> fields() {
        return List.of("intValue", "list", "a", "b", "c", "d");
    }

    @Override
    public @NonNull Builder<GBean<A, B>> builder() {
        return new Wrapper<>();
    }

    @Override
    public @Nullable Col<GBean<A, B>> col(@NonNull String colName) {
        return switch (colName) {
            case "intValue" -> intValueCol;
            case "list" -> listCol;
            case "a" -> aCol;
            case "b" -> bCol;
            case "c" -> cCol;
            case "d" -> dCol;
            default -> null;
        };
    }

    @Override
    public @Nullable GBean<A, B> byName(@NonNull String name) {
        return null;
    }
}
