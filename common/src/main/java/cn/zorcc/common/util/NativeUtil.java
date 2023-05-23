package cn.zorcc.common.util;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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
     *   动态链接库缓存,避免重复加载
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
            SymbolLookup symbolLookup = NativeUtil.loadLibraryFromResource(NativeUtil.netLib());
            MethodHandle stdoutHandle = NativeUtil.methodHandle(symbolLookup, "g_stdout", FunctionDescriptor.of(ValueLayout.ADDRESS));
            stdout = (MemorySegment) stdoutHandle.invokeExact();
            MethodHandle stderrHandle = NativeUtil.methodHandle(symbolLookup, "g_stderr", FunctionDescriptor.of(ValueLayout.ADDRESS));
            stderr = (MemorySegment) stdoutHandle.invokeExact();
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

    public static boolean isLinux() {
        return LINUX;
    }

    public static boolean isWindows() {
        return WINDOWS;
    }

    public static boolean isMacos() {
        return MACOS;
    }

    /**
     *   获取当前系统CPU核心数
     */
    public static int getCpuCores() {
        return CPU_CORES;
    }

    /**
     *   获取当前操作系统对应的net动态链接库路径
     */
    public static String netLib() {
        if(LINUX) {
            return "/lib/lib_linux.so";
        }else if(WINDOWS) {
            return "/lib/lib_win.dll";
        }else if(MACOS) {
            return "/lib/lib_macos.dylib";
        }else {
            throw new FrameworkException(ExceptionType.NATIVE, "Unsupported operating system : %s".formatted(OS_NAME));
        }
    }

    /**
     *   获取当前操作系统对应的crypto动态链接库路径
     */
    public static String cryptoLib() {
        if(LINUX) {
            return "/ssl/libcrypto.so";
        }else if(WINDOWS) {
            return "/ssl/libcrypto-3-x64.dll";
        }else if(MACOS) {
            return "/ssl/libcrypto.3.dylib";
        }else {
            throw new FrameworkException(ExceptionType.NATIVE, "Unsupported operating system : %s".formatted(OS_NAME));
        }
    }

    /**
     *   获取当前操作系统对应的ssl动态链接库路径
     */
    public static String sslLib() {
        if(LINUX) {
            return "/ssl/libssl.so";
        }else if(WINDOWS) {
            return "/ssl/libssl-3-x64.dll";
        }else if(MACOS) {
            return "/ssl/libssl.3.dylib";
        }else {
            throw new FrameworkException(ExceptionType.NATIVE, "Unsupported operating system : %s".formatted(OS_NAME));
        }
    }

    /**
     * 从C动态库中获取指定函数的MethodHandle
     * @param lookup 函数库地址
     * @param methodName 函数名称
     * @param functionDescriptor 函数参数描述
     * @return 对应MethodHandle
     */
    public static MethodHandle methodHandle(SymbolLookup lookup, String methodName, FunctionDescriptor functionDescriptor) {
        MemorySegment methodPointer = lookup.find(methodName)
                .orElseThrow(() -> new FrameworkException(ExceptionType.NATIVE, "Unable to load target native method : %s".formatted(methodName)));
        return linker.downcallHandle(methodPointer, functionDescriptor);
    }

    /**
     *  从当前的common模块下resource中加载动态链接库
     */
    public static SymbolLookup loadLibraryFromResource(String resourcePath) {
        return loadLibraryFromResource(resourcePath, null);
    }

    /**
     * 从指定Class下的resource文件夹路径加载动态链接库 必须在platform thread下进行操作,因为拷贝文件可能阻塞
     * @param resourcePath 动态链接库路径
     * @param clazz 需要加载资源的检索类,如果指定为null则从common项目下加载
     * @return 指定库对应SymbolLookup
     */
    public static SymbolLookup loadLibraryFromResource(String resourcePath, Class<?> clazz) {
        return cache.computeIfAbsent(resourcePath, k -> {
            Path path = FileUtil.toTmp(k, Constants.TMP_LIB, clazz);
            return SymbolLookup.libraryLookup(path, SegmentScope.global());
        });
    }

    /**
     *  从操作系统已加载的动态链接库中获取函数索引,例如strlen
     */
    public static MethodHandle getNativeMethodHandle(String methodName, FunctionDescriptor functionDescriptor) {
        return linker.downcallHandle(linker.defaultLookup()
                .find(methodName)
                .orElseThrow(() -> new FrameworkException(ExceptionType.NATIVE, "Unable to locate [%s] native method".formatted(methodName))),
                functionDescriptor);
    }

    /**
     *  检查MemorySegment是否为空指针
     */
    public static boolean checkNullPointer(MemorySegment memorySegment) {
        return memorySegment == null || memorySegment.address() == 0L;
    }

    /**
     *  从MemorySegment中获取指定index的byte值
     */
    public static byte getByte(MemorySegment memorySegment, long index) {
        return (byte) byteHandle.get(memorySegment, index);
    }

    /**
     *  向MemorySegment中设定byte值
     */
    public static void setByte(MemorySegment memorySegment, long index, byte value) {
        byteHandle.set(memorySegment, index, value);
    }

    /**
     *  从MemorySegment中获取指定index的short值
     */
    public static short getShort(MemorySegment memorySegment, long index) {
        return (short) shortHandle.get(memorySegment, index);
    }

    /**
     *  向MemorySegment中设定short值
     */
    public static void setShort(MemorySegment memorySegment, long index, short value) {
        shortHandle.set(memorySegment, index, value);
    }

    /**
     *  从MemorySegment中获取指定index的int值
     */
    public static int getInt(MemorySegment memorySegment, long index) {
        return (int) intHandle.get(memorySegment, index);
    }

    /**
     *  向MemorySegment中设定int值
     */
    public static void setInt(MemorySegment memorySegment, long index, int value) {
        intHandle.set(memorySegment, index, value);
    }

    /**
     *  从MemorySegment中获取指定index的long值
     */
    public static long getLong(MemorySegment memorySegment, long index) {
        return (long) longHandle.get(memorySegment, index);
    }

    /**
     *  向MemorySegment中设定long值
     */
    public static void setLong(MemorySegment memorySegment, long index, long value) {
        longHandle.set(memorySegment, index, value);
    }

    /**
     *   分配C风格的字符串
     */
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

    /**
     *   获取C风格的字符串
     */
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
