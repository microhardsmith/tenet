package cn.zorcc.common.util;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.OsType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.structure.Allocator;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private static final VarHandle BYTE_HANDLE = ValueLayout.JAVA_BYTE.varHandle().withInvokeExactBehavior();
    private static final VarHandle SHORT_HANDLE = ValueLayout.JAVA_SHORT_UNALIGNED.varHandle().withInvokeExactBehavior();
    private static final VarHandle INT_HANDLE = ValueLayout.JAVA_INT_UNALIGNED.varHandle().withInvokeExactBehavior();
    private static final VarHandle LONG_HANDLE = ValueLayout.JAVA_LONG_UNALIGNED.varHandle().withInvokeExactBehavior();
    private static final VarHandle FLOAT_HANDLE = ValueLayout.JAVA_FLOAT_UNALIGNED.varHandle().withInvokeExactBehavior();
    private static final VarHandle DOUBLE_HANDLE = ValueLayout.JAVA_DOUBLE_UNALIGNED.varHandle().withInvokeExactBehavior();
    private static final VarHandle ADDRESS_HANDLE = ValueLayout.ADDRESS_UNALIGNED.varHandle().withInvokeExactBehavior();

    /**
     *   SIMD related
     */
    private static final boolean usingSIMD = shouldEnableSIMD();

    public static boolean isUsingSIMD() {
        return usingSIMD;
    }

    /**
     *   Detect if current program could use SIMD instruction TODO currently we would just determine from the system property
     */
    private static boolean shouldEnableSIMD() {
        return switch (System.getProperty("usingSIMD")) {
            case null -> false;
            case "true" -> true;
            case "false" -> false;
            default -> throw new FrameworkException(ExceptionType.CONTEXT, "Unrecognized enableSIMD property");
        };
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

    /**
     *  Loading a dynamic library from the TENET_LIBRARY_PATH
     *  Note that link would probably not work well, it's recommended to just make a copy into the folder
     *  Due to historical reasons, dynamic libraries in Windows are not named with the "lib" prefix convention, but Linux and macOS usually did, sometimes the name of dynamic library matters
     *  So in general, you will need to specify the full file name as identifier to load it, no regarding which operating system you are using.
     */
    private static String getDynamicLibraryPath(String identifier) {
        String fileName = switch (osType) {
            case Windows -> STR."\{identifier}.dll";
            case Linux -> STR."\{identifier}.so";
            case MacOS -> STR."\{identifier}.dylib";
            default -> throw new FrameworkException(ExceptionType.NATIVE, "Unrecognized operating system");
        };
        String defaultPath = libPath + Constants.SEPARATOR + fileName;
        if(Files.exists(Path.of(defaultPath))) {
            return defaultPath;
        }
        // Naming convention for Windows/Linux users
        if(fileName.startsWith(Constants.LIB)) {
            fileName = fileName.substring(Constants.LIB.length());
        }else {
            fileName = Constants.LIB + fileName;
        }
        String fallBackPath = libPath + Constants.SEPARATOR + fileName;
        if(Files.exists(Path.of(fallBackPath))) {
            return fallBackPath;
        }
        throw new FrameworkException(ExceptionType.NATIVE, "Native library not found");
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
     *  Load a set of native library by environment variable, return the last one, throw an exception if library was not found in environment variables
     */
    public static SymbolLookup loadMultipleLibrary(String identifiers) {
        String[] s = identifiers.split(",");
        if(s.length > 0) {
            SymbolLookup r = null;
            for (String identifier : s) {
                r = loadLibrary(identifier);
            }
            return Objects.requireNonNull(r);
        }else {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED);
        }
    }

    /**
     *  Load a native library by environment variable, throw an exception if library was not found in environment variables
     */
    public static SymbolLookup loadLibrary(String identifier) {
        if(libPath == null) {
            throw new FrameworkException(ExceptionType.NATIVE, "Global libPath not found");
        }
        return Objects.requireNonNull(libraryCache.computeIfAbsent(identifier, i -> SymbolLookup.libraryLookup(getDynamicLibraryPath(i), globalArena)));
    }

    /**
     *  Load function from system library, such as strlen()
     *  Note that for methods from system library, there are no cache, every time a new MethodHandle would be created, and it's totally fine by JVM
     */
    public static MethodHandle nativeMethodHandle(String methodName, FunctionDescriptor functionDescriptor, Linker.Option... options) {
        MemorySegment method = linker.defaultLookup().find(methodName).orElseThrow(() -> new FrameworkException(ExceptionType.NATIVE, STR."Unable to locate [\{methodName}] native method"));
        return linker.downcallHandle(method, functionDescriptor, options);
    }

    /**
     *   Check if a memorySegment is a Null Pointer
     *   Note that heap segment always got an address of Zero, so this function shouldn't be used on heap segment
     */
    public static boolean checkNullPointer(MemorySegment memorySegment) {
        MemorySegment m = Objects.requireNonNull(memorySegment);
        return m.isNative() && m.address() == 0L;
    }

    /**
     *   Convert an intValue to its ASCII byte form
     */
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

    /**
     *   Underneath all the offset means byte offset, not element offset
     */

    public static byte getByte(MemorySegment m, long offset) {
        return (byte) BYTE_HANDLE.get(m, offset);
    }

    public static void setByte(MemorySegment m, long offset, byte b) {
        BYTE_HANDLE.set(m, offset, b);
    }

    public static short getShort(MemorySegment m, long offset) {
        return (short) SHORT_HANDLE.get(m, offset);
    }

    public static void setShort(MemorySegment m, long offset, short s) {
        SHORT_HANDLE.set(m, offset, s);
    }

    public static int getInt(MemorySegment m, long offset) {
        return (int) INT_HANDLE.get(m, offset);
    }

    public static void setInt(MemorySegment m, long offset, int i) {
        INT_HANDLE.set(m, offset, i);
    }

    public static long getLong(MemorySegment m, long offset) {
        return (long) LONG_HANDLE.get(m, offset);
    }

    public static void setLong(MemorySegment m, long offset, long l) {
        LONG_HANDLE.set(m, offset, l);
    }

    public static float getFloat(MemorySegment m, long offset) {
        return (float) FLOAT_HANDLE.get(m, offset);
    }

    public static void setFloat(MemorySegment m, long offset, float f) {
        FLOAT_HANDLE.set(m, offset, f);
    }

    public static double getDouble(MemorySegment m, long offset) {
        return (double) DOUBLE_HANDLE.get(m, offset);
    }

    public static void setDouble(MemorySegment m, long offset, double d) {
        DOUBLE_HANDLE.set(m, offset, d);
    }

    public static MemorySegment getAddress(MemorySegment m, long offset) {
        return (MemorySegment) ADDRESS_HANDLE.get(m, offset);
    }

    public static void setAddress(MemorySegment m, long offset, MemorySegment address) {
        ADDRESS_HANDLE.set(m, offset, address);
    }

    /**
     *   Convert a native segment to heap segment
     */
    public static MemorySegment toHeap(MemorySegment nativeSegment) {
        if(!nativeSegment.isNative()) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED);
        }
        if(checkNullPointer(nativeSegment)) {
            return MemorySegment.NULL;
        }
        long len = nativeSegment.byteSize();
        MemorySegment heapSegment = Allocator.HEAP.allocate(ValueLayout.JAVA_BYTE, len);
        MemorySegment.copy(nativeSegment, 0L, heapSegment, 0L, len);
        return heapSegment;
    }

    /**
     *   Convert a heap segment to native segment
     */
    public static MemorySegment toNative(MemorySegment heapSegment, Allocator allocator) {
        if(heapSegment.isNative() || !allocator.isNative()) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED);
        }
        long len = heapSegment.byteSize();
        MemorySegment nativeSegment = allocator.allocate(ValueLayout.JAVA_BYTE, len);
        MemorySegment.copy(heapSegment, 0L, nativeSegment, 0L, len);
        return nativeSegment;
    }

    /**
     *   Calculate a grown size for the collection with overflow checking
     */
    public static int grow(int currentSize) {
        int nextSize = currentSize + (currentSize >> 1);
        if(currentSize < 0 || nextSize < 0) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Possible overflow");
        }
        return nextSize;
    }
    public static long grow(long currentSize) {
        long nextSize = currentSize << 1;//currentSize + (currentSize >> 1);
        if(currentSize < 0L || nextSize < 0L) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Possible overflow");
        }
        return nextSize;
    }

}
