package cn.zorcc.common;

import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ReflectUtil;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class Record<T> {
    private static final Map<Class<?>, Record<?>> recordMap = new ConcurrentHashMap<>(Constants.KB);

    @SuppressWarnings("unchecked")
    public static <T> Record<T> of(Class<T> clazz) {
        return (Record<T>) recordMap.computeIfAbsent(clazz, Record::register);
    }

    private final Class<T> clazz;
    private final Object[] elementArray;
    private final Map<String, Integer> elementIndexMap;
    private final Function<Object[], T> constructor;
    private final Map<String, RecordInfo> recordInfoMap;
    private final List<RecordInfo> recordInfoList;

    public Record(Class<T> clazz, Object[] elementArray, Map<String, Integer> elementIndexMap, Function<Object[], T> constructor, Map<String, RecordInfo> recordInfoMap) {
        this.clazz = clazz;
        this.elementArray = elementArray;
        this.elementIndexMap = elementIndexMap;
        this.constructor = constructor;
        this.recordInfoMap = recordInfoMap;
        this.recordInfoList = recordInfoMap.values().stream().toList();
    }

    public Class<T> clazz() {
        return clazz;
    }

    public Object[] createElementArray() {
        return Arrays.copyOf(elementArray, elementArray.length);
    }

    public T construct(Object[] args) {
        return constructor.apply(args);
    }

    public void assign(Object[] args, RecordInfo recordInfo, Object value) {
        Integer index = elementIndexMap.get(recordInfo.fieldName());
        if(index != null) {
            args[index] = value;
        }
    }

    public RecordInfo recordInfo(String fieldName) {
        return recordInfoMap.get(fieldName);
    }

    public List<RecordInfo> recordInfoList() {
        return recordInfoList;
    }

    private static <T> Record<T> register(Class<T> recordClass) {
        try{
            if(!recordClass.isRecord()) {
                throw new FrameworkException(ExceptionType.CONTEXT, "Only record type supported");
            }
            Field[] fields = recordClass.getDeclaredFields();
            List<Class<?>> parameterTypes = new ArrayList<>();
            Object[] elementArray = new Object[fields.length];
            Map<String, Integer> elementIndexMap = new HashMap<>();
            int index = 0;
            for (Field f : fields) {
                Class<?> fieldType = f.getType();
                parameterTypes.add(fieldType);
                if(fieldType.isPrimitive()) {
                    elementArray[index] = getDefaultPrimitiveValue(fieldType);
                }
                elementIndexMap.put(f.getName(), index++);
            }
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(recordClass, MethodHandles.lookup());
            Function<Object[], T> constructor = ReflectUtil.createRecordConstructor(lookup, recordClass, parameterTypes);
            Map<String, RecordInfo> recordInfoMap = new LinkedHashMap<>();
            for (Field f : Arrays.stream(fields).sorted((o1, o2) -> {
                Ordinal a1 = o1.getAnnotation(Ordinal.class);
                Ordinal a2 = o2.getAnnotation(Ordinal.class);
                return (a1 == null || a2 == null) ? 0 : Integer.compare(a1.sequence(), a2.sequence());
            }).toList()) {
                String fieldName = f.getName();
                Class<?> fieldClass = f.getType();
                Type genericType = f.getGenericType();
                Function<Object, Object> getter = ReflectUtil.createGetter(lookup, lookup.findVirtual(recordClass, fieldName, MethodType.methodType(fieldClass)), fieldClass);
                recordInfoMap.put(fieldName, new RecordInfo(fieldName, fieldClass, genericType, getter, f.isAnnotationPresent(Format.class) ? f.getAnnotation(Format.class) : null));
            }
            return new Record<>(recordClass, elementArray, elementIndexMap, constructor, Collections.unmodifiableMap(recordInfoMap));
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED, throwable);
        }
    }

    private static Object getDefaultPrimitiveValue(Class<?> clazz) {
        return switch (clazz) {
            case Class<?> t when t == boolean.class -> Boolean.FALSE;
            case Class<?> t when t == byte.class -> (byte) 0;
            case Class<?> t when t == short.class -> (short) 0;
            case Class<?> t when t == int.class -> 0;
            case Class<?> t when t == long.class -> 0L;
            case Class<?> t when t == float.class -> 0.0f;
            case Class<?> t when t == double.class -> 0.0d;
            case Class<?> t when t == char.class -> '\u0000';
            default -> throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
        };
    }

}
