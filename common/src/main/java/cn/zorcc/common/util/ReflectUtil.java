package cn.zorcc.common.util;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ReflectUtil {
    private static final Map<Class<?>, Class<?>> wrapperMap = Map.of(byte.class, Byte.class,
            short.class, Short.class,
            int.class, Integer.class,
            long.class, Long.class,
            float.class, Float.class,
            double.class, Double.class,
            boolean.class, Boolean.class,
            char.class, Character.class);

    private ReflectUtil() {
        throw new UnsupportedOperationException();
    }

    public static Class<?> getWrapperClass(Class<?> primitiveClass) {
        return wrapperMap.get(primitiveClass);
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


    /**
     *   Create a constructor method using lambda
     */
    @SuppressWarnings("unchecked")
    public static <T> Supplier<T> createConstructor(Class<T> type) {
        try{
            MethodHandle cmh = Constants.LOOKUP.findConstructor(type, MethodType.methodType(void.class));
            MethodType methodType = cmh.type();
            CallSite callSite = LambdaMetafactory.metafactory(Constants.LOOKUP,
                    Constants.GET,
                    MethodType.methodType(Supplier.class),
                    methodType.erase(), cmh, methodType);
            return (Supplier<T>) callSite.getTarget().invokeExact();
        }catch (Throwable e) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Target class %s lacks a parameterless constructor".formatted(type.getName()), e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Function<Object[], T> createRecordConstructor(Class<T> recordClass, List<Class<?>> parameterTypes) {
        try{
            MethodHandle cmh = MethodHandles.lookup().findConstructor(recordClass, MethodType.methodType(void.class, parameterTypes)).asSpreader(Object[].class, parameterTypes.size());
            return (Function<Object[], T>) MethodHandleProxies.asInterfaceInstance(Function.class, cmh);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED, throwable);
        }
    }

    /**
     *   Create a getter method using lambda
     */
    @SuppressWarnings("unchecked")
    public static Function<Object, Object> createGetter(MethodHandle mh, Class<?> fieldClass) {
        try{
            MethodType type = mh.type();
            if(fieldClass.isPrimitive()) {
                type = type.changeReturnType(ReflectUtil.getWrapperClass(fieldClass));
            }
            CallSite callSite = LambdaMetafactory.metafactory(Constants.LOOKUP,
                    Constants.APPLY,
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
    public static BiConsumer<Object, Object> createSetter(MethodHandle mh, Class<?> fieldClass) {
        try{
            MethodType type = mh.type();
            if(fieldClass.isPrimitive()) {
                type = type.changeParameterType(Constants.ONE, ReflectUtil.getWrapperClass(fieldClass));
            }
            CallSite callSite = LambdaMetafactory.metafactory(Constants.LOOKUP,
                    Constants.ACCEPT,
                    MethodType.methodType(BiConsumer.class),
                    type.erase(), mh, type);
            return (BiConsumer<Object, Object>) callSite.getTarget().invokeExact();
        }catch (Throwable e) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED, e);
        }
    }
}
