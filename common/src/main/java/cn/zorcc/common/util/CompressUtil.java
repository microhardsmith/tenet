package cn.zorcc.common.util;

import cn.zorcc.common.Constants;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.zip.*;

/**
 *   This class provide easy access to gzip and deflate compression and decompression using JDK's implementation or libdeflate's implementation
 *   In general, JDK's implementation is a little faster when dataset is small, could be twice slower if the dataset was larger
 *   The compression and decompression speed of gzip and deflate algorithm are quite slow compared to other technic like zstd
 */
public final class CompressUtil {
    private static final Arena autoArena = NativeUtil.autoArena();
    public static final int LIBDEFLATE_SUCCESS = 0;
    public static final int LIBDEFLATE_BAD_DATA = 1;
    public static final int LIBDEFLATE_SHORT_OUTPUT = 2;
    public static final int LIBDEFLATE_INSUFFICIENT_SPACE = 3;
    public static final int LIBDEFLATE_FASTEST_LEVEL = 1;
    public static final int LIBDEFLATE_SLOWEST_LEVEL = 12;
    private static final int CHUNK_SIZE = 4 * Constants.KB;
    private static final int ESTIMATE_RATIO = 3;
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

    private CompressUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     *   Corresponding to `struct libdeflate_compressor* libdeflate_alloc_compressor(int compression_level)`
     */
    private static MemorySegment allocCompressor(int level) {
        try{
            return (MemorySegment) allocCompressor.invokeExact(level);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Failed to alloc compressor");
        }
    }

    /**
     *   Corresponding to `void libdeflate_free_compressor(struct libdeflate_compressor *compressor)`
     */
    private static void freeCompressor(MemorySegment compressor) {
        try{
            freeCompressor.invokeExact(compressor);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Failed to free compressor");
        }
    }

    /**
     *   Corresponding to `struct libdeflate_decompressor* libdeflate_alloc_decompressor(void)`
     */
    private static MemorySegment allocDecompressor() {
        try{
            return (MemorySegment) allocDecompressor.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Failed to alloc decompressor");
        }
    }

    /**
     *   Corresponding to `void libdeflate_free_decompressor(struct libdeflate_decompressor *decompressor)`
     */
    private static void freeDecompressor(MemorySegment decompressor) {
        try{
            freeDecompressor.invokeExact(decompressor);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Failed to free decompressor");
        }
    }

    /**
     *   Corresponding to `size_t libdeflate_deflate_compress(struct libdeflate_compressor* compressor, const void *in, size_t in_nbytes, void *out, size_t out_nbytes_avail)`
     */
    private static long deflateCompress(MemorySegment compressor, MemorySegment in, long inBytes, MemorySegment out, long outBytes) {
        try{
            return (long) deflateCompress.invokeExact(compressor, in, inBytes, out, outBytes);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Failed to perform deflateCompress");
        }
    }

    /**
     *   Corresponding to `size_t libdeflate_deflate_compress_bound(struct libdeflate_compressor *compressor, size_t in_nbytes)`
     */
    private static long deflateCompressBound(MemorySegment compressor, long inBytes) {
        try{
            return (long) deflateCompressBound.invokeExact(compressor, inBytes);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Failed to perform deflateCompressBound");
        }
    }

    /**
     *   Corresponding to `size_t libdeflate_gzip_compress(struct libdeflate_compressor* compressor, const void *in, size_t in_nbytes, void *out, size_t out_nbytes_avail)`
     */
    private static long gzipCompress(MemorySegment compressor, MemorySegment in, long inBytes, MemorySegment out, long outBytes) {
        try{
            return (long) gzipCompress.invokeExact(compressor, in, inBytes, out, outBytes);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Failed to perform gzipCompress");
        }
    }

    /**
     *   Corresponding to `size_t libdeflate_gzip_compress_bound(struct libdeflate_compressor *compressor, size_t in_nbytes)`
     */
    private static long gzipCompressBound(MemorySegment compressor, long inBytes) {
        try{
            return (long) gzipCompressBound.invokeExact(compressor, inBytes);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Failed to perform gzipCompressBound");
        }
    }

    /**
     *   Corresponding to `enum libdeflate_result libdeflate_deflate_decompress(struct libdeflate_decompressor* decompressor, const void* in, size_t in_nbytes, void* out, size_t out_nbytes_avail, size_t* actual_out_nbytes_ret)`
     */
    private static int deflateDecompress(MemorySegment decompressor, MemorySegment in, long inBytes, MemorySegment out, long outBytes, MemorySegment actualOutBytes) {
        try{
            return (int) deflateDecompress.invokeExact(decompressor, in, inBytes, out, outBytes, actualOutBytes);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Failed to perform deflateDecompress");
        }
    }

