package cn.zorcc.common.binding;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

public final class DeflateBinding {
    public static final int LIBDEFLATE_SUCCESS = 0;
    public static final int LIBDEFLATE_BAD_DATA = 1;
    public static final int LIBDEFLATE_SHORT_OUTPUT = 2;
    public static final int LIBDEFLATE_INSUFFICIENT_SPACE = 3;
    public static final int LIBDEFLATE_FASTEST_LEVEL = 1;
    public static final int LIBDEFLATE_SLOWEST_LEVEL = 12;
    private static final MethodHandle allocCompressor;
    private static final MethodHandle freeCompressor;
    private static final MethodHandle allocDecompressor;
    private static final MethodHandle freeDecompressor;
    private static final MethodHandle deflateCompress;
    private static final MethodHandle deflateCompressBound;
    private static final MethodHandle gzipCompress;
    private static final MethodHandle gzipCompressBound;
    private static final MethodHandle deflateDecompress;
    private static final MethodHandle gzipDecompress;

    static {
        SymbolLookup symbolLookup = NativeUtil.loadLibrary(Constants.DEFLATE);
        allocCompressor = NativeUtil.methodHandle(symbolLookup, "libdeflate_alloc_compressor", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        freeCompressor = NativeUtil.methodHandle(symbolLookup, "libdeflate_free_compressor", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        allocDecompressor = NativeUtil.methodHandle(symbolLookup, "libdeflate_alloc_decompressor", FunctionDescriptor.of(ValueLayout.ADDRESS));
        freeDecompressor = NativeUtil.methodHandle(symbolLookup, "libdeflate_free_decompressor", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        deflateCompress = NativeUtil.methodHandle(symbolLookup, "libdeflate_deflate_compress", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        deflateCompressBound = NativeUtil.methodHandle(symbolLookup, "libdeflate_deflate_compress_bound", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        gzipCompress = NativeUtil.methodHandle(symbolLookup, "libdeflate_gzip_compress", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        gzipCompressBound = NativeUtil.methodHandle(symbolLookup, "libdeflate_gzip_compress_bound", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        deflateDecompress = NativeUtil.methodHandle(symbolLookup, "libdeflate_deflate_decompress", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
        gzipDecompress = NativeUtil.methodHandle(symbolLookup, "libdeflate_gzip_decompress", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
    }

    /**
     *   Corresponding to `struct libdeflate_compressor* libdeflate_alloc_compressor(int compression_level)`
     */
    public static MemorySegment allocCompressor(int level) {
        try{
            return (MemorySegment) allocCompressor.invokeExact(level);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Failed to alloc compressor");
        }
    }

    /**
     *   Corresponding to `void libdeflate_free_compressor(struct libdeflate_compressor *compressor)`
     */
    public static void freeCompressor(MemorySegment compressor) {
        try{
            freeCompressor.invokeExact(compressor);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Failed to free compressor");
        }
    }

    /**
     *   Corresponding to `struct libdeflate_decompressor* libdeflate_alloc_decompressor(void)`
     */
    public static MemorySegment allocDecompressor() {
        try{
            return (MemorySegment) allocDecompressor.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Failed to alloc decompressor");
        }
    }

    /**
     *   Corresponding to `void libdeflate_free_decompressor(struct libdeflate_decompressor *decompressor)`
     */
    public static void freeDecompressor(MemorySegment decompressor) {
        try{
            freeDecompressor.invokeExact(decompressor);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Failed to free decompressor");
        }
    }

    /**
     *   Corresponding to `size_t libdeflate_deflate_compress(struct libdeflate_compressor* compressor, const void *in, size_t in_nbytes, void *out, size_t out_nbytes_avail)`
     */
    public static long deflateCompress(MemorySegment compressor, MemorySegment in, long inBytes, MemorySegment out, long outBytes) {
        try{
            return (long) deflateCompress.invokeExact(compressor, in, inBytes, out, outBytes);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Failed to perform deflateCompress");
        }
    }

    /**
     *   Corresponding to `size_t libdeflate_deflate_compress_bound(struct libdeflate_compressor *compressor, size_t in_nbytes)`
     */
    public static long deflateCompressBound(MemorySegment compressor, long inBytes) {
        try{
            return (long) deflateCompressBound.invokeExact(compressor, inBytes);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Failed to perform deflateCompressBound");
        }
    }

    /**
     *   Corresponding to `size_t libdeflate_gzip_compress(struct libdeflate_compressor* compressor, const void *in, size_t in_nbytes, void *out, size_t out_nbytes_avail)`
     */
    public static long gzipCompress(MemorySegment compressor, MemorySegment in, long inBytes, MemorySegment out, long outBytes) {
        try{
            return (long) gzipCompress.invokeExact(compressor, in, inBytes, out, outBytes);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Failed to perform gzipCompress");
        }
    }

    /**
     *   Corresponding to `size_t libdeflate_gzip_compress_bound(struct libdeflate_compressor *compressor, size_t in_nbytes)`
     */
    public static long gzipCompressBound(MemorySegment compressor, long inBytes) {
        try{
            return (long) gzipCompressBound.invokeExact(compressor, inBytes);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Failed to perform gzipCompressBound");
        }
    }

    /**
     *   Corresponding to `enum libdeflate_result libdeflate_deflate_decompress(struct libdeflate_decompressor* decompressor, const void* in, size_t in_nbytes, void* out, size_t out_nbytes_avail, size_t* actual_out_nbytes_ret)`
     */
    public static int deflateDecompress(MemorySegment decompressor, MemorySegment in, long inBytes, MemorySegment out, long outBytes, MemorySegment actualOutBytes) {
        try{
            return (int) deflateDecompress.invokeExact(decompressor, in, inBytes, out, outBytes, actualOutBytes);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Failed to perform deflateDecompress");
        }
    }

    /**
     *   Corresponding to `enum libdeflate_result libdeflate_gzip_decompress(struct libdeflate_decompressor* decompressor, const void* in, size_t in_nbytes, void* out, size_t out_nbytes_avail, size_t* actual_out_nbytes_ret)`
     */
    public static int gzipDecompress(MemorySegment decompressor, MemorySegment in, long inBytes, MemorySegment out, long outBytes, MemorySegment actualOutBytes) {
        try{
            return (int) gzipDecompress.invokeExact(decompressor, in, inBytes, out, outBytes, actualOutBytes);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Failed to perform gzipDecompress");
        }
    }
}
