package cc.zorcc.tenet.serdeproc.example;

import cc.zorcc.tenet.serde.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@AimAt(target = "cc.zorcc.tenet.serdeproc.example.Bean")
public final class BeanExample implements Refer<Bean> {
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final BeanExample SINGLETON = new BeanExample();

    private BeanExample() {
        if(!initialized.compareAndSet(false, true)) {
            throw new IllegalStateException("BeanExample already initialized");
        }
    }

    static {
        try{
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(Bean.class, MethodHandles.lookup());
            intValueHandle = lookup.findVarHandle(Bean.class, "intValue", Integer.class);
            listHandle = lookup.findVarHandle(Bean.class, "list", List.class);
            doubleListHandle = lookup.findVarHandle(Bean.class, "doubleList", List.class);
            mapHandle = lookup.findVarHandle(Bean.class, "map", Map.class);
            SerdeContext.registerRefer(Bean.class, SINGLETON);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final VarHandle intValueHandle;
    private static final VarHandle listHandle;
    private static final VarHandle doubleListHandle;
    private static final VarHandle mapHandle;

    private final Col<Bean> intValueCol = new Col<>("intValue", Form.of(int.class),
            BeanExample::intValueTagMapping, BeanExample::intValueAssign, BeanExample::intValueGet);
    private final Col<Bean> listCol = new Col<>("list", Form.of(List.class, Form.L, String.class, Form.R),
            BeanExample::listTagMapping, BeanExample::listAssign, BeanExample::listGet);
    private final Col<Bean> doubleListCol = new Col<>("doubleList", Form.of(List.class, Form.L, List.class, Form.L, String.class, Form.R, Form.R),
            TagMappingFunc.NULLIFY, BeanExample::doubleListAssign, BeanExample::doubleListGet);
    private final Col<Bean> mapCol = new Col<>("map", Form.of(Map.class, Form.L, Integer.class, String.class, Form.R),
            TagMappingFunc.NULLIFY, BeanExample::mapAssign, BeanExample::mapGet);

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

    private static void intValueAssign(Builder<Bean> builder, Object value) {
        if(builder instanceof Wrapper wrapper) {
            intValueSet(wrapper.instance(), (Integer) value);
        } else {
            throw new ClassCastException("class %s cannot be cast to class wrapper".formatted(builder.getClass()));
        }
    }

    @SuppressWarnings("unchecked")
    private static void listAssign(Builder<Bean> builder, Object value) {
        if(builder instanceof Wrapper wrapper) {
            listSet(wrapper.instance(), (List<String>) value);
        } else {
            throw new ClassCastException("class %s cannot be cast to class wrapper".formatted(builder.getClass()));
        }
    }

    @SuppressWarnings("unchecked")
    private static void doubleListAssign(Builder<Bean> builder, Object value) {
        if(builder instanceof Wrapper wrapper) {
            doubleListSet(wrapper.instance(), (List<List<String>>) value);
        } else {
            throw new ClassCastException("class %s cannot be cast to class wrapper".formatted(builder.getClass()));
        }
    }

    @SuppressWarnings("unchecked")
    private static void mapAssign(Builder<Bean> builder, Object value) {
        if(builder instanceof Wrapper wrapper) {
            mapSet(wrapper.instance(), (Map<Integer, String>) value);
        } else {
            throw new ClassCastException("class %s cannot be cast to class wrapper".formatted(builder.getClass()));
        }
    }

    private static void intValueSet(Bean instance, Integer value) {
        intValueHandle.set(instance, value);
    }

    private static Integer intValueGet(Bean instance) {
        return (Integer) intValueHandle.get(instance);
    }

    private static void listSet(Bean instance, List<String> value) {
        listHandle.set(instance, value);
    }

    @SuppressWarnings("unchecked")
    private static List<String> listGet(Bean instance) {
        return (List<String>) listHandle.get(instance);
    }

    private static void doubleListSet(Bean instance, List<List<String>> value) {
        doubleListHandle.set(instance, value);
    }

    @SuppressWarnings("unchecked")
    private static List<List<String>> doubleListGet(Bean instance) {
        return (List<List<String>>) doubleListHandle.get(instance);
    }

    private static void mapSet(Bean instance, Map<Integer, String> value) {
        mapHandle.set(instance, value);
    }

    @SuppressWarnings("unchecked")
    private static Map<Integer, String> mapGet(Bean instance) {
        return (Map<Integer, String>) mapHandle.get(instance);
    }

    private record Wrapper(Bean instance, AtomicBoolean flag) implements Builder<Bean> {

        Wrapper() {
            this(new Bean(), new AtomicBoolean(false));
        }

        @Override
        public @NonNull Bean build() {
            if(flag.compareAndSet(false, true)) {
                return instance;
            }else {
                throw new IllegalCallerException("build() should only be invoked once");
            }
        }
    }

    @Override
    public @NonNull List<String> fields() {
        return List.of("intValue", "list", "doubleList", "map");
    }

    @Override
    public @NonNull Builder<Bean> builder() {
        return new Wrapper();
    }

    @Override
    public @Nullable Col<Bean> col(@NonNull String colName) {
        return switch (colName) {
            case "intValue" -> intValueCol;
            case "list" -> listCol;
            case "doubleList" -> doubleListCol;
            case "map" -> mapCol;
            default -> null;
        };
    }

    @Override
    public @Nullable Bean byName(@NonNull String name) {
        return null;
    }
}
