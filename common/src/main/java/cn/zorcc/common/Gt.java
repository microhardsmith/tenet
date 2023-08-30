package cn.zorcc.common;

import cn.zorcc.common.anno.Ordinal;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ReflectUtil;

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *   Gt stands for better accessing getter and setter methods
 */
public final class Gt<T> {
    private static final Map<Class<?>, Gt<?>> gtMap = new ConcurrentHashMap<>();
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
    private static final String GET = "get";
    private static final String APPLY = "apply";
    private static final String ACCEPT = "accept";
    private static final Map<Class<?>, Class<?>> wrapperMap = Map.of(byte.class, Byte.class,
            short.class, Short.class,
            int.class, Integer.class,
            long.class, Long.class,
            float.class, Float.class,
            double.class, Double.class,
            boolean.class, Boolean.class,
            char.class, Character.class);
    private static final Map<Class<?>, Map<String, Object>> enumCacheMap = new ConcurrentHashMap<>();


    @SuppressWarnings("unchecked")
    public static <T> Gt<T> of(Class<T> clazz) {
        return (Gt<T>) gtMap.computeIfAbsent(clazz, Gt::register);
    }

    private final Class<T> clazz;
    private final Supplier<T> constructor;
    private final Map<String, MetaInfo> metaInfoMap;
    private final List<MetaInfo> metaInfoList;

    private Gt(Class<T> clazz, Supplier<T> constructor, Map<String, MetaInfo> metaInfoMap, List<MetaInfo> metaInfoList) {
        this.clazz = clazz;
        this.constructor = constructor;
        this.metaInfoMap = metaInfoMap;
        this.metaInfoList = metaInfoList;
    }

    public Class<T> clazz() {
        return clazz;
    }

    public Supplier<T> constructor() {
        return constructor;
    }

    public MetaInfo metaInfo(String fieldName) {
        return metaInfoMap.get(fieldName);
    }

    public List<MetaInfo> metaInfoList() {
        return metaInfoList;
    }

    private static <T> Gt<T> register(Class<T> clazz) {
        try{
            if(clazz.isPrimitive() || clazz.isAnnotation() || clazz.isRecord() || clazz.isMemberClass()) {
                throw new FrameworkException(ExceptionType.CONTEXT, "Unsupported class type");
            }
            Supplier<T> constructor = createConstructor(clazz);
            Map<String, MetaInfo> metaInfoMap = new HashMap<>();
            List<MetaInfo> metaInfoList = new ArrayList<>();
            for (Field f : ReflectUtil.getAllFields(clazz).stream().sorted((o1, o2) -> {
                Ordinal a1 = o1.getAnnotation(Ordinal.class);
                Ordinal a2 = o2.getAnnotation(Ordinal.class);
                if(a1 == null || a2 == null) {
                    return Constants.ZERO;
                }else {
                    return Integer.compare(a1.sequence(), a2.sequence());
                }
            }).toList()) {
                String fieldName = f.getName();
                Class<?> fieldType = f.getType();
                String getterMethodName = ReflectUtil.getterName(fieldType, fieldName);
                MethodHandle gmh = lookup.findVirtual(clazz, getterMethodName, MethodType.methodType(fieldType));
                Function<Object, Object> getter = createGetter(gmh, fieldType);
                String setterMethodName = ReflectUtil.setterName(fieldName);
                MethodHandle smh = lookup.findVirtual(clazz, setterMethodName, MethodType.methodType(void.class, fieldType));
                BiConsumer<Object, Object> setter = createSetter(smh, fieldType);
                Map<String, Object> enumMap = fieldType.isEnum() ? enumCacheMap.computeIfAbsent(fieldType, c -> {
                    Map<String, Object> m = new HashMap<>();
                    for (Object enumConstant : fieldType.getEnumConstants()) {
                        m.put(enumConstant.toString(), enumConstant);
                    }
                    return Collections.unmodifiableMap(m);
                }) : null;
                MetaInfo metaInfo = new MetaInfo(fieldName, fieldType, gmh, smh, getter, setter, enumMap);
                metaInfoMap.put(fieldName, metaInfo);
                metaInfoList.add(metaInfo);
            }
            return new Gt<>(clazz, constructor, Collections.unmodifiableMap(metaInfoMap), Collections.unmodifiableList(metaInfoList));
        }catch (Throwable e) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED, e);
        }
    }

    /**
     *   Create a constructor method using lambda
     */
    @SuppressWarnings("unchecked")
    private static <T> Supplier<T> createConstructor(Class<T> clazz) {
        try{
            MethodHandle cmh = lookup.findConstructor(clazz, MethodType.methodType(void.class));
            CallSite callSite = LambdaMetafactory.metafactory(lookup,
                    GET,
                    MethodType.methodType(Supplier.class),
                    cmh.type().generic(), cmh, cmh.type());
            return (Supplier<T>) callSite.getTarget().invokeExact();
        }catch (Throwable e) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Target class %s lacks a parameterless constructor".formatted(clazz.getName()), e);
        }
    }

    /**
     *   Create a getter method using lambda
     */
    @SuppressWarnings("unchecked")
    private static Function<Object, Object> createGetter(MethodHandle mh, Class<?> fieldType) {
        try{
            MethodType type = mh.type();
            if(fieldType.isPrimitive()) {
                type = type.changeReturnType(wrapperMap.get(fieldType));
            }
            CallSite callSite = LambdaMetafactory.metafactory(lookup,
                    APPLY,
                    MethodType.methodType(Function.class),
                    type.erase(), mh, type);
            return (Function<Object, Object>) callSite.getTarget().invokeExact();
        }catch (Throwable e) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED, e);
        }
    }

    /**
     *   Create a setter method using lambda
     */
    @SuppressWarnings("unchecked")
    private static BiConsumer<Object, Object> createSetter(MethodHandle mh, Class<?> fieldType) {
        try{
            MethodType type = mh.type();
            if(fieldType.isPrimitive()) {
                type = type.changeParameterType(1, wrapperMap.get(fieldType));
            }
            CallSite callSite = LambdaMetafactory.metafactory(lookup,
                    ACCEPT,
                    MethodType.methodType(BiConsumer.class),
                    type.erase(), mh, type);
            return (BiConsumer<Object, Object>) callSite.getTarget().invokeExact();
        }catch (Throwable e) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED, e);
        }
    }

    /**
     *   Obtain all the field names with their values from target
     *   the returned fields could be different from parameter fields if the target has no such field
     */
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
