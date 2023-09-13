package cn.zorcc.common;

import cn.zorcc.common.anno.Format;
import cn.zorcc.common.anno.Ordinal;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ReflectUtil;

import java.lang.invoke.*;
import java.lang.reflect.Field;
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
    private final Function<Object[], T> constructor;
    private final Map<String, RecordInfo> recordInfoMap;
    private final List<RecordInfo> recordInfoList;

    public Record(Class<T> clazz, Function<Object[], T> constructor, Map<String, RecordInfo> recordInfoMap) {
        this.clazz = clazz;
        this.constructor = constructor;
        this.recordInfoMap = recordInfoMap;
        this.recordInfoList = recordInfoMap.values().stream().toList();
    }

    public Class<T> clazz() {
        return clazz;
    }

    public Function<Object[], T> constructor() {
        return constructor;
    }

    public RecordInfo recordInfo(String fieldName) {
        return recordInfoMap.get(fieldName);
    }

    public List<RecordInfo> recordInfoList() {
        return recordInfoList;
    }

    private static <T> Record<T> register(Class<T> type) {
        try{
            if(!type.isRecord()) {
                throw new FrameworkException(ExceptionType.CONTEXT, "Only record type supported");
            }
            Field[] fields = type.getDeclaredFields();
            Function<Object[], T> constructor = ReflectUtil.createRecordConstructor(type, fields);
            Map<String, RecordInfo> recordInfoMap = new LinkedHashMap<>();
            for (Field f : Arrays.stream(fields).sorted((o1, o2) -> {
                Ordinal a1 = o1.getAnnotation(Ordinal.class);
                Ordinal a2 = o2.getAnnotation(Ordinal.class);
                return (a1 == null || a2 == null) ? Constants.ZERO : Integer.compare(a1.sequence(), a2.sequence());
            }).toList()) {
                String fieldName = f.getName();
                Class<?> fieldType = f.getType();
                Function<Object, Object> getter = ReflectUtil.createGetter(Constants.LOOKUP.findVirtual(type, fieldName, MethodType.methodType(fieldType)), fieldType);
                recordInfoMap.put(fieldName, new RecordInfo(fieldName, getter, f.isAnnotationPresent(Format.class) ? f.getAnnotation(Format.class) : null));
            }
            return new Record<>(type, constructor, Collections.unmodifiableMap(recordInfoMap));
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED, throwable);
        }
    }


}
