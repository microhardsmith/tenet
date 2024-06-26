package cn.zorcc.common.util;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *   Reflection helper class
 */
public final class ReflectUtil {

    private ReflectUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     *   Get the corresponding wrapper type based on its primitive type
     */
    public static Class<?> getWrapperClass(Class<?> primitiveClass) {
        return switch (primitiveClass) {
            case Class<?> t when t == byte.class -> Byte.class;
            case Class<?> t when t == short.class -> Short.class;
            case Class<?> t when t == int.class -> Integer.class;
            case Class<?> t when t == long.class -> Long.class;
            case Class<?> t when t == float.class -> Float.class;
            case Class<?> t when t == double.class -> Double.class;
            case Class<?> t when t == char.class -> Character.class;
            case Class<?> t when t == boolean.class -> Boolean.class;
            default -> throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
        };
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
        return (fieldType == boolean.class ? Constants.IS : Constants.GET) + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }

    public static String setterName(String fieldName) {
        return Constants.SET + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }


    /**
     *   Create a plain-class's constructor method using lambda
     */
    @SuppressWarnings("unchecked")
    public static <T> Supplier<T> createConstructor(MethodHandles.Lookup lookup, Class<T> type) {
        try{
            MethodHandle cmh = lookup.findConstructor(type, MethodType.methodType(void.class));
            MethodType methodType = cmh.type();
            CallSite callSite = LambdaMetafactory.metafactory(lookup,
                    Constants.GET,
                    MethodType.methodType(Supplier.class),
                    methodType.erase(), cmh, methodType);
            return (Supplier<T>) callSite.getTarget().invokeExact();
        }catch (Throwable e) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Target class %s lacks a parameterless constructor".formatted(type.getName()), e);
        }
    }

    /**
     *   Create a record's constructor method using lambda
     */
    @SuppressWarnings("unchecked")
    public static <T> Function<Object[], T> createRecordConstructor(MethodHandles.Lookup lookup, Class<T> recordClass, List<Class<?>> parameterTypes) {
        try{
            MethodHandle cmh = lookup.findConstructor(recordClass, MethodType.methodType(void.class, parameterTypes)).asSpreader(Object[].class, parameterTypes.size());
            MethodType methodType = cmh.type();
            CallSite callSite = LambdaMetafactory.metafactory(lookup,
                    Constants.APPLY,
                    MethodType.methodType(Function.class, MethodHandle.class),
                    methodType.erase(),
                    MethodHandles.exactInvoker(methodType),
                    methodType);
            return (Function<Object[], T>) callSite.getTarget().invokeExact(cmh);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED, throwable);
        }
    }

    /**
     *   Create a public method invoker using lambda
     */
    @SuppressWarnings("unchecked")
    public static BiFunction<Object, Object[], Object> createMethodCaller(MethodHandles.Lookup lookup, Class<?> targetType, String methodName, Class<?> returnType, List<Class<?>> parameterTypes) {
        try{
            MethodHandle cmh = lookup.findVirtual(targetType, methodName, MethodType.methodType(returnType, parameterTypes)).asSpreader(Object[].class, parameterTypes.size());
            MethodType methodType = cmh.type();
            CallSite callSite = LambdaMetafactory.metafactory(lookup,
                    Constants.APPLY,
                    MethodType.methodType(BiFunction.class, MethodHandle.class),
                    methodType.erase(),
                    MethodHandles.exactInvoker(methodType),
                    methodType);
            return (BiFunction<Object, Object[], Object>) callSite.getTarget().invokeExact(cmh);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED, throwable);
        }
    }

    /**
     *   Create a getter method using lambda
     */
    @SuppressWarnings("unchecked")
    public static Function<Object, Object> createGetter(MethodHandles.Lookup lookup, MethodHandle mh, Class<?> fieldClass) {
        try{
            MethodType type = mh.type();
            if(fieldClass.isPrimitive()) {
                type = type.changeReturnType(ReflectUtil.getWrapperClass(fieldClass));
            }
            CallSite callSite = LambdaMetafactory.metafactory(lookup,
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
    public static BiConsumer<Object, Object> createSetter(MethodHandles.Lookup lookup, MethodHandle mh, Class<?> fieldClass) {
        try{
            MethodType type = mh.type();
            if(fieldClass.isPrimitive()) {
                type = type.changeParameterType(1, ReflectUtil.getWrapperClass(fieldClass));
            }
            CallSite callSite = LambdaMetafactory.metafactory(lookup,
                    Constants.ACCEPT,
                    MethodType.methodType(BiConsumer.class),
                    type.erase(), mh, type);
            return (BiConsumer<Object, Object>) callSite.getTarget().invokeExact();
        }catch (Throwable e) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED, e);
        }
    }
}