    /**
     *   Corresponding to `enum libdeflate_result libdeflate_gzip_decompress(struct libdeflate_decompressor* decompressor, const void* in, size_t in_nbytes, void* out, size_t out_nbytes_avail, size_t* actual_out_nbytes_ret)`
     */
    private static int gzipDecompress(MemorySegment decompressor, MemorySegment in, long inBytes, MemorySegment out, long outBytes, MemorySegment actualOutBytes) {
        try{
            return (int) gzipDecompress.invokeExact(decompressor, in, inBytes, out, outBytes, actualOutBytes);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Failed to perform gzipDecompress");
        }
    }

    private static MemorySegment toNativeSegment(MemorySegment in) {
        if(in.isNative()) {
            return in;
        }else {
            long byteSize = in.byteSize();
            MemorySegment nativeSegment = autoArena.allocateArray(ValueLayout.JAVA_BYTE, byteSize);
            MemorySegment.copy(in, Constants.ZERO, nativeSegment, Constants.ZERO, byteSize);
            return nativeSegment;
        }
    }

    public static MemorySegment compressUsingDeflate(MemorySegment input, int level) {
        MemorySegment in = toNativeSegment(input);
        if(level < LIBDEFLATE_FASTEST_LEVEL || level > LIBDEFLATE_SLOWEST_LEVEL) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Unsupported level");
        }
        long inBytes = in.byteSize();
        MemorySegment compressor = allocCompressor(level);
        try{
            long outBytes = deflateCompressBound(compressor, inBytes);
            MemorySegment out = autoArena.allocateArray(ValueLayout.JAVA_BYTE, outBytes);
            long compressed = deflateCompress(compressor, in, inBytes, out, outBytes);
            if(compressed <= Constants.ZERO) {
                throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
            }
            return out.asSlice(Constants.ZERO, compressed);
        }finally {
            freeCompressor(compressor);
        }
    }

    public static MemorySegment compressUsingGzip(MemorySegment input, int level) {
        MemorySegment in = toNativeSegment(input);
        if(level < LIBDEFLATE_FASTEST_LEVEL || level > LIBDEFLATE_SLOWEST_LEVEL) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Unsupported level");
        }
        long inBytes = in.byteSize();
        MemorySegment compressor = allocCompressor(level);
        try{
            long outBytes = gzipCompressBound(compressor, inBytes);
            MemorySegment out = autoArena.allocateArray(ValueLayout.JAVA_BYTE, outBytes);
            long compressed = gzipCompress(compressor, in, inBytes, out, outBytes);
            if(compressed <= Constants.ZERO) {
                throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
            }
            return out.asSlice(Constants.ZERO, compressed);
        }finally {
            freeCompressor(compressor);
        }
    }

    public static MemorySegment decompressUsingDeflate(MemorySegment input) {
        return decompressUsingDeflate(input, Long.MIN_VALUE);
    }

    public static MemorySegment decompressUsingDeflate(MemorySegment input, long originalSize) {
        MemorySegment in = toNativeSegment(input);
        long inBytes = in.byteSize();
        MemorySegment decompressor = allocDecompressor();
        try{
            MemorySegment out = autoArena.allocateArray(ValueLayout.JAVA_BYTE, originalSize > 0 ? originalSize : inBytes * ESTIMATE_RATIO);
            MemorySegment actualOutBytes = originalSize > 0 ? NativeUtil.NULL_POINTER : autoArena.allocate(ValueLayout.JAVA_LONG);
            for( ; ; ) {
                switch (deflateDecompress(decompressor, in, inBytes, out, out.byteSize(), actualOutBytes)) {
                    case LIBDEFLATE_SUCCESS -> {
                        long written = NativeUtil.getLong(actualOutBytes, Constants.ZERO);
                        return out.asSlice(Constants.ZERO, written);
                    }
                    case LIBDEFLATE_BAD_DATA -> throw new FrameworkException(ExceptionType.COMPRESS, "Bad data");
                    case LIBDEFLATE_SHORT_OUTPUT -> throw new FrameworkException(ExceptionType.COMPRESS, originalSize > Constants.ZERO ? "Fewer originalSize expected" : Constants.UNREACHED);
                    case LIBDEFLATE_INSUFFICIENT_SPACE -> {
                        if(originalSize > 0) {
                            throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
                        }else {
                            out = autoArena.allocateArray(ValueLayout.JAVA_BYTE, out.byteSize() << 1);
                        }
                    }
                }
            }
        }finally {
            freeDecompressor(decompressor);
        }
    }

    public static MemorySegment decompressUsingGzip(MemorySegment input) {
        return decompressUsingGzip(input, Long.MIN_VALUE);
    }

    public static MemorySegment decompressUsingGzip(MemorySegment input, long originalSize) {
        MemorySegment in = toNativeSegment(input);
        long inBytes = in.byteSize();
        MemorySegment decompressor = allocDecompressor();
        try{
            MemorySegment out = autoArena.allocateArray(ValueLayout.JAVA_BYTE, originalSize > 0 ? originalSize : inBytes * ESTIMATE_RATIO);
            MemorySegment actualOutBytes = originalSize > Constants.ZERO ? NativeUtil.NULL_POINTER : autoArena.allocate(ValueLayout.JAVA_LONG);
            for( ; ; ) {
                switch (gzipDecompress(decompressor, in, inBytes, out, out.byteSize(), actualOutBytes)) {
                    case LIBDEFLATE_SUCCESS -> {
                        long written = NativeUtil.getLong(actualOutBytes, Constants.ZERO);
                        return out.asSlice(Constants.ZERO, written);
                    }
                    case LIBDEFLATE_BAD_DATA -> throw new FrameworkException(ExceptionType.COMPRESS, "Bad data");
                    case LIBDEFLATE_SHORT_OUTPUT -> throw new FrameworkException(ExceptionType.COMPRESS, originalSize > Constants.ZERO ? "Fewer originalSize expected" : Constants.UNREACHED);
                    case LIBDEFLATE_INSUFFICIENT_SPACE -> {
                        if(originalSize > Constants.ZERO) {
                            throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
                        }else {
                            out = autoArena.allocateArray(ValueLayout.JAVA_BYTE, out.byteSize() << 1);
                        }
                    }
                }
            }
        }finally {
            freeDecompressor(decompressor);
        }
    }


    @SuppressWarnings("unused")
    public static byte[] compressUsingJdkGzip(final byte[] rawData) {
        return compressUsingJdkGzip(rawData, Deflater.DEFAULT_COMPRESSION);
    }

    public static byte[] compressUsingJdkGzip(final byte[] rawData, final int level) {
        try(WriteBuffer writeBuffer = WriteBuffer.newHeapWriteBuffer(); GZIPOutputStream gzipOutputStream = new GZIPOutputStream(writeBuffer){{
            def.setLevel(level >= Deflater.BEST_SPEED && level <= Deflater.BEST_COMPRESSION ? level : Deflater.DEFAULT_COMPRESSION);
        }}) {
            gzipOutputStream.write(rawData);
            gzipOutputStream.finish();
            return writeBuffer.toArray();
        } catch (IOException e) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Unable to perform gzip compression", e);
        }
    }

    public static byte[] decompressUsingJdkGzip(final byte[] compressedData) {
        try (WriteBuffer writeBuffer = WriteBuffer.newHeapWriteBuffer(); GZIPInputStream gzipIn = new GZIPInputStream(new ByteArrayInputStream(compressedData))) {
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            while ((bytesRead = gzipIn.read(buffer)) != -1) {
                writeBuffer.write(buffer, 0, bytesRead);
            }
            return writeBuffer.toArray();
        }catch (IOException e) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Unable to perform gzip decompression", e);
        }
    }

    @SuppressWarnings("unused")
    public static byte[] compressUsingJdkDeflate(final byte[] rawData) {
        return compressUsingJdkDeflate(rawData, Deflater.DEFAULT_COMPRESSION);
    }

    public static byte[] compressUsingJdkDeflate(final byte[] rawData, final int level) {
        Deflater deflater = new Deflater(level >= Deflater.BEST_SPEED && level <= Deflater.BEST_COMPRESSION ? level : Deflater.DEFAULT_COMPRESSION);
        deflater.setInput(rawData);
        deflater.finish();
        byte[] buffer = new byte[CHUNK_SIZE];
        try(WriteBuffer writeBuffer = WriteBuffer.newHeapWriteBuffer()) {
            while (!deflater.finished()) {
                int len = deflater.deflate(buffer);
                writeBuffer.writeBytes(buffer, Constants.ZERO, len);
            }
            return writeBuffer.toArray();
        } finally {
            deflater.end();
        }
    }

    public static byte[] decompressUsingJdkDeflate(final byte[] compressedData) {
        Inflater inflater = new Inflater();
        inflater.setInput(compressedData);
        try (WriteBuffer writeBuffer = WriteBuffer.newHeapWriteBuffer()) {
            byte[] buffer = new byte[CHUNK_SIZE];
            while (!inflater.finished()) {
                int decompressLen = inflater.inflate(buffer);
                writeBuffer.writeBytes(buffer, Constants.ZERO, decompressLen);
            }
            return writeBuffer.toArray();
        }catch (DataFormatException e) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Unable to perform deflate decompression", e);
        }finally {
            inflater.end();
        }
    }
}
