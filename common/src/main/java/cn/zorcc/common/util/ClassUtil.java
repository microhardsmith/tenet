package cn.zorcc.common.util;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
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
     * @param clazz 需要获取方法的类
     * @return 方法列表
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
     * @param clazz 需要获取字段的类
     * @return 字段列表
     */
    public static List<Field> getAllFields(Class<?> clazz) {
        return clazz.isRecord() ? Arrays.asList(clazz.getDeclaredFields()) : getAllFields(clazz, new ArrayList<>());
    }

    private static List<Field> getAllFields(Class<?> clazz, List<Field> list) {
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            getAllFields(superclass, list);
        }
        list.addAll(Arrays.asList(clazz.getDeclaredFields()));
        return list;
    }

    /**
     * 获取对象的get方法名
     * @param fieldName 字段名
     * @return 字段的get方法名
     */
    public static String getterName(String fieldName) {
        if (fieldName.length() < 1) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Unresolved fieldName");
        }
        return Constants.GET + fieldName.substring(Constants.ZERO, Constants.ONE).toUpperCase() + fieldName.substring(Constants.ONE);
    }

    /**
     * 获取对象的set方法名
     * @param fieldName 字段名
     * @return 字段的set方法名
     */
    public static String setterName(String fieldName) {
        if (fieldName.length() < 1) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Unresolved fieldName");
        }
        return Constants.SET + fieldName.substring(Constants.ZERO, Constants.ONE).toUpperCase() + fieldName.substring(Constants.ONE);
    }


    /**
     * 寻找指定成员函数methodHandle,如果不存在则返回null
     * @param lookup 可访问私有方法的privateLookUp
     * @param clazz 包含指定方法的类
     * @param methodName 方法名
     * @param methodType 方法参数类型
     * @return 成员方法对应MethodHandle
     */
    public static MethodHandle findHandle(MethodHandles.Lookup lookup, Class<?> clazz, String methodName, MethodType methodType) {
        try{
            return lookup.findSpecial(clazz, methodName, methodType, clazz);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            return null;
        }
    }

    /**
     * 寻找指定构造函数methodHandle,如果不存在则返回null
     * @param lookup 可访问私有方法的privateLookUp
     * @param clazz 需要构造的类
     * @param methodType 构造函数类型
     * @return 构造函数对应MethodHandle
     */
    public static MethodHandle findConstructor(MethodHandles.Lookup lookup, Class<?> clazz, MethodType methodType) {
        try{
            return lookup.findConstructor(clazz, methodType);
        }catch (NoSuchMethodException | IllegalAccessException e) {
            return null;
        }
    }

    /**
     * 获取指定类对象的私有lookUp方法
     * @param clazz 需要访问的类对象
     * @return 私有访问lookup
     */
    public static MethodHandles.Lookup lookup(Class<?> clazz) {
        try{
             return MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
        }catch (IllegalAccessException e) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Unable to lookUp target class");
        }
    }
}
