package cn.zorcc.common.util;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.bindings.DeflateBinding;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.structure.WriteBuffer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.ValueLayout;
import java.util.zip.*;

/**
 *   This class provide easy access to gzip and deflate compression and decompression using JDK's implementation or libdeflate's implementation
 *   In general, JDK's implementation is a little bit faster when dataset is small, could be twice slower if the dataset was larger
 *   The compression and decompression speed of gzip and deflate algorithm are quite slow compared to other technic like zstd
 */
@SuppressWarnings("unused")
public final class CompressUtil {
    private static final int CHUNK_SIZE = 4 * Constants.KB;
    private static final int ESTIMATE_RATIO = 3;

    private CompressUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     *   When using libdeflate, level should between DeflateBinding.LIBDEFLATE_SLOWEST_LEVEL and DeflateBinding.LIBDEFLATE_FASTEST_LEVEL
     */
    public static MemorySegment compressUsingDeflate(MemorySegment input, int level, SegmentAllocator allocator) {
        MemorySegment in = NativeUtil.toNativeSegment(input);
        level = level >= DeflateBinding.LIBDEFLATE_FASTEST_LEVEL && level <= DeflateBinding.LIBDEFLATE_SLOWEST_LEVEL ? level : DeflateBinding.LIBDEFLATE_DEFAULT_LEVEL;
        long inBytes = in.byteSize();
        MemorySegment compressor = DeflateBinding.allocCompressor(level);
        try{
            long outBytes = DeflateBinding.deflateCompressBound(compressor, inBytes);
            MemorySegment out = allocator.allocate(ValueLayout.JAVA_BYTE, outBytes);
            long compressed = DeflateBinding.deflateCompress(compressor, in, inBytes, out, outBytes);
            if(compressed <= 0L) {
                throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
            }
            return out.asSlice(0L, compressed);
        }finally {
            DeflateBinding.freeCompressor(compressor);
        }
    }

    public static MemorySegment compressUsingGzip(MemorySegment input, int level, SegmentAllocator allocator) {
        MemorySegment in = NativeUtil.toNativeSegment(input);
        level = level >= DeflateBinding.LIBDEFLATE_FASTEST_LEVEL && level <= DeflateBinding.LIBDEFLATE_SLOWEST_LEVEL ? level : DeflateBinding.LIBDEFLATE_DEFAULT_LEVEL;
        long inBytes = in.byteSize();
        MemorySegment compressor = DeflateBinding.allocCompressor(level);
        try{
            long outBytes = DeflateBinding.gzipCompressBound(compressor, inBytes);
            MemorySegment out = allocator.allocate(ValueLayout.JAVA_BYTE, outBytes);
            long compressed = DeflateBinding.gzipCompress(compressor, in, inBytes, out, outBytes);
            if(compressed <= 0L) {
                throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
            }
            return out.asSlice(0L, compressed);
        }finally {
            DeflateBinding.freeCompressor(compressor);
        }
    }

    public static MemorySegment decompressUsingDeflate(MemorySegment input, SegmentAllocator allocator) {
        return decompressUsingDeflate(input, Long.MIN_VALUE, allocator);
    }

    public static MemorySegment decompressUsingDeflate(MemorySegment input, long originalSize, SegmentAllocator allocator) {
        MemorySegment in = NativeUtil.toNativeSegment(input);
        long inBytes = in.byteSize();
        MemorySegment decompressor = DeflateBinding.allocDecompressor();
        try{
            MemorySegment out = allocator.allocate(ValueLayout.JAVA_BYTE, originalSize > 0L ? originalSize : inBytes * ESTIMATE_RATIO);
            MemorySegment actualOutBytes = originalSize > 0L ? MemorySegment.NULL : allocator.allocate(ValueLayout.JAVA_LONG);
            for( ; ; ) {
                switch (DeflateBinding.deflateDecompress(decompressor, in, inBytes, out, out.byteSize(), actualOutBytes)) {
                    case DeflateBinding.LIBDEFLATE_SUCCESS -> {
                        long written = actualOutBytes.get(ValueLayout.JAVA_LONG_UNALIGNED, 0L);
                        return out.asSlice(0L, written);
                    }
                    case DeflateBinding.LIBDEFLATE_BAD_DATA -> throw new FrameworkException(ExceptionType.COMPRESS, "Bad data");
                    case DeflateBinding.LIBDEFLATE_SHORT_OUTPUT -> throw new FrameworkException(ExceptionType.COMPRESS, originalSize > 0L ? "Fewer originalSize expected" : Constants.UNREACHED);
                    case DeflateBinding.LIBDEFLATE_INSUFFICIENT_SPACE -> {
                        if(originalSize > 0) {
                            throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
                        }else {
                            out = allocator.allocate(ValueLayout.JAVA_BYTE, out.byteSize() << 1);
                        }
                    }
                    default -> throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
                }
            }
        }finally {
            DeflateBinding.freeDecompressor(decompressor);
        }
    }

