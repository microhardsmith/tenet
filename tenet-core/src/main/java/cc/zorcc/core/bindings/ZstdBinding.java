package cc.zorcc.core.bindings;

import cc.zorcc.core.Constants;
import cc.zorcc.core.Dyn;
import cc.zorcc.core.ExceptionType;
import cc.zorcc.core.FrameworkException;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 *   Adding support for Zstd compression/decompression using <a href="https://github.com/facebook/zstd"> zstd </a>
 */
public final class ZstdBinding {
    public static final int ZSTD_MIN_LEVEL = 1;
    public static final int ZSTD_MAX_LEVEL = 22;
    public static final int ZSTD_DEFAULT_LEVEL = 6;

    private static final MethodHandle zstdCompressBound;
    private static final MethodHandle zstdIsError;
    private static final MethodHandle zstdCompress;
    private static final MethodHandle zstdGetFrameContentSize;
    private static final MethodHandle zstdDecompress;

    static {
        SymbolLookup zstd = Dyn.loadDynLibrary("libzstd");
        zstdCompressBound = Dyn.mh(zstd, "ZSTD_compressBound",
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG));
        zstdIsError = Dyn.mh(zstd, "ZSTD_isError",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));
        zstdCompress = Dyn.mh(zstd, "ZSTD_compress",
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));
        zstdGetFrameContentSize = Dyn.mh(zstd, "ZSTD_getFrameContentSize",
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        zstdDecompress = Dyn.mh(zstd, "ZSTD_decompress",
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
    }

    /**
     *   ZstdBinding shouldn't be initialized
     */
    private ZstdBinding() {
        throw new UnsupportedOperationException();
    }

    public static long zstdCompressBound(long inputSize) {
        try {
            return (long) zstdCompressBound.invokeExact(inputSize);
        } catch (Throwable e) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static int zstdIsError(long code) {
        try {
            return (int) zstdIsError.invokeExact(code);
        } catch (Throwable e) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static long zstdCompress(MemorySegment dst, long dstCapacity, MemorySegment src, long srcSize, int compressionLevel) {
        try {
            return (long) zstdCompress.invokeExact(dst, dstCapacity, src, srcSize, compressionLevel);
        } catch (Throwable e) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static long zstdGetFrameContentSize(MemorySegment src, long srcSize) {
        try {
            return (long) zstdGetFrameContentSize.invokeExact(src, srcSize);
        } catch (Throwable e) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static long zstdDecompress(MemorySegment dst, long dstCapacity, MemorySegment src, long compressedSize) {
        try {
            return (long) zstdDecompress.invokeExact(dst, dstCapacity, src, compressedSize);
        } catch (Throwable e) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }
}
