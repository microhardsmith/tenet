package cn.zorcc.common;

import cn.zorcc.common.anno.Format;
import cn.zorcc.common.anno.Ordinal;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ReflectUtil;

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *   Gt stands for better accessing getter and setter methods
 */
public final class Meta<T> {
    private static final Map<Class<?>, Meta<?>> metaMap = new ConcurrentHashMap<>();
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
    private static final String GET = "get";
    private static final String APPLY = "apply";
    private static final String ACCEPT = "accept";
    private static final Map<Class<?>, Map<String, Object>> enumCacheMap = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> Meta<T> of(Class<T> clazz) {
        return (Meta<T>) metaMap.computeIfAbsent(clazz, Meta::register);
    }

    private final Class<T> clazz;
    private final Supplier<T> constructor;
    private final Map<String, MetaInfo> metaInfoMap;
    private final List<MetaInfo> metaInfoList;
    private final Map<String, Object> enumMap;

    private Meta(Class<T> clazz, Supplier<T> constructor, Map<String, MetaInfo> metaInfoMap, List<MetaInfo> metaInfoList, Map<String, Object> enumMap) {
        this.clazz = clazz;
        this.constructor = constructor;
        this.metaInfoMap = metaInfoMap;
        this.metaInfoList = metaInfoList;
        this.enumMap = enumMap;
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

    public Map<String, Object> enumMap() {
        return enumMap;
    }

    private static <T> Meta<T> register(Class<T> type) {
        try{
            if(type.isPrimitive() || type.isAnnotation() || type.isRecord() || type.isMemberClass()) {
                throw new FrameworkException(ExceptionType.CONTEXT, "Unsupported class type");
            }
            Map<String, Object> enumMap = type.isEnum() ? enumCacheMap.computeIfAbsent(type, c -> {
                Map<String, Object> m = new HashMap<>();
                for (Object enumConstant : type.getEnumConstants()) {
                    m.put(enumConstant.toString(), enumConstant);
                }
                return Collections.unmodifiableMap(m);
            }) : null;
            Supplier<T> constructor = createConstructor(type);
            Map<String, MetaInfo> metaInfoMap = new HashMap<>();
            for (Field f : ReflectUtil.getAllFields(type).stream().sorted((o1, o2) -> {
                Ordinal a1 = o1.getAnnotation(Ordinal.class);
                Ordinal a2 = o2.getAnnotation(Ordinal.class);
                return (a1 == null || a2 == null) ? Constants.ZERO : Integer.compare(a1.sequence(), a2.sequence());
            }).toList()) {
                String fieldName = f.getName();
                Class<?> fieldType = f.getType();
                Type genericType = f.getGenericType();
                String getterMethodName = ReflectUtil.getterName(fieldType, fieldName);
                Function<Object, Object> getter = createGetter(lookup.findVirtual(type, getterMethodName, MethodType.methodType(fieldType)), fieldType);
                String setterMethodName = ReflectUtil.setterName(fieldName);
                BiConsumer<Object, Object> setter = createSetter(lookup.findVirtual(type, setterMethodName, MethodType.methodType(void.class, fieldType)), fieldType);
                MetaInfo metaInfo = new MetaInfo(fieldName, fieldType, genericType, getter, setter, f.isAnnotationPresent(Format.class) ? f.getAnnotation(Format.class) : null);
                metaInfoMap.put(fieldName, metaInfo);
            }
            return new Meta<>(type, constructor, Collections.unmodifiableMap(metaInfoMap), metaInfoMap.values().stream().toList(), enumMap);
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
    private static Function<Object, Object> createGetter(MethodHandle mh, Class<?> fieldClass) {
        try{
            MethodType type = mh.type();
            if(fieldClass.isPrimitive()) {
                type = type.changeReturnType(ReflectUtil.getWrapperClass(fieldClass));
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
    private static BiConsumer<Object, Object> createSetter(MethodHandle mh, Class<?> fieldClass) {
        try{
            MethodType type = mh.type();
            if(fieldClass.isPrimitive()) {
                type = type.changeParameterType(Constants.ONE, ReflectUtil.getWrapperClass(fieldClass));
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
