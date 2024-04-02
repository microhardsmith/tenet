package cn.zorcc.common.bindings;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 *   System API which JVM could already provide us
 */
public final class SystemBinding {
    private static final MethodHandle mallocHandle;
    private static final MethodHandle reallocHandle;
    private static final MethodHandle freeHandle;
    private static final MethodHandle strlenHandle;


    static {
        mallocHandle = NativeUtil.nativeMethodHandle("malloc", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), Linker.Option.critical(false));
        reallocHandle = NativeUtil.nativeMethodHandle("realloc", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), Linker.Option.critical(false));
        freeHandle = NativeUtil.nativeMethodHandle("free", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS), Linker.Option.critical(false));
        strlenHandle = NativeUtil.nativeMethodHandle("strnlen", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), Linker.Option.critical(true));
    }

    private SystemBinding() {
        throw new UnsupportedOperationException();
    }

    public static MemorySegment malloc(long byteSize) {
        try {
            return (MemorySegment) mallocHandle.invokeExact(byteSize);
        } catch (Throwable e) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED);
        }
    }

    public static MemorySegment realloc(MemorySegment ptr, long newSize) {
        try{
            return (MemorySegment) reallocHandle.invokeExact(ptr, newSize);
        } catch (Throwable e) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED);
        }
    }

    public static void free(MemorySegment ptr) {
        try{
            freeHandle.invokeExact(ptr);
        } catch (Throwable e) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED);
        }
    }

    public static long strlen(MemorySegment ptr, long available) {
        try{
            return (long) strlenHandle.invokeExact(ptr, available);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED);
        }
    }

}