    public static MemorySegment decompressUsingGzip(MemorySegment input, SegmentAllocator allocator) {
        return decompressUsingGzip(input, Long.MIN_VALUE, allocator);
    }

    public static MemorySegment decompressUsingGzip(MemorySegment input, long originalSize, SegmentAllocator allocator) {
        MemorySegment in = NativeUtil.toNativeSegment(input);
        long inBytes = in.byteSize();
        MemorySegment decompressor = DeflateBinding.allocDecompressor();
        try{
            MemorySegment out = allocator.allocate(ValueLayout.JAVA_BYTE, originalSize > 0L ? originalSize : inBytes * ESTIMATE_RATIO);
            MemorySegment actualOutBytes = originalSize > 0L ? MemorySegment.NULL : allocator.allocate(ValueLayout.JAVA_LONG);
            for( ; ; ) {
                switch (DeflateBinding.gzipDecompress(decompressor, in, inBytes, out, out.byteSize(), actualOutBytes)) {
                    case DeflateBinding.LIBDEFLATE_SUCCESS -> {
                        long written = actualOutBytes.get(ValueLayout.JAVA_LONG, 0L);
                        return out.asSlice(0L, written);
                    }
                    case DeflateBinding.LIBDEFLATE_BAD_DATA -> throw new FrameworkException(ExceptionType.COMPRESS, "Bad data");
                    case DeflateBinding.LIBDEFLATE_SHORT_OUTPUT -> throw new FrameworkException(ExceptionType.COMPRESS, originalSize > 0L ? "Fewer originalSize expected" : Constants.UNREACHED);
                    case DeflateBinding.LIBDEFLATE_INSUFFICIENT_SPACE -> {
                        if(originalSize > 0L) {
                            throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
                        }else {
                            out = allocator.allocate(ValueLayout.JAVA_BYTE, out.byteSize() << 1);
                        }
                    }
                    default -> throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
                }
            }
        }finally {
            DeflateBinding.freeDecompressor(decompressor);
        }
    }


    /**
     *   When using gzip and deflate implemented by JDK, level should between Deflater.BEST_SPEED and Deflater.BEST_COMPRESSION
     */
    public static byte[] compressUsingJdkGzip(final byte[] rawData) {
        return compressUsingJdkGzip(rawData, Deflater.DEFAULT_COMPRESSION);
    }

    public static byte[] compressUsingJdkGzip(final byte[] rawData, final int level) {
        try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(rawData.length); GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream){{
            def.setLevel(level >= Deflater.BEST_SPEED && level <= Deflater.BEST_COMPRESSION ? level : Deflater.DEFAULT_COMPRESSION);
        }}) {
            gzipOutputStream.write(rawData);
            gzipOutputStream.finish();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Unable to perform gzip compression", e);
        }
    }

    public static byte[] decompressUsingJdkGzip(final byte[] compressedData) {
        try (WriteBuffer writeBuffer = WriteBuffer.newHeapWriteBuffer(compressedData.length); GZIPInputStream gzipIn = new GZIPInputStream(new ByteArrayInputStream(compressedData))) {
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            while ((bytesRead = gzipIn.read(buffer)) != -1) {
                writeBuffer.writeBytes(buffer, 0, bytesRead);
            }
            return writeBuffer.asArray();
        }catch (IOException e) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Unable to perform gzip decompression", e);
        }
    }


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
                writeBuffer.writeBytes(buffer, 0, len);
            }
            return writeBuffer.asArray();
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
                writeBuffer.writeBytes(buffer, 0, decompressLen);
            }
            return writeBuffer.asArray();
        }catch (DataFormatException e) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Unable to perform deflate decompression", e);
        }finally {
            inflater.end();
        }
    }
}
