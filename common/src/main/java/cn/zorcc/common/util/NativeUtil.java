package cn.zorcc.common.util;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.enums.OsType;
import cn.zorcc.common.exception.FrameworkException;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 *  Native Helper class for accessing native memory and methods
 */
public final class NativeUtil {
    /**
     *   Global NULL pointer, don't use it if the application would modify the actual address of this pointer
     */
    public static final MemorySegment NULL_POINTER = MemorySegment.ofAddress(Constants.ZERO).reinterpret(ValueLayout.ADDRESS.byteSize(), Arena.global(), null);
    /**
     *   Current operating system name
     */
    private static final String osName = System.getProperty("os.name").toLowerCase();
    /**
     *   Current dynamic library path that tenet application will look up for
     */
    private static final String libPath = System.getProperty(Constants.TENET_LIBRARY_PATH);
    private static final OsType osType;
    private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();
    private static final Linker linker = Linker.nativeLinker();
    /**
     *   Global dynamic library cache to avoid repeated loading
     */
    private static final Map<String, SymbolLookup> libraryCache = new ConcurrentHashMap<>();
    /**
     *   Global native method cache to avoid repeated capturing
     */
    private static final Map<String, MethodHandle> nativeMethodCache = new ConcurrentHashMap<>();
    private static final Arena globalArena = Arena.global();
    private static final Arena autoArena = Arena.ofAuto();
    private static final VarHandle byteHandle = MethodHandles.memorySegmentViewVarHandle(ValueLayout.JAVA_BYTE);
    private static final VarHandle shortHandle = MethodHandles.memorySegmentViewVarHandle(ValueLayout.JAVA_SHORT_UNALIGNED);
    private static final VarHandle intHandle = MethodHandles.memorySegmentViewVarHandle(ValueLayout.JAVA_INT_UNALIGNED);
    private static final VarHandle longHandle = MethodHandles.memorySegmentViewVarHandle(ValueLayout.JAVA_LONG_UNALIGNED);
    private static final MemorySegment stdout;
    private static final MemorySegment stderr;

    static {
        osType = detectOsType();
        if(libPath == null || libPath.isEmpty()) {
            throw new FrameworkException(ExceptionType.NATIVE, STR."\{Constants.TENET_LIBRARY_PATH} not found in environment variables");
        }
        try{
            SymbolLookup symbolLookup = NativeUtil.loadLibrary(Constants.TENET);
            MethodHandle stdoutHandle = NativeUtil.methodHandle(symbolLookup, "get_stdout", FunctionDescriptor.of(ValueLayout.ADDRESS));
            stdout = (MemorySegment) stdoutHandle.invokeExact();
            MethodHandle stderrHandle = NativeUtil.methodHandle(symbolLookup, "get_stderr", FunctionDescriptor.of(ValueLayout.ADDRESS));
            stderr = (MemorySegment) stderrHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    private static OsType detectOsType() {
        if(osName.contains("windows")) {
            return OsType.Windows;
        }else if(osName.contains("linux")) {
            return OsType.Linux;
        }else if(osName.contains("mac") && osName.contains("os")) {
            return OsType.MacOS;
        }else {
            return OsType.Unknown;
        }
    }

    private NativeUtil() {
        throw new UnsupportedOperationException();
    }

    private static String getDynamicLibraryName(String identifier) {
        return switch (osType) {
            case Windows -> STR."lib\{identifier}.dll";
            case Linux -> STR."lib\{identifier}.so";
            case MacOS -> STR."lib\{identifier}.dylib";
            default -> throw new FrameworkException(ExceptionType.NATIVE, "Unrecognized operating system");
        };
    }

    public static Arena globalArena() {
        return globalArena;
    }

    public static Arena autoArena() {
        return autoArena;
    }

    public static MemorySegment stdout() {
        return stdout;
    }

    public static MemorySegment stderr() {
        return stderr;
    }

    public static String osName() {
        return osName;
    }

    public static OsType ostype() {
        return osType;
    }

    /**
     *   Return current CPU cores
     *   Note that usually a physical CPU core could usually carry two threads at the same time, the return value is actually the logical core numbers.
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
     *  Load a native library by environment variable, return null if system library was not found in environment variables
     */
    public static SymbolLookup loadLibrary(String identifier) {
        return libraryCache.computeIfAbsent(identifier, i -> SymbolLookup.libraryLookup(libPath + Constants.SEPARATOR + getDynamicLibraryName(i), Arena.global()));
    }

    public static MethodHandle nativeMethodHandle(String methodName, FunctionDescriptor functionDescriptor) {
        return nativeMethodHandle(methodName, functionDescriptor, false);
    }

    /**
     *  Load function from system library, such as strlen()
     */
    public static MethodHandle nativeMethodHandle(String methodName, FunctionDescriptor functionDescriptor, boolean isTrivial) {
        return nativeMethodCache.computeIfAbsent(methodName, k -> {
            MemorySegment method = linker.defaultLookup().find(k).orElseThrow(() -> new FrameworkException(ExceptionType.NATIVE, STR."Unable to locate [\{methodName}] native method"));
            return isTrivial ? linker.downcallHandle(method, functionDescriptor, Linker.Option.isTrivial()): linker.downcallHandle(method, functionDescriptor);
        });
    }

    public static boolean checkNullPointer(MemorySegment memorySegment) {
        return memorySegment == null || memorySegment.address() == Constants.ZERO;
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
        return getStr(memorySegment, Constants.ZERO);
    }

    public static String getStr(MemorySegment ptr, int maxLength) {
        if(maxLength > 0) {
            byte[] bytes = new byte[maxLength];
            for(int i = 0; i < maxLength; i++) {
                byte b = getByte(ptr, i);
                if(b == Constants.NUT) {
                    return new String(bytes, Constants.ZERO, i, StandardCharsets.UTF_8);
                }else {
                    bytes[i] = b;
                }
            }
        }else {
            for(int i = 0; i < Integer.MAX_VALUE; i++) {
                byte b = getByte(ptr, i);
                if(b == Constants.NUT) {
                    return new String(ptr.asSlice(Constants.ZERO, i).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8);
                }
            }
        }
        throw new FrameworkException(ExceptionType.NATIVE, "Not a valid C style string");
    }

    // TODO remove
    public static MemorySegment accessPtr(MemorySegment pp, Arena arena, Consumer<MemorySegment> cleanup) {
        return pp.get(ValueLayout.ADDRESS, Constants.ZERO).reinterpret(ValueLayout.ADDRESS.byteSize(), arena, cleanup);
    }
}
