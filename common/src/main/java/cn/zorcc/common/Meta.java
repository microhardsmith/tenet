package cn.zorcc.common;

import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ReflectUtil;

import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *   Meta is used for better accessing getter and setter methods with lmf
 */
public final class Meta<T> {
    private static final Map<Class<?>, Meta<?>> metaMap = new ConcurrentHashMap<>(Constants.KB);
    private static final Map<Class<?>, Map<String, Object>> enumCacheMap = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> Meta<T> of(Class<T> objectClass) {
        return (Meta<T>) metaMap.computeIfAbsent(objectClass, Meta::register);
    }

    private final Class<T> clazz;
    private final Supplier<T> constructor;
    private final Map<String, MetaInfo> metaInfoMap;
    private final List<MetaInfo> metaInfoList;
    private final Map<String, Object> enumMap;

    private Meta(Class<T> clazz, Supplier<T> constructor, Map<String, MetaInfo> metaInfoMap, Map<String, Object> enumMap) {
        this.clazz = clazz;
        this.constructor = constructor;
        this.metaInfoMap = metaInfoMap;
        this.metaInfoList = metaInfoMap.values().stream().toList();
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

    private static <T> Meta<T> register(Class<T> objectClass) {
        try{
            if(objectClass.isPrimitive() || objectClass.isAnnotation() || objectClass.isRecord() || objectClass.isMemberClass()) {
                throw new FrameworkException(ExceptionType.CONTEXT, "Unsupported class type");
            }
            Map<String, Object> enumMap = objectClass.isEnum() ? enumCacheMap.computeIfAbsent(objectClass, t -> {
                Map<String, Object> m = new HashMap<>();
                for (Object enumConstant : t.getEnumConstants()) {
                    m.put(enumConstant.toString(), enumConstant);
                }
                return Collections.unmodifiableMap(m);
            }) : null;
            Supplier<T> constructor = ReflectUtil.createConstructor(objectClass);
            Map<String, MetaInfo> metaInfoMap = new LinkedHashMap<>();
            for (Field f : ReflectUtil.getAllFields(objectClass).stream().sorted((o1, o2) -> {
                Ordinal a1 = o1.getAnnotation(Ordinal.class);
                Ordinal a2 = o2.getAnnotation(Ordinal.class);
                return (a1 == null || a2 == null) ? 0 : Integer.compare(a1.sequence(), a2.sequence());
            }).toList()) {
                String fieldName = f.getName();
                Class<?> fieldType = f.getType();
                Type genericType = f.getGenericType();
                String getterMethodName = ReflectUtil.getterName(fieldType, fieldName);
                Function<Object, Object> getter =ReflectUtil.createGetter(Constants.LOOKUP.findVirtual(objectClass, getterMethodName, MethodType.methodType(fieldType)), fieldType);
                String setterMethodName = ReflectUtil.setterName(fieldName);
                BiConsumer<Object, Object> setter = ReflectUtil.createSetter(Constants.LOOKUP.findVirtual(objectClass, setterMethodName, MethodType.methodType(void.class, fieldType)), fieldType);
                MetaInfo metaInfo = new MetaInfo(fieldName, fieldType, genericType, getter, setter, f.isAnnotationPresent(Format.class) ? f.getAnnotation(Format.class) : null);
                metaInfoMap.put(fieldName, metaInfo);
            }
            return new Meta<>(objectClass, constructor, Collections.unmodifiableMap(metaInfoMap), enumMap);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED, throwable);
        }
    }


    /**
     *   TODO refactor
     *   Obtain all the field names with their values from target
     *   the returned fields could be different from parameter fields if the target has no such field
     */
    public OldPair<List<String>, List<Object>> getAll(Object target, List<String> fields) {
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
        return OldPair.of(columns, result);
    }
}
