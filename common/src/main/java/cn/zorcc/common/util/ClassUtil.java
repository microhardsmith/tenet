package cn.zorcc.common.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *  Class对象工具类
 */
public class ClassUtil {

    private ClassUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * 递归获取类所有的method,包括父类中声明的method
     */
    public static List<Method> getAllMethod(Class<?> clazz) {
        return getAllMethod(clazz, new ArrayList<>());
    }

    private static List<Method> getAllMethod(Class<?> clazz, List<Method> list) {
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            getAllMethod(superclass, list);
        }
        list.addAll(Arrays.asList(clazz.getDeclaredMethods()));
        return list;
    }

    /**
     * 递归获取类所有的field,包括父类中声明的字段
     */
    public static List<Field> getAllFields(Class<?> clazz) {
        return getAllFields(clazz, new ArrayList<>());
    }

    private static List<Field> getAllFields(Class<?> clazz, List<Field> list) {
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            getAllFields(superclass, list);
        }
        list.addAll(Arrays.asList(clazz.getDeclaredFields()));
        return list;
    }
}
