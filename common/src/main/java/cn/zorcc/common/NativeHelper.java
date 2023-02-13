package cn.zorcc.common;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.net.URL;

/**
 *  Helper class when need to reach C native methods
 */
public class NativeHelper {
    private static final Linker linker = Linker.nativeLinker();

    /**
     * 从C动态库中获取指定函数的MethodHandle
     * @param lookup 函数库地址
     * @param methodName 函数名称
     * @param functionDescriptor 函数参数描述
     * @return 对应MethodHandle
     */
    public static MethodHandle methodHandle(SymbolLookup lookup, String methodName, FunctionDescriptor functionDescriptor) {
        MemorySegment methodPointer = lookup.find(methodName)
                .orElseThrow(() -> new FrameworkException(ExceptionType.NATIVE, "Unable to load target native method : %s", methodName));
        return linker.downcallHandle(methodPointer, functionDescriptor);
    }

    /**
     * 从resource文件夹路径下加载动态链接库
     * @param resourcePath 动态链接库路径
     */
    public static SymbolLookup loadLibraryFromResource(String resourcePath) {
        URL resource = NativeHelper.class.getResource(resourcePath);
        if(resource == null) {
            throw new FrameworkException(ExceptionType.NATIVE, "ResourcePath is not valid");
        }else {
            String path = resource.getPath();
            if(path.startsWith("/")) {
                path = path.substring(Constants.ONE);
            }
            return SymbolLookup.libraryLookup(path, SegmentScope.global());
        }
    }

    /**
     *  检查是否为空指针
     */
    public static boolean checkNullPointer(MemorySegment memorySegment) {
        return memorySegment == null || memorySegment.address() == 0L;
    }

    /**
     * 分配基本类型的对象,返回其指针
     * @param arena 内存作用域
     * @param memoryLayout 内存布局
     * @param value 对象值
     * @return 指向该对象的指针
     */
    public static MemorySegment allocate(Arena arena, MemoryLayout memoryLayout, Object value) {
        if(memoryLayout == null) {
            throw new FrameworkException(ExceptionType.NET, "Empty memoryLayout");
        }
        VarHandle varHandle = memoryLayout.varHandle();
        MemorySegment memorySegment = arena.allocate(memoryLayout);
        if(memoryLayout.equals(ValueLayout.JAVA_INT) && value instanceof Integer i) {
            varHandle.set(memorySegment, i);
        } else if(memoryLayout.equals(ValueLayout.JAVA_BYTE) && value instanceof Byte b) {
            varHandle.set(memorySegment, b);
        }else if(memoryLayout.equals(ValueLayout.JAVA_CHAR) && value instanceof Character c) {
            varHandle.set(memorySegment, c);
        }else if (memoryLayout.equals(ValueLayout.JAVA_BOOLEAN) && value instanceof Boolean b) {
            varHandle.set(memorySegment, b);
        } else if(memoryLayout.equals(ValueLayout.JAVA_LONG) && value instanceof Long l) {
            varHandle.set(memorySegment, l);
        } else if(memoryLayout.equals(ValueLayout.JAVA_FLOAT) && value instanceof Float f) {
            varHandle.set(memorySegment, f);
        }else if(memoryLayout.equals(ValueLayout.JAVA_DOUBLE) && value instanceof Double d) {
            varHandle.set(memorySegment, d);
        }else if(memoryLayout.equals(ValueLayout.JAVA_SHORT) && value instanceof Short s) {
            varHandle.set(memorySegment, s);
        }
        return memorySegment;
    }
}
