package cc.zorcc.tenet.serdeproc.example;

import cc.zorcc.tenet.serde.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@AimAt(target = "cc.zorcc.tenet.serdeproc.example.RBean")
public final class RBeanExample implements Refer<RBean> {
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final RBeanExample SINGLETON = new RBeanExample();

    private RBeanExample() {
        if(!initialized.compareAndSet(false, true)) {
            throw new IllegalStateException("BeanExample already initialized");
        }
    }

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(RBean.class, MethodHandles.lookup());
            intValueHandle = lookup.findVarHandle(RBean.class, "intValue", Integer.class);
            listHandle = lookup.findVarHandle(RBean.class, "list", List.class);
            doubleListHandle = lookup.findVarHandle(RBean.class, "doubleList", List.class);
            mapHandle = lookup.findVarHandle(RBean.class, "map", Map.class);
            SerdeContext.registerRefer(RBean.class, SINGLETON);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final VarHandle intValueHandle;
    private static final VarHandle listHandle;
    private static final VarHandle doubleListHandle;
    private static final VarHandle mapHandle;

    private final Col<RBean> intValueCol = new Col<>("intValue", Form.of(int.class),
            RBeanExample::intValueTagMapping, RBeanExample::intValueAssign, RBeanExample::intValueGet);
    private final Col<RBean> listCol = new Col<>("list", Form.of(List.class, Form.L, String.class, Form.R),
            RBeanExample::listTagMapping, RBeanExample::listAssign, RBeanExample::listGet);
    private final Col<RBean> doubleListCol = new Col<>("doubleList", Form.of(List.class, Form.L, List.class, Form.L, String.class, Form.R, Form.R),
            TagMappingFunc.NULLIFY, RBeanExample::doubleListAssign, RBeanExample::doubleListGet);
    private final Col<RBean> mapCol = new Col<>("map", Form.of(Map.class, Form.L, Integer.class, String.class, Form.R),
            TagMappingFunc.NULLIFY, RBeanExample::mapAssign, RBeanExample::mapGet);

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

    private static void intValueAssign(Builder<RBean> builder, Object value) {
        if(builder instanceof Wrapper wrapper) {
            wrapper.intValue = (Integer) value;
        } else {
            throw new ClassCastException("class %s cannot be cast to class wrapper".formatted(builder.getClass()));
        }
    }

    @SuppressWarnings("unchecked")
    private static void listAssign(Builder<RBean> builder, Object value) {
        if(builder instanceof Wrapper wrapper) {
            wrapper.list = (List<String>) value;
        } else {
            throw new ClassCastException("class %s cannot be cast to class wrapper".formatted(builder.getClass()));
        }
    }

    @SuppressWarnings("unchecked")
    private static void doubleListAssign(Builder<RBean> builder, Object value) {
        if(builder instanceof Wrapper wrapper) {
            wrapper.doubleList = (List<List<String>>) value;
        } else {
            throw new ClassCastException("class %s cannot be cast to class wrapper".formatted(builder.getClass()));
        }
    }

    @SuppressWarnings("unchecked")
    private static void mapAssign(Builder<RBean> builder, Object value) {
        if(builder instanceof Wrapper wrapper) {
            wrapper.map = (Map<Integer, String>) value;
        } else {
            throw new ClassCastException("class %s cannot be cast to class wrapper".formatted(builder.getClass()));
        }
    }

    private static Integer intValueGet(RBean instance) {
        return (Integer) intValueHandle.get(instance);
    }

    @SuppressWarnings("unchecked")
    private static List<String> listGet(RBean instance) {
        return (List<String>) listHandle.get(instance);
    }

    @SuppressWarnings("unchecked")
    private static List<List<String>> doubleListGet(RBean instance) {
        return (List<List<String>>) doubleListHandle.get(instance);
    }

    @SuppressWarnings("unchecked")
    private static Map<Integer, String> mapGet(RBean instance) {
        return (Map<Integer, String>) mapHandle.get(instance);
    }


    private static final class Wrapper implements Builder<RBean> {
        private final AtomicBoolean flag = new AtomicBoolean(false);
        private Integer intValue;
        private List<String> list;
        private List<List<String>> doubleList;
        private Map<Integer, String> map;

        @Override
        public @NonNull RBean build() {
            if(flag.compareAndSet(false, true)) return new RBean(intValue, list, doubleList, map);
            else throw new IllegalCallerException("build() should only be invoked once");
        }
    }

    @Override
    public @NonNull List<String> fields() {
        return List.of("intValue", "list", "doubleList", "map");
    }

    @Override
    public @NonNull Builder<RBean> builder() {
        return new Wrapper();
    }

    @Override
    public @Nullable Col<RBean> col(@NonNull String colName) {
        return switch (colName) {
            case "intValue" -> intValueCol;
            case "list" -> listCol;
            case "doubleList" -> doubleListCol;
            case "map" -> mapCol;
            default -> null;
        };
    }

    @Override
    public @Nullable RBean byName(@NonNull String name) {
        return null;
    }
}
