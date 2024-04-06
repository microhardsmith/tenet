package cn.zorcc.common.bindings;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

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
        SymbolLookup zstd = NativeUtil.loadLibrary(Constants.ZSTD);
        zstdCompressBound = NativeUtil.methodHandle(zstd, "ZSTD_compressBound",
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG));
        zstdIsError = NativeUtil.methodHandle(zstd, "ZSTD_isError",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));
        zstdCompress = NativeUtil.methodHandle(zstd, "ZSTD_compress",
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));
        zstdGetFrameContentSize = NativeUtil.methodHandle(zstd, "ZSTD_getFrameContentSize",
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        zstdDecompress = NativeUtil.methodHandle(zstd, "ZSTD_decompress",
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
    }
    private ZstdBinding() {
        throw new UnsupportedOperationException();
    }

    public static long zstdCompressBound(long inputSize) {
        try {
            return (long) zstdCompressBound.invokeExact(inputSize);
        } catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
        }
    }

    public static int zstdIsError(long code) {
        try {
            return (int) zstdIsError.invokeExact(code);
        } catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
        }
    }

    public static long zstdCompress(MemorySegment dst, long dstCapacity, MemorySegment src, long srcSize, int compressionLevel) {
        try {
            return (long) zstdCompress.invokeExact(dst, dstCapacity, src, srcSize, compressionLevel);
        } catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
        }
    }

    public static long zstdGetFrameContentSize(MemorySegment src, long srcSize) {
        try {
            return (long) zstdGetFrameContentSize.invokeExact(src, srcSize);
        } catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
        }
    }

    public static long zstdDecompress(MemorySegment dst, long dstCapacity, MemorySegment src, long compressedSize) {
        try {
            return (long) zstdDecompress.invokeExact(dst, dstCapacity, src, compressedSize);
        } catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
        }
    }


}
