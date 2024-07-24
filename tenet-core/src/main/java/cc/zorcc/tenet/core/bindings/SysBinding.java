package cc.zorcc.tenet.core.bindings;

import cc.zorcc.tenet.core.Constants;
import cc.zorcc.tenet.core.Dyn;
import cc.zorcc.tenet.core.ExceptionType;
import cc.zorcc.tenet.core.TenetException;

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
    private static final MethodHandle setvbufHandle;
    private static final MethodHandle fopenHandle;
    private static final MethodHandle fwriteHandle;
    private static final MethodHandle fflushHandle;
    private static final MethodHandle fcloseHandle;
    private static final MethodHandle ferrorHandle;
    private static final MethodHandle clearerrHandle;

    static {
        mallocHandle = Dyn.vmMh("malloc", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), Linker.Option.critical(false));
        reallocHandle = Dyn.vmMh("realloc", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), Linker.Option.critical(false));
        freeHandle = Dyn.vmMh("free", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS), Linker.Option.critical(false));
        strlenHandle = Dyn.vmMh("strnlen", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), Linker.Option.critical(true));
        memcpyHandle = Dyn.vmMh("memcpy", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), Linker.Option.critical(true));
        setvbufHandle = Dyn.vmMh("setvbuf", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));
        fopenHandle = Dyn.vmMh("fopen", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        fwriteHandle = Dyn.vmMh("fwrite", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
        fflushHandle = Dyn.vmMh("fflush", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        fcloseHandle = Dyn.vmMh("fclose", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        ferrorHandle = Dyn.vmMh("ferror", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        clearerrHandle = Dyn.vmMh("clearerr", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
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
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static MemorySegment realloc(MemorySegment ptr, long newSize) {
        try{
            return (MemorySegment) reallocHandle.invokeExact(ptr, newSize);
        } catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static void free(MemorySegment ptr) {
        try{
            freeHandle.invokeExact(ptr);
        } catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static long strlen(MemorySegment ptr, long available) {
        try{
            return (long) strlenHandle.invokeExact(ptr, available);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static MemorySegment memcpy(MemorySegment target, MemorySegment source, long count) {
        try{
            return (MemorySegment) memcpyHandle.invokeExact(target, source, count);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static int setvbuf(MemorySegment stream, MemorySegment buffer, int mode, long size) {
        try{
            return (int) setvbufHandle.invokeExact(stream, buffer, mode, size);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static MemorySegment fopen(MemorySegment path, MemorySegment mode) {
        try{
            return (MemorySegment) fopenHandle.invokeExact(path, mode);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static long fwrite(MemorySegment buffer, MemorySegment stream) {
        try{
            return (long) fwriteHandle.invokeExact(buffer, ValueLayout.JAVA_BYTE.byteSize(), buffer.byteSize(), stream);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static int fflush(MemorySegment stream) {
        try{
            return (int) fflushHandle.invokeExact(stream);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static int fclose(MemorySegment stream) {
        try{
            return (int) fcloseHandle.invokeExact(stream);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }
}
