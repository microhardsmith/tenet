package cn.zorcc.common.util;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.Native;
import cn.zorcc.common.network.Openssl;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  Helper class when need to reach C native methods
 */
public final class NativeUtil {
    /**
     *   Global NULL pointer
     */
    public static final MemorySegment NULL_POINTER = MemorySegment.ofAddress(0L, ValueLayout.ADDRESS.byteSize(), SegmentScope.global());
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final boolean LINUX = OS_NAME.contains("linux");
    private static final boolean WINDOWS = OS_NAME.contains("windows");
    private static final boolean MACOS = OS_NAME.contains("mac") && OS_NAME.contains("os");
    private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();
    private static final Linker linker = Linker.nativeLinker();
    /**
     *   Global dynamic library cache to avoid repeated loading
     */
    private static final Map<String, SymbolLookup> cache = new ConcurrentHashMap<>();
    private static final VarHandle byteHandle = MethodHandles.memorySegmentViewVarHandle(ValueLayout.JAVA_BYTE);
    private static final VarHandle shortHandle = MethodHandles.memorySegmentViewVarHandle(ValueLayout.JAVA_SHORT);
    private static final VarHandle intHandle = MethodHandles.memorySegmentViewVarHandle(ValueLayout.JAVA_INT);
    private static final VarHandle longHandle = MethodHandles.memorySegmentViewVarHandle(ValueLayout.JAVA_LONG);
    private static final MemorySegment stdout;
    private static final MemorySegment stderr;
    static {
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

    public static boolean isLinux() {
        return LINUX;
    }

    public static boolean isWindows() {
        return WINDOWS;
    }

    public static boolean isMacos() {
        return MACOS;
    }

    public static String libSuffix() {
        if(WINDOWS) {
            return ".dll";
        }else if(LINUX) {
            return ".so";
        }else if(MACOS) {
            return ".dylib";
        }else {
            throw new FrameworkException(ExceptionType.NATIVE, "Unrecognized operating system");
        }
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
     *  Load a native library by environment variable, if system library was not found in environment variables, will try to copy a duplicate from resource to the tmp folder
     *  for operating system to load. Note that this mechanism would be significantly slower than directly loading and it's not recommended
     */
    public static SymbolLookup loadLibrary(String identifier) {
        return cache.computeIfAbsent(identifier, i -> {
            String path = System.getProperty(i);
            if(path == null || path.isEmpty()) {
                String libSuffix = libSuffix();
                String resourcePath = switch (identifier) {
                    case Native.LIB -> "libtenet" + libSuffix;
                    case Openssl.CRYPTO_LIB -> "libcrypto" + libSuffix;
                    case Openssl.SSL_LIB -> "libssl" + libSuffix;
                    default -> throw new FrameworkException(ExceptionType.NATIVE, "Environment variable not found : %s".formatted(identifier));
                };
                path = FileUtil.toTmp(resourcePath);
            }
            return SymbolLookup.libraryLookup(path, SegmentScope.global());
        });
    }

    /**
     *  Load function from libc, such as strlen
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

    /**
     *  从MemorySegment中获取指定index的byte值
     */
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
        int size = (int) memorySegment.byteSize();
        byte[] bytes = new byte[size];
        for(int i = 0; i < size; i++) {
            byte b = getByte(memorySegment, i);
            if(b == Constants.NUT) {
                return new String(bytes, 0, i);
            }else {
                bytes[i] = b;
            }
        }
        throw new FrameworkException(ExceptionType.NATIVE, "Not a valid C style string");
    }
}
