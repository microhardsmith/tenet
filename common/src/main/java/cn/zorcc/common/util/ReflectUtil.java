package cn.zorcc.common.util;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ReflectUtil {

    private ReflectUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     *  Recursively gets all methods of the class, including methods declared in the parent class
     */
    public static List<Method> getAllMethod(Class<?> clazz) {
        if(clazz.isRecord() || clazz.isEnum() || clazz.isPrimitive() || clazz.isArray()) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Unsupported reflection usage");
        }
        return getAllMethod(clazz, new ArrayList<>());
    }

    private static List<Method> getAllMethod(Class<?> clazz, List<Method> list) {
        while (clazz != null) {
            list.addAll(Arrays.asList(clazz.getDeclaredMethods()));
            clazz = clazz.getSuperclass();
        }
        return list;
    }

    /**
     *  Recursively gets all fields of the class, including fields declared in the parent class
     */
    public static List<Field> getAllFields(Class<?> clazz) {
        if(clazz.isRecord() || clazz.isEnum() || clazz.isPrimitive() || clazz.isArray()) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Unsupported reflection usage");
        }
        return getAllFields(clazz, new ArrayList<>());
    }

    private static List<Field> getAllFields(Class<?> clazz, List<Field> list) {
        while (clazz != null) {
            list.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return list;
    }

    public static String getterName(Class<?> fieldType, String fieldName) {
        return (fieldType == boolean.class ? Constants.IS : Constants.GET) + fieldName.substring(Constants.ZERO, Constants.ONE).toUpperCase() + fieldName.substring(Constants.ONE);
    }

    public static String setterName(String fieldName) {
        return Constants.SET + fieldName.substring(Constants.ZERO, Constants.ONE).toUpperCase() + fieldName.substring(Constants.ONE);
    }
}
