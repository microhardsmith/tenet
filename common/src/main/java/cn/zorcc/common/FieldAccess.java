package cn.zorcc.common;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ClassUtil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class FieldAccess {
    /**
     *  对象类型
     */
    private final Class<?> clazz;
    /**
     *  构造函数,对于普通Bean是无参构造函数,对于Record而言是全参数构造函数
     */
    private final MethodHandle constructor;
    /**
     *  字段访问域
     */
    private final Map<String, Access> accessMap;
    private FieldAccess(Class<?> clazz, MethodHandle constructor, Map<String, Access> accessMap) {
        this.clazz = clazz;
        this.constructor = constructor;
        this.accessMap = Collections.unmodifiableMap(accessMap);
    }

    private static final Map<Class<?>, FieldAccess> fieldAccessMap = new ConcurrentHashMap<>();

    /**
     * 构造对象Access
     * @param clazz 类对象
     * @return 类对象字段访问器
     */
    public static FieldAccess get(Class<?> clazz) {
        return fieldAccessMap.computeIfAbsent(clazz, FieldAccess::create);
    }

    private static FieldAccess create(Class<?> clazz) {
        boolean isRecord = clazz.isRecord();
        List<Field> allFields = ClassUtil.getAllFields(clazz);
        Map<String, Access> accessMap = new LinkedHashMap<>(allFields.size());
        MethodHandles.Lookup lookup = ClassUtil.lookup(clazz);
        MethodType constructorMethodType = isRecord ?
                MethodType.methodType(void.class, allFields.stream().map(Field::getType).toArray(Class<?>[]::new)) :
                MethodType.methodType(void.class);
        MethodHandle constructor =ClassUtil.findConstructor(lookup, clazz, constructorMethodType);
        if(constructor == null) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Unable to locate no-arg constructor for %s", clazz.getName());
        }
        for (Field field : allFields) {
            String fieldName = field.getName();
            Class<?> fieldType = field.getType();
            MethodType getterType = MethodType.methodType(fieldType);
            // 先尝试record形式的get方法
            MethodHandle getter = ClassUtil.findHandle(lookup, clazz, fieldName, getterType);
            if(getter == null) {
                // 尝试普通类型的get方法
                getter = ClassUtil.findHandle(lookup, clazz, ClassUtil.getterName(fieldName), getterType);
                if(getter == null) {
                    // 未找到对应的getter方法
                    continue;
                }
            }
            if(isRecord) {
                accessMap.put(fieldName, new Access(fieldName, fieldType, getter, null));
            }else {
                MethodType setterType = MethodType.methodType(void.class, fieldType);
                MethodHandle setter = ClassUtil.findHandle(lookup, clazz, ClassUtil.setterName(fieldName), setterType);
                if(setter == null) {
                    // 未找到对应的setter方法
                    continue;
                }
                accessMap.put(fieldName, new Access(fieldName, fieldType, getter, setter));
            }
        }
        return new FieldAccess(clazz, constructor, accessMap);
    }

    public Access access(String name) {
        return accessMap.get(name);
    }

}
