package cn.zorcc.common.util;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.OsType;
import cn.zorcc.common.exception.FrameworkException;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  Native Helper class for accessing native memory and methods
 */
public final class NativeUtil {
    public static final Arena globalArena = Arena.global();
    public static final Arena autoArena = Arena.ofAuto();
    /**
     *   Current operating system name
     */
    private static final String osName = System.getProperty("os.name").toLowerCase();
    private static final boolean runningFromJar = runningFromJar();
    /**
     *   Current dynamic library path that tenet application will look up for
     */
    private static final String libPath = System.getProperty(Constants.TENET_LIBRARY_PATH);
    private static final OsType osType = detectOsType();
    private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();
    private static final Linker linker = Linker.nativeLinker();
    /**
     *   Global dynamic library cache to avoid repeated loading
     */
    private static final Map<String, SymbolLookup> libraryCache = new ConcurrentHashMap<>();
    private static final MethodHandle mallocHandle;
    private static final MethodHandle reallocHandle;
    private static final MethodHandle freeHandle;

    static {
        mallocHandle = NativeUtil.nativeMethodHandle("malloc", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), Linker.Option.critical(false));
        reallocHandle = NativeUtil.nativeMethodHandle("realloc", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        freeHandle = NativeUtil.nativeMethodHandle("free", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS), Linker.Option.critical(false));
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

    /**
     *   Detect if current program is running from a jar file
     */
    private static boolean runningFromJar() {
        String className = NativeUtil.class.getName().replace('.', '/');
        String classJar = Objects.requireNonNull(NativeUtil.class.getResource(STR."/\{className}.class")).toString();
        return classJar.startsWith("jar:");
    }

    public static boolean isRunningFromJar() {
        return runningFromJar;
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

    public static MemorySegment toNativeSegment(MemorySegment memorySegment) {
        return toNativeSegment(memorySegment, autoArena);
    }

    public static MemorySegment toNativeSegment(MemorySegment memorySegment, SegmentAllocator allocator) {
        if(memorySegment.isNative()) {
            return memorySegment;
        }else {
            long byteSize = memorySegment.byteSize();
            MemorySegment nativeSegment = allocator.allocate(ValueLayout.JAVA_BYTE, byteSize);
            MemorySegment.copy(memorySegment, 0L, nativeSegment, 0L, byteSize);
            return nativeSegment;
        }
    }

    private static String getDynamicLibraryName(String identifier) {
        return switch (osType) {
            case Windows -> STR."lib\{identifier}.dll";
            case Linux -> STR."lib\{identifier}.so";
            case MacOS -> STR."lib\{identifier}.dylib";
            default -> throw new FrameworkException(ExceptionType.NATIVE, "Unrecognized operating system");
        };
    }
    public static OsType ostype() {
        return osType;
    }

    /**
     *   Return current CPU cores count
     *   Note that usually a physical CPU core could usually carry two threads at the same time, the return value is actually the logical core numbers.
     */
    public static int getCpuCores() {
        return CPU_CORES;
    }

    /**
     *  Load function from dynamic library
     */
    public static MethodHandle methodHandle(SymbolLookup lookup, String methodName, FunctionDescriptor functionDescriptor, Linker.Option... options) {
        MemorySegment methodPointer = lookup.find(methodName)
                .orElseThrow(() -> new FrameworkException(ExceptionType.NATIVE, STR."Unable to load target native method : \{methodName}"));
        return linker.downcallHandle(methodPointer, functionDescriptor, options);
    }

    /**
     *   Due to macro issue, there could be multiple implementations from the dynamic library, then this function could be used
     */
    public static MethodHandle methodHandle(SymbolLookup lookup, List<String> methodNames, FunctionDescriptor functionDescriptor, Linker.Option... options) {
        for (String methodName : methodNames) {
            Optional<MemorySegment> methodPointer = lookup.find(methodName);
            if (methodPointer.isPresent()) {
                return linker.downcallHandle(methodPointer.get(), functionDescriptor, options);
            }
        }
        throw new FrameworkException(ExceptionType.NATIVE, STR."Unable to load target native method : \{methodNames}");
    }

    /**
     *  Load a native library by environment variable, return null if system library was not found in environment variables
     */
    public static SymbolLookup loadLibrary(String identifier) {
        if(libPath == null) {
            throw new FrameworkException(ExceptionType.NATIVE, "Global libPath not found");
        }
        return libraryCache.computeIfAbsent(identifier, i -> SymbolLookup.libraryLookup(libPath + Constants.SEPARATOR + getDynamicLibraryName(i), globalArena));
    }

    /**
     *  Load function from system library, such as strlen()
     *  Note that for methods from system library, there are no cache, every time a new MethodHandle would be created, and it's totally fine by JVM
     */
    public static MethodHandle nativeMethodHandle(String methodName, FunctionDescriptor functionDescriptor, Linker.Option... options) {
        MemorySegment method = linker.defaultLookup().find(methodName).orElseThrow(() -> new FrameworkException(ExceptionType.NATIVE, STR."Unable to locate [\{methodName}] native method"));
        return linker.downcallHandle(method, functionDescriptor, options);
    }

    public static boolean checkNullPointer(MemorySegment memorySegment) {
        return memorySegment == null || memorySegment.address() == 0;
    }

    public static byte toAsciiByte(int i) {
        return (byte) (i + 48);
    }

    /**
     *   Using brute-force search for target bytes in a memorySegment
     *   This method could be optimized for better efficiency using other algorithms like BM or KMP, however when the length of bytes are small and unrepeated, BF is simple and good enough
     *   Usually this method is used to find a target separator in a sequence
     */
    public static boolean matches(MemorySegment m, long offset, byte... bytes) {
        for(int index = 0; index < bytes.length; index++) {
            if (m.get(ValueLayout.JAVA_BYTE, offset + index) != bytes[index]) {
                return false;
            }
        }
        return true;
    }
}
