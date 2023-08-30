package cn.zorcc.common.util;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.enums.OsType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.Native;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  Native Helper class for accessing native memory and methods
 */
public final class NativeUtil {
    /**
     *   Global NULL pointer, don't use it if the application would modify the actual address of this pointer
     */
    public static final MemorySegment NULL_POINTER = MemorySegment.ofAddress(0L).reinterpret(ValueLayout.ADDRESS.byteSize(), Arena.global(), null);
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final OsType ostype;
    private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();
    private static final Linker linker = Linker.nativeLinker();
    /**
     *   Global dynamic library cache to avoid repeated loading
     */
    private static final Map<String, SymbolLookup> cache = new ConcurrentHashMap<>();
    private static final VarHandle byteHandle = MethodHandles.memorySegmentViewVarHandle(ValueLayout.JAVA_BYTE);
    private static final VarHandle shortHandle = MethodHandles.memorySegmentViewVarHandle(ValueLayout.JAVA_SHORT_UNALIGNED);
    private static final VarHandle intHandle = MethodHandles.memorySegmentViewVarHandle(ValueLayout.JAVA_INT_UNALIGNED);
    private static final VarHandle longHandle = MethodHandles.memorySegmentViewVarHandle(ValueLayout.JAVA_LONG_UNALIGNED);
    private static final MemorySegment stdout;
    private static final MemorySegment stderr;
    static {
        if(OS_NAME.contains("windows")) {
            ostype = OsType.Windows;
        }else if(OS_NAME.contains("linux")) {
            ostype = OsType.Linux;
        }else if(OS_NAME.contains("mac") && OS_NAME.contains("os")) {
            ostype = OsType.MacOS;
        }else {
            ostype = OsType.Unknown;
        }
        try{
            SymbolLookup symbolLookup = NativeUtil.loadLibrary(Native.LIB);
            MethodHandle stdoutHandle = NativeUtil.methodHandle(symbolLookup, "g_stdout", FunctionDescriptor.of(ValueLayout.ADDRESS));
            stdout = (MemorySegment) stdoutHandle.invokeExact();
            MethodHandle stderrHandle = NativeUtil.methodHandle(symbolLookup, "g_stderr", FunctionDescriptor.of(ValueLayout.ADDRESS));
            stderr = (MemorySegment) stderrHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Unable to load stdout and stderr", throwable);
        }
    }

    private NativeUtil() {
        throw new UnsupportedOperationException();
    }

    public static MemorySegment stdout() {
        return stdout;
    }

    public static MemorySegment stderr() {
        return stderr;
    }

    public static String osName() {
        return OS_NAME;
    }

    public static OsType ostype() {
        return ostype;
    }

    /**
     *   Return current CPU cores
     *   Note that usually a physical core could carry two threads at the same time, the return value is actually the logical core numbers.
     */
    public static int getCpuCores() {
        return CPU_CORES;
    }

    /**
     *  Load function from dynamic library
     */
    public static MethodHandle methodHandle(SymbolLookup lookup, String methodName, FunctionDescriptor functionDescriptor) {
        MemorySegment methodPointer = lookup.find(methodName)
                .orElseThrow(() -> new FrameworkException(ExceptionType.NATIVE, "Unable to load target native method : %s".formatted(methodName)));
        return linker.downcallHandle(methodPointer, functionDescriptor);
    }

    /**
     *  Load a native library by environment variable, if system library was not found in environment variables, will throw a exception
     */
    public static SymbolLookup loadLibrary(String identifier) {
        return cache.computeIfAbsent(identifier, i -> {
            String path = System.getProperty(i);
            if(path == null || path.isEmpty()) {
                throw new FrameworkException(ExceptionType.NATIVE, "Environment variable not found : %s".formatted(i));
            }
            return SymbolLookup.libraryLookup(path, Arena.global());
        });
    }

