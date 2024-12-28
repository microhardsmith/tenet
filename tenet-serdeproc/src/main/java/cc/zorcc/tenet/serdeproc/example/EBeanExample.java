package cc.zorcc.tenet.serdeproc.example;

import cc.zorcc.tenet.serde.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@AimAt(target = "cc.zorcc.tenet.serdeproc.example.EBean")
public final class EBeanExample implements Refer<EBean> {
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final EBeanExample SINGLETON = new EBeanExample();

    private EBeanExample() {
        if(!initialized.compareAndSet(false, true)) {
            throw new IllegalStateException("BeanExample already initialized");
        }
    }

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(EBean.class, MethodHandles.lookup());
            intValueHandle = lookup.findVarHandle(EBean.class, "intValue", Integer.class);
            listHandle = lookup.findVarHandle(EBean.class, "list", List.class);
            doubleListHandle = lookup.findVarHandle(EBean.class, "doubleList", List.class);
            mapHandle = lookup.findVarHandle(EBean.class, "map", Map.class);
            SerdeContext.registerRefer(EBean.class, SINGLETON);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final VarHandle intValueHandle;
    private static final VarHandle listHandle;
    private static final VarHandle doubleListHandle;
    private static final VarHandle mapHandle;

    private final Col<EBean> intValueCol = new Col<>("intValue", Form.of(int.class),
            EBeanExample::intValueTagMapping, EBeanExample::intValueAssign, EBeanExample::intValueGet);
    private final Col<EBean> listCol = new Col<>("list", Form.of(List.class, Form.L, String.class, Form.R),
            EBeanExample::listTagMapping, EBeanExample::listAssign, EBeanExample::listGet);
    private final Col<EBean> doubleListCol = new Col<>("doubleList", Form.of(List.class, Form.L, List.class, Form.L, String.class, Form.R, Form.R),
            TagMappingFunc.NULLIFY, EBeanExample::doubleListAssign, EBeanExample::doubleListGet);
    private final Col<EBean> mapCol = new Col<>("map", Form.of(Map.class, Form.L, Integer.class, String.class, Form.R),
            TagMappingFunc.NULLIFY, EBeanExample::mapAssign, EBeanExample::mapGet);

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

    private static void intValueAssign(Builder<EBean> builder, Object value) {
        if(builder instanceof Wrapper wrapper) {
            wrapper.intValue = (Integer) value;
        } else {
            throw new ClassCastException("class %s cannot be cast to class wrapper".formatted(builder.getClass()));
        }
    }

    @SuppressWarnings("unchecked")
    private static void listAssign(Builder<EBean> builder, Object value) {
        if(builder instanceof Wrapper wrapper) {
            wrapper.list = (List<String>) value;
        } else {
            throw new ClassCastException("class %s cannot be cast to class wrapper".formatted(builder.getClass()));
        }
    }

    @SuppressWarnings("unchecked")
    private static void doubleListAssign(Builder<EBean> builder, Object value) {
        if(builder instanceof Wrapper wrapper) {
            wrapper.doubleList = (List<List<String>>) value;
        } else {
            throw new ClassCastException("class %s cannot be cast to class wrapper".formatted(builder.getClass()));
        }
    }

    @SuppressWarnings("unchecked")
    private static void mapAssign(Builder<EBean> builder, Object value) {
        if(builder instanceof Wrapper wrapper) {
            wrapper.map = (Map<Integer, String>) value;
        } else {
            throw new ClassCastException("class %s cannot be cast to class wrapper".formatted(builder.getClass()));
        }
    }

    private static Integer intValueGet(EBean instance) {
        return (Integer) intValueHandle.get(instance);
    }

    @SuppressWarnings("unchecked")
    private static List<String> listGet(EBean instance) {
        return (List<String>) listHandle.get(instance);
    }

    @SuppressWarnings("unchecked")
    private static List<List<String>> doubleListGet(EBean instance) {
        return (List<List<String>>) doubleListHandle.get(instance);
    }

    @SuppressWarnings("unchecked")
    private static Map<Integer, String> mapGet(EBean instance) {
        return (Map<Integer, String>) mapHandle.get(instance);
    }

    private static final class Wrapper implements Builder<EBean> {
        private static final EBean[] values = EBean.values();
        private final AtomicBoolean flag = new AtomicBoolean(false);
        private Integer intValue;
        private List<String> list;
        private List<List<String>> doubleList;
        private Map<Integer, String> map;

        @Override
        public @Nullable EBean build() {
            if(flag.compareAndSet(false, true)) {
                for (EBean value : values) {
                    if(Objects.equals(intValueGet(value), intValue) &&
                    Objects.equals(listGet(value), list) &&
                    Objects.equals(doubleListGet(value), doubleList) &&
                    Objects.equals(mapGet(value), map)) {
                        return value;
                    }
                }
                return null;
            }else throw new IllegalCallerException("build() should only be invoked once");
        }
    }

    @Override
    public @NonNull List<String> fields() {
        return List.of("intValue", "list", "doubleList", "map");
    }

    @Override
    public @NonNull Builder<EBean> builder() {
        return new Wrapper();
    }

    @Override
    public @Nullable Col<EBean> col(@NonNull String colName) {
        return switch (colName) {
            case "intValue" -> intValueCol;
            case "list" -> listCol;
            case "doubleList" -> doubleListCol;
            case "map" -> mapCol;
            default -> null;
        };
    }

    @Override
    public @Nullable EBean byName(@NonNull String name) {
        return switch (name) {
            case "Test1" -> EBean.Test1;
            case "Test2" -> EBean.Test2;
            default -> null;
        };
    }
}
