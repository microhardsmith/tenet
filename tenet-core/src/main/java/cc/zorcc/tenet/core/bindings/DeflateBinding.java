package cc.zorcc.tenet.core.bindings;

import cc.zorcc.tenet.core.Constants;
import cc.zorcc.tenet.core.Dyn;
import cc.zorcc.tenet.core.TenetException;
import cc.zorcc.tenet.core.ExceptionType;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 *   Adding support for GZIP and Deflate compression/decompression using <a href="https://github.com/ebiggers/libdeflate"> libdeflate </a>
 */
public final class DeflateBinding {
    public static final int LIBDEFLATE_SUCCESS = 0;
    public static final int LIBDEFLATE_BAD_DATA = 1;
    public static final int LIBDEFLATE_SHORT_OUTPUT = 2;
    public static final int LIBDEFLATE_INSUFFICIENT_SPACE = 3;
    public static final int LIBDEFLATE_FASTEST_LEVEL = 1;
    public static final int LIBDEFLATE_SLOWEST_LEVEL = 12;
    public static final int LIBDEFLATE_DEFAULT_LEVEL = 6;

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
        SymbolLookup deflate = Dyn.loadDynLibrary(Constants.DEFLATE);
        allocCompressor = Dyn.mh(deflate, "libdeflate_alloc_compressor",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        freeCompressor = Dyn.mh(deflate, "libdeflate_free_compressor",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        allocDecompressor = Dyn.mh(deflate, "libdeflate_alloc_decompressor",
                FunctionDescriptor.of(ValueLayout.ADDRESS));
        freeDecompressor = Dyn.mh(deflate, "libdeflate_free_decompressor",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        deflateCompress = Dyn.mh(deflate, "libdeflate_deflate_compress",
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        deflateCompressBound = Dyn.mh(deflate, "libdeflate_deflate_compress_bound",
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        gzipCompress = Dyn.mh(deflate, "libdeflate_gzip_compress",
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        gzipCompressBound = Dyn.mh(deflate, "libdeflate_gzip_compress_bound",
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        deflateDecompress = Dyn.mh(deflate, "libdeflate_deflate_decompress",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
        gzipDecompress = Dyn.mh(deflate, "libdeflate_gzip_decompress",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
    }

    /**
     *   DeflateBinding shouldn't be initialized
     */
    private DeflateBinding() {
        throw new UnsupportedOperationException();
    }

    public static MemorySegment allocCompressor(int level) {
        try{
            return (MemorySegment) allocCompressor.invokeExact(level);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static void freeCompressor(MemorySegment compressor) {
        try{
            freeCompressor.invokeExact(compressor);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static MemorySegment allocDecompressor() {
        try{
            return (MemorySegment) allocDecompressor.invokeExact();
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static void freeDecompressor(MemorySegment decompressor) {
        try{
            freeDecompressor.invokeExact(decompressor);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static long deflateCompress(MemorySegment compressor, MemorySegment in, long inBytes, MemorySegment out, long outBytes) {
        try{
            return (long) deflateCompress.invokeExact(compressor, in, inBytes, out, outBytes);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static long deflateCompressBound(MemorySegment compressor, long inBytes) {
        try{
            return (long) deflateCompressBound.invokeExact(compressor, inBytes);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static long gzipCompress(MemorySegment compressor, MemorySegment in, long inBytes, MemorySegment out, long outBytes) {
        try{
            return (long) gzipCompress.invokeExact(compressor, in, inBytes, out, outBytes);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static long gzipCompressBound(MemorySegment compressor, long inBytes) {
        try{
            return (long) gzipCompressBound.invokeExact(compressor, inBytes);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static int deflateDecompress(MemorySegment decompressor, MemorySegment in, long inBytes, MemorySegment out, long outBytes, MemorySegment actualOutBytes) {
        try{
            return (int) deflateDecompress.invokeExact(decompressor, in, inBytes, out, outBytes, actualOutBytes);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static int gzipDecompress(MemorySegment decompressor, MemorySegment in, long inBytes, MemorySegment out, long outBytes, MemorySegment actualOutBytes) {
        try{
            return (int) gzipDecompress.invokeExact(decompressor, in, inBytes, out, outBytes, actualOutBytes);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }
}
