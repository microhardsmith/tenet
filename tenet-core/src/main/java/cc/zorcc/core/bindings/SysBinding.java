package cc.zorcc.core.bindings;

import cc.zorcc.core.Constants;
import cc.zorcc.core.Dyn;
import cc.zorcc.core.ExceptionType;
import cc.zorcc.core.FrameworkException;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 *   System API which JVM could already provide us without loading another dynamic library
 */
public final class SysBinding {
    private static final MethodHandle mallocHandle;
    private static final MethodHandle reallocHandle;
    private static final MethodHandle freeHandle;
    private static final MethodHandle strlenHandle;
    private static final MethodHandle memcpyHandle;

    static {
        mallocHandle = Dyn.vmMh("malloc", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), Linker.Option.critical(false));
        reallocHandle = Dyn.vmMh("realloc", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), Linker.Option.critical(false));
        freeHandle = Dyn.vmMh("free", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS), Linker.Option.critical(false));
        strlenHandle = Dyn.vmMh("strnlen", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), Linker.Option.critical(true));
        memcpyHandle = Dyn.vmMh("memcpy", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), Linker.Option.critical(true));
    }

    /**
     *   SysBinding shouldn't be initialized
     */
    private SysBinding() {
        throw new UnsupportedOperationException();
    }

    public static MemorySegment malloc(long byteSize) {
        try {
            return (MemorySegment) mallocHandle.invokeExact(byteSize);
        } catch (Throwable e) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static MemorySegment realloc(MemorySegment ptr, long newSize) {
        try{
            return (MemorySegment) reallocHandle.invokeExact(ptr, newSize);
        } catch (Throwable e) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static void free(MemorySegment ptr) {
        try{
            freeHandle.invokeExact(ptr);
        } catch (Throwable e) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static long strlen(MemorySegment ptr, long available) {
        try{
            return (long) strlenHandle.invokeExact(ptr, available);
        }catch (Throwable e) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static MemorySegment memcpy(MemorySegment target, MemorySegment source, long count) {
        try{
            return (MemorySegment) memcpyHandle.invokeExact(target, source, count);
        }catch (Throwable e) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }
}