    /**
     *  Load function from system library, such as strlen
     */
    public static MethodHandle nativeMethodHandle(String methodName, FunctionDescriptor functionDescriptor) {
        return linker.downcallHandle(linker.defaultLookup()
                .find(methodName)
                .orElseThrow(() -> new FrameworkException(ExceptionType.NATIVE, "Unable to locate [%s] native method".formatted(methodName))),
                functionDescriptor);
    }

    public static boolean checkNullPointer(MemorySegment memorySegment) {
        return memorySegment == null || memorySegment.address() == 0L;
    }

    public static byte getByte(MemorySegment memorySegment, long index) {
        return (byte) byteHandle.get(memorySegment, index);
    }

    public static void setByte(MemorySegment memorySegment, long index, byte value) {
        byteHandle.set(memorySegment, index, value);
    }

    public static short getShort(MemorySegment memorySegment, long index) {
        return (short) shortHandle.get(memorySegment, index);
    }

    public static void setShort(MemorySegment memorySegment, long index, short value) {
        shortHandle.set(memorySegment, index, value);
    }

    public static int getInt(MemorySegment memorySegment, long index) {
        return (int) intHandle.get(memorySegment, index);
    }

    public static void setInt(MemorySegment memorySegment, long index, int value) {
        intHandle.set(memorySegment, index, value);
    }

    public static long getLong(MemorySegment memorySegment, long index) {
        return (long) longHandle.get(memorySegment, index);
    }

    public static void setLong(MemorySegment memorySegment, long index, long value) {
        longHandle.set(memorySegment, index, value);
    }

    /**
     *   Using brute-force search for target bytes in a memorySegment
     *   This method could be optimized for better efficiency using other algorithms like BM or KMP, however when bytes.length is small and unrepeated, BF is simple and good enough
     *   Usually this method is used to find a target separator in a sequence
     */
    public static boolean matches(MemorySegment m, long offset, byte[] bytes) {
        for(int index = 0; index < bytes.length; index++) {
            if (getByte(m, offset + index) != bytes[index]) {
                return false;
            }
        }
        return true;
    }

    public static MemorySegment allocateStr(Arena arena, String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        MemorySegment memorySegment = arena.allocateArray(ValueLayout.JAVA_BYTE, bytes.length + 1);
        for(int i = 0; i < bytes.length; i++) {
            setByte(memorySegment, i, bytes[i]);
        }
        setByte(memorySegment, bytes.length, Constants.NUT);
        return memorySegment;
    }

    public static MemorySegment allocateStr(Arena arena, String str, int len) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        if(len < bytes.length + 1) {
            throw new FrameworkException(ExceptionType.NATIVE, "String out of range");
        }
        MemorySegment memorySegment = arena.allocateArray(ValueLayout.JAVA_BYTE, len);
        for(int i = 0; i < bytes.length; i++) {
            setByte(memorySegment, i, bytes[i]);
        }
        setByte(memorySegment, bytes.length, Constants.NUT);
        return memorySegment;
    }

    public static String getStr(MemorySegment memorySegment) {
        return getStr(memorySegment, 0);
    }

    public static String getStr(MemorySegment ptr, int maxLength) {
        if(maxLength > 0) {
            byte[] bytes = new byte[maxLength];
            for(int i = 0; i < maxLength; i++) {
                byte b = getByte(ptr, i);
                if(b == Constants.NUT) {
                    return new String(bytes, 0, i, StandardCharsets.UTF_8);
                }else {
                    bytes[i] = b;
                }
            }
        }else {
            for(int i = 0; i < Integer.MAX_VALUE; i++) {
                byte b = getByte(ptr, i);
                if(b == Constants.NUT) {
                    byte[] bytes = new byte[i];
                    MemorySegment.copy(ptr, ValueLayout.JAVA_BYTE, 0, bytes, 0, i);
                    return new String(bytes, StandardCharsets.UTF_8);
                }
            }
        }
        throw new FrameworkException(ExceptionType.NATIVE, "Not a valid C style string");
    }

    public static MemorySegment accessPtr(MemorySegment pp, Arena arena) {
        return pp.get(ValueLayout.ADDRESS, 0L).reinterpret(ValueLayout.ADDRESS.byteSize(), arena, null);
    }
}
