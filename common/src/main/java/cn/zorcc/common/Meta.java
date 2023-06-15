package cn.zorcc.common;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ClassUtil;

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *   Meta is for accessing the plain old java object with Lombok @Data described getter and setter methods
 *   Target class should only have getter and setter for its fields, thus easy to analysis
 */
public final class Meta {
    private static final Map<Class<?>, Meta> metaMap = new ConcurrentHashMap<>();
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
    private static final String GET = "get";
    private static final String APPLY = "apply";
    private static final String ACCEPT = "accept";

    public static Meta of(Class<?> clazz) {
        return metaMap.computeIfAbsent(clazz, Meta::register);
    }

    private final Class<?> clazz;
    private final Supplier<Object> constructor;
    private final Map<String, MetaInfo> metaInfoMap;

    private Meta(Class<?> clazz, Supplier<Object> constructor, Map<String, MetaInfo> metaInfoMap) {
        this.clazz = clazz;
        this.constructor = constructor;
        this.metaInfoMap = metaInfoMap;
    }

    public Class<?> clazz() {
        return clazz;
    }

    public Supplier<Object> constructor() {
        return constructor;
    }

    public MetaInfo metaInfo(String fieldName) {
        return metaInfoMap.get(fieldName);
    }

    private static Meta register(Class<?> clazz) {
        try{
            if(clazz.isRecord()) {
                throw new FrameworkException(ExceptionType.CONTEXT, "Record is not supported and recommended in this scenario");
            }
            Supplier<Object> constructor = createConstructor(clazz);
            Map<String, MetaInfo> metaInfoMap = new HashMap<>();
            List<Field> allFields = ClassUtil.getAllFields(clazz);
            for (Field f : allFields) {
                String fieldName = f.getName();
                Class<?> fieldType = f.getType();
                Function<Object, Object> getter = createGetter(clazz, fieldName, fieldType);
                BiConsumer<Object, Object> setter = createSetter(clazz, fieldName, fieldType);
                metaInfoMap.put(fieldName, new MetaInfo(fieldName, fieldType, getter, setter));
            }
            return new Meta(clazz, constructor, Collections.unmodifiableMap(metaInfoMap));
        }catch (Throwable e) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Supplier<Object> createConstructor(Class<?> clazz) {
        try{
            MethodHandle cmh = lookup.findConstructor(clazz, MethodType.methodType(void.class));
            CallSite callSite = LambdaMetafactory.metafactory(lookup,
                    GET,
                    MethodType.methodType(Supplier.class),
                    cmh.type().generic(), cmh, cmh.type());
            return (Supplier<Object>) callSite.getTarget().invokeExact();
        }catch (Throwable e) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Function<Object, Object> createGetter(Class<?> clazz, String fieldName, Class<?> fieldType) {
        try{
            String methodName = ClassUtil.getterName(fieldName);
            MethodHandle mh = lookup.findVirtual(clazz, methodName, MethodType.methodType(fieldType));
            CallSite callSite = LambdaMetafactory.metafactory(lookup,
                    APPLY,
                    MethodType.methodType(Function.class),
                    MethodType.methodType(Object.class, Object.class), mh, MethodType.methodType(fieldType, clazz));
            return (Function<Object, Object>) callSite.getTarget().invokeExact();
        }catch (Throwable e) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static BiConsumer<Object, Object> createSetter(Class<?> clazz, String fieldName, Class<?> fieldType) {
        try{
            String methodName = ClassUtil.setterName(fieldName);
            MethodHandle mh = lookup.findVirtual(clazz, methodName, MethodType.methodType(void.class, fieldType));
            CallSite callSite = LambdaMetafactory.metafactory(lookup,
                    "accept",
                    MethodType.methodType(BiConsumer.class),
                    MethodType.methodType(void.class, Object.class, Object.class), mh, MethodType.methodType(void.class, clazz, fieldType));
            return (BiConsumer<Object, Object>) callSite.getTarget().invokeExact();
        }catch (Throwable e) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED, e);
        }
    }

    public Pair<List<String>, List<Object>> getAll(Object target, List<String> fields) {
        List<String> columns = new ArrayList<>();
        List<Object> result = new ArrayList<>();
        for (String field : fields) {
            MetaInfo metaInfo = metaInfoMap.get(field);
            if(metaInfo != null) {
                Object o = metaInfo.getter().apply(target);
                if(o != null) {
                    columns.add(field);
                    result.add(o);
                }
            }
        }
        return Pair.of(columns, result);
    }
}
