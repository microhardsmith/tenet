package cn.zorcc.common.util;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.bindings.DeflateBinding;
import cn.zorcc.common.exception.FrameworkException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.zip.*;

/**
 *   This class provide easy access to gzip and deflate compression and decompression using JDK's implementation or libdeflate's implementation
 *   In general, JDK's implementation is a little faster when dataset is small, could be twice slower if the dataset was larger
 *   The compression and decompression speed of gzip and deflate algorithm are quite slow compared to other technic like zstd
 */
@SuppressWarnings("unused")
public final class CompressUtil {
    private static final Arena autoArena = NativeUtil.autoArena();
    private static final int CHUNK_SIZE = 4 * Constants.KB;
    private static final int ESTIMATE_RATIO = 3;

    private CompressUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     *   When using libdeflate, level should between DeflateBinding.LIBDEFLATE_SLOWEST_LEVEL and DeflateBinding.LIBDEFLATE_FASTEST_LEVEL
     */
    public static MemorySegment compressUsingDeflate(MemorySegment input, int level) {
        MemorySegment in = NativeUtil.toNativeSegment(input);
        level = level >= DeflateBinding.LIBDEFLATE_FASTEST_LEVEL && level <= DeflateBinding.LIBDEFLATE_SLOWEST_LEVEL ? level : DeflateBinding.LIBDEFLATE_DEFAULT_LEVEL;
        long inBytes = in.byteSize();
        MemorySegment compressor = DeflateBinding.allocCompressor(level);
        try{
            long outBytes = DeflateBinding.deflateCompressBound(compressor, inBytes);
            MemorySegment out = autoArena.allocateArray(ValueLayout.JAVA_BYTE, outBytes);
            long compressed = DeflateBinding.deflateCompress(compressor, in, inBytes, out, outBytes);
            if(compressed <= 0) {
                throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
            }
            return out.asSlice(0, compressed);
        }finally {
            DeflateBinding.freeCompressor(compressor);
        }
    }

    public static MemorySegment compressUsingGzip(MemorySegment input, int level) {
        MemorySegment in = NativeUtil.toNativeSegment(input);
        level = level >= DeflateBinding.LIBDEFLATE_FASTEST_LEVEL && level <= DeflateBinding.LIBDEFLATE_SLOWEST_LEVEL ? level : DeflateBinding.LIBDEFLATE_DEFAULT_LEVEL;
        long inBytes = in.byteSize();
        MemorySegment compressor = DeflateBinding.allocCompressor(level);
        try{
            long outBytes = DeflateBinding.gzipCompressBound(compressor, inBytes);
            MemorySegment out = autoArena.allocateArray(ValueLayout.JAVA_BYTE, outBytes);
            long compressed = DeflateBinding.gzipCompress(compressor, in, inBytes, out, outBytes);
            if(compressed <= 0) {
                throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
            }
            return out.asSlice(0, compressed);
        }finally {
            DeflateBinding.freeCompressor(compressor);
        }
    }

    public static MemorySegment decompressUsingDeflate(MemorySegment input) {
        return decompressUsingDeflate(input, Long.MIN_VALUE);
    }

    public static MemorySegment decompressUsingDeflate(MemorySegment input, long originalSize) {
        MemorySegment in = NativeUtil.toNativeSegment(input);
        long inBytes = in.byteSize();
        MemorySegment decompressor = DeflateBinding.allocDecompressor();
        try{
            MemorySegment out = autoArena.allocateArray(ValueLayout.JAVA_BYTE, originalSize > 0 ? originalSize : inBytes * ESTIMATE_RATIO);
            MemorySegment actualOutBytes = originalSize > 0 ? NativeUtil.NULL_POINTER : autoArena.allocate(ValueLayout.JAVA_LONG);
            for( ; ; ) {
                switch (DeflateBinding.deflateDecompress(decompressor, in, inBytes, out, out.byteSize(), actualOutBytes)) {
                    case DeflateBinding.LIBDEFLATE_SUCCESS -> {
                        long written = NativeUtil.getLong(actualOutBytes, 0);
                        return out.asSlice(0, written);
                    }
                    case DeflateBinding.LIBDEFLATE_BAD_DATA -> throw new FrameworkException(ExceptionType.COMPRESS, "Bad data");
                    case DeflateBinding.LIBDEFLATE_SHORT_OUTPUT -> throw new FrameworkException(ExceptionType.COMPRESS, originalSize > 0 ? "Fewer originalSize expected" : Constants.UNREACHED);
                    case DeflateBinding.LIBDEFLATE_INSUFFICIENT_SPACE -> {
                        if(originalSize > 0) {
                            throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
                        }else {
                            out = autoArena.allocateArray(ValueLayout.JAVA_BYTE, out.byteSize() << 1);
                        }
                    }
                }
            }
        }finally {
            DeflateBinding.freeDecompressor(decompressor);
        }
    }

    public static MemorySegment decompressUsingGzip(MemorySegment input) {
        return decompressUsingGzip(input, Long.MIN_VALUE);
    }

    public static MemorySegment decompressUsingGzip(MemorySegment input, long originalSize) {
        MemorySegment in = NativeUtil.toNativeSegment(input);
        long inBytes = in.byteSize();
        MemorySegment decompressor = DeflateBinding.allocDecompressor();
        try{
            MemorySegment out = autoArena.allocateArray(ValueLayout.JAVA_BYTE, originalSize > 0 ? originalSize : inBytes * ESTIMATE_RATIO);
            MemorySegment actualOutBytes = originalSize > 0 ? NativeUtil.NULL_POINTER : autoArena.allocate(ValueLayout.JAVA_LONG);
            for( ; ; ) {
                switch (DeflateBinding.gzipDecompress(decompressor, in, inBytes, out, out.byteSize(), actualOutBytes)) {
                    case DeflateBinding.LIBDEFLATE_SUCCESS -> {
                        long written = NativeUtil.getLong(actualOutBytes, 0);
                        return out.asSlice(0, written);
                    }
                    case DeflateBinding.LIBDEFLATE_BAD_DATA -> throw new FrameworkException(ExceptionType.COMPRESS, "Bad data");
                    case DeflateBinding.LIBDEFLATE_SHORT_OUTPUT -> throw new FrameworkException(ExceptionType.COMPRESS, originalSize > 0 ? "Fewer originalSize expected" : Constants.UNREACHED);
                    case DeflateBinding.LIBDEFLATE_INSUFFICIENT_SPACE -> {
                        if(originalSize > 0) {
                            throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
                        }else {
                            out = autoArena.allocateArray(ValueLayout.JAVA_BYTE, out.byteSize() << 1);
                        }
                    }
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
            return writeBuffer.toArray();
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
                writeBuffer.writeBytes(buffer, 0, decompressLen);
            }
            return writeBuffer.toArray();
        }catch (DataFormatException e) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Unable to perform deflate decompression", e);
        }finally {
            inflater.end();
        }
    }
}
