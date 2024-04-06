package cn.zorcc.common.util;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.bindings.BrotliBinding;
import cn.zorcc.common.bindings.DeflateBinding;
import cn.zorcc.common.bindings.ZstdBinding;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.structure.Allocator;
import cn.zorcc.common.structure.MemApi;
import cn.zorcc.common.structure.WriteBuffer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.zip.*;

/**
 *   This class provide easy access to gzip and deflate compression and decompression using JDK's implementation or libdeflate's implementation
 *   In general, JDK's implementation is a little bit faster when dataset is small, could be twice slower if the dataset was larger
 *   The compression and decompression speed of gzip and deflate algorithm are quite slow compared to other technic like zstd
 *   When using compression and decompression, the lifecycle of a native segment must be well taken care of, so all the API are forced to be consuming the native segments and return heap segments
 *   thus caller wouldn't be worrying about the lifecycle of the returned segment
 */
@SuppressWarnings("unused")
public final class CompressUtil {
    private static final int CHUNK_SIZE = 4 * Constants.KB;
    private static final int ESTIMATE_RATIO = 3;

    private CompressUtil() {
        throw new UnsupportedOperationException();
    }

    public static MemorySegment compressUsingZstd(MemorySegment input, MemApi memApi) {
        return compressUsingZstd(input, ZstdBinding.ZSTD_DEFAULT_LEVEL, memApi);
    }

    public static MemorySegment compressUsingZstd(MemorySegment input, int level, MemApi memApi) {
        if(!input.isNative()) {
            throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
        }
        level = level < ZstdBinding.ZSTD_MIN_LEVEL || level > ZstdBinding.ZSTD_MAX_LEVEL ? ZstdBinding.ZSTD_DEFAULT_LEVEL : level;
        long upperBound = ZstdBinding.zstdCompressBound(input.byteSize());
        if(ZstdBinding.zstdIsError(upperBound) == 1) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Failed to fetch ZSTD upper bound");
        }
        MemorySegment out = memApi.allocateMemory(upperBound).reinterpret(upperBound);
        try{
            long decompressedSize = ZstdBinding.zstdCompress(out, out.byteSize(), input, input.byteSize(), level);
            return NativeUtil.toHeap(decompressedSize == out.byteSize() ? out : out.asSlice(0L, decompressedSize));
        } finally {
            memApi.freeMemory(out);
        }
    }

    public static MemorySegment compressUsingBrotli(MemorySegment input, MemApi memApi) {
        return compressUsingBrotli(input, BrotliBinding.BROTLI_DEFAULT_QUALITY, BrotliBinding.BROTLI_DEFAULT_WINDOW_BITS, BrotliBinding.BROTLI_MODE_GENERIC, memApi);
    }

    public static MemorySegment compressUsingBrotli(MemorySegment input, int level, int windowSize, int mode, MemApi memApi) {
        if(!input.isNative()) {
            throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
        }
        try(Allocator allocator = Allocator.newDirectAllocator(memApi)) {
            level = level >= BrotliBinding.BROTLI_MIN_QUALITY && level <= BrotliBinding.BROTLI_NAX_QUALITY ? level : BrotliBinding.BROTLI_DEFAULT_QUALITY;
            windowSize = windowSize >= BrotliBinding.BROTLI_MIN_WINDOW_BITS && windowSize <= BrotliBinding.BROTLI_MAX_WINDOW_BITS ? windowSize : BrotliBinding.BROTLI_DEFAULT_WINDOW_BITS;
            mode = mode >= BrotliBinding.BROTLI_MODE_GENERIC && mode <= BrotliBinding.BROTLI_MODE_FONT ? mode : BrotliBinding.BROTLI_MODE_GENERIC;
            long inBytes = input.byteSize();
            long outBytes = BrotliBinding.encoderMaxCompressedSize(inBytes);
            if(outBytes <= 0L) {
                throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
            }
            MemorySegment outBytesPtr = allocator.allocate(ValueLayout.JAVA_LONG);
            NativeUtil.setLong(outBytesPtr, 0L, outBytes);
            MemorySegment out = allocator.allocate(ValueLayout.JAVA_BYTE, outBytes);
            if (BrotliBinding.encoderCompress(level, windowSize, mode, inBytes, input, outBytesPtr, out) != BrotliBinding.BROTLI_BOOL_TRUE) {
                throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
            }
            long compressed = NativeUtil.getLong(outBytesPtr, 0L);
            return NativeUtil.toHeap(out.asSlice(0L, compressed));
        }
    }

    public static MemorySegment compressUsingDeflate(MemorySegment input, MemApi memApi) {
        return compressUsingDeflate(input, DeflateBinding.LIBDEFLATE_DEFAULT_LEVEL, memApi);
    }

    /**
     *   When using libdeflate, level should between DeflateBinding.LIBDEFLATE_SLOWEST_LEVEL and DeflateBinding.LIBDEFLATE_FASTEST_LEVEL
     */
    public static MemorySegment compressUsingDeflate(MemorySegment input, int level, MemApi memApi) {
        if(!input.isNative()) {
            throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
        }
        MemorySegment compressor = DeflateBinding.allocCompressor(level >= DeflateBinding.LIBDEFLATE_FASTEST_LEVEL && level <= DeflateBinding.LIBDEFLATE_SLOWEST_LEVEL ? level : DeflateBinding.LIBDEFLATE_DEFAULT_LEVEL);
        try(Allocator allocator = Allocator.newDirectAllocator(memApi)) {
            long inBytes = input.byteSize();
            long outBytes = DeflateBinding.deflateCompressBound(compressor, inBytes);
            if(outBytes <= 0L) {
                throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
            }
            MemorySegment out = allocator.allocate(ValueLayout.JAVA_BYTE, outBytes);
            long compressed = DeflateBinding.deflateCompress(compressor, input, inBytes, out, outBytes);
            if(compressed <= 0L) {
                throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
            }
            return NativeUtil.toHeap(out.asSlice(0L, compressed));
        }finally {
            DeflateBinding.freeCompressor(compressor);
        }
    }

    public static MemorySegment compressUsingGzip(MemorySegment input, MemApi memApi) {
        return compressUsingGzip(input, DeflateBinding.LIBDEFLATE_DEFAULT_LEVEL, memApi);
    }

    public static MemorySegment compressUsingGzip(MemorySegment input, int level, MemApi memApi) {
        if(!input.isNative()) {
            throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
        }
        MemorySegment compressor = DeflateBinding.allocCompressor(level >= DeflateBinding.LIBDEFLATE_FASTEST_LEVEL && level <= DeflateBinding.LIBDEFLATE_SLOWEST_LEVEL ? level : DeflateBinding.LIBDEFLATE_DEFAULT_LEVEL);
        try(Allocator allocator = Allocator.newDirectAllocator(memApi)) {
            long inBytes = input.byteSize();
            long outBytes = DeflateBinding.gzipCompressBound(compressor, inBytes);
            if(outBytes <= 0L) {
                throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
            }
            MemorySegment out = allocator.allocate(ValueLayout.JAVA_BYTE, outBytes);
            long compressed = DeflateBinding.gzipCompress(compressor, input, inBytes, out, outBytes);
            if(compressed <= 0L) {
                throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
            }
            return NativeUtil.toHeap(out.asSlice(0L, compressed));
        }finally {
            DeflateBinding.freeCompressor(compressor);
        }
    }

    public static MemorySegment decompressUsingZstd(MemorySegment input, MemApi memApi) {
        if(!input.isNative()) {
            throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
        }
        long decompressedSize = ZstdBinding.zstdGetFrameContentSize(input, input.byteSize());
        if(decompressedSize <= 0L) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Unable to fetch ZSTD decompressed size");
        }
        MemorySegment out = memApi.allocateMemory(decompressedSize).reinterpret(decompressedSize);
        try{
            long len = ZstdBinding.zstdDecompress(out, out.byteSize(), input, input.byteSize());
            if(decompressedSize == len) {
                return NativeUtil.toHeap(out);
            }else {
                throw new FrameworkException(ExceptionType.COMPRESS, "Failed to decompress ZSTD frame, the uncompressed length is unknown");
            }
        } finally {
            memApi.freeMemory(out);
        }
    }

    public static MemorySegment decompressUsingBrotli(MemorySegment input, MemApi memApi) {
        return decompressUsingBrotli(input, Long.MIN_VALUE, memApi);
    }

    public static MemorySegment decompressUsingBrotli(MemorySegment input, long chunkSize, MemApi memApi) {
        if(!input.isNative()) {
            throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
        }
        MemorySegment state = BrotliBinding.decoderCreateInstance(MemorySegment.NULL, MemorySegment.NULL, MemorySegment.NULL);
        if(NativeUtil.checkNullPointer(state)) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Failed to initialize brotli decoder state");
        }
        try(Allocator allocator = Allocator.newDirectAllocator(memApi); WriteBuffer writeBuffer = WriteBuffer.newNativeWriteBuffer(memApi, CHUNK_SIZE)) {
            MemorySegment in = input;
            MemorySegment out = allocator.allocate(chunkSize < 0L ? CHUNK_SIZE : chunkSize);
            MemorySegment pInput = allocator.allocate(ValueLayout.ADDRESS);
            MemorySegment pOutput = allocator.allocate(ValueLayout.ADDRESS);
            MemorySegment inputSize = allocator.allocate(ValueLayout.JAVA_LONG);
            MemorySegment outputSize = allocator.allocate(ValueLayout.JAVA_LONG);
            for( ; ; ) {
                NativeUtil.setLong(inputSize, 0L, in.byteSize());
                NativeUtil.setLong(outputSize, 0L, out.byteSize());
                NativeUtil.setAddress(pInput, 0L, in);
                NativeUtil.setAddress(pOutput, 0L, out);
                int r = BrotliBinding.decoderDecompressStream(state, inputSize, pInput, outputSize, pOutput, MemorySegment.NULL);
                switch (r) {
                    case BrotliBinding.BROTLI_DECODER_RESULT_ERROR -> throw new FrameworkException(ExceptionType.COMPRESS, "Failed to perform brotli decompression, input is corrupted, or memory allocation failed");
                    case BrotliBinding.BROTLI_DECODER_RESULT_SUCCESS -> {
                        long unusedOutput = NativeUtil.getLong(outputSize, 0L);
                        MemorySegment data = unusedOutput == 0L ? out : out.asSlice(0L, out.byteSize() - unusedOutput);
                        if(writeBuffer.writeIndex() == 0L) {
                            return NativeUtil.toHeap(data);
                        } else {
                            writeBuffer.writeSegment(data);
                            return NativeUtil.toHeap(writeBuffer.asSegment());
                        }
                    }
                    case BrotliBinding.BROTLI_DECODER_RESULT_NEEDS_MORE_INPUT -> throw new FrameworkException(ExceptionType.COMPRESS, "Input not sufficient");
                    case BrotliBinding.BROTLI_DECODER_RESULT_NEEDS_MORE_OUTPUT -> {
                        long unusedInput = NativeUtil.getLong(inputSize, 0L);
                        long unusedOutput = NativeUtil.getLong(outputSize, 0L);
                        writeBuffer.writeSegment(unusedOutput == 0L ? out : out.asSlice(0L, out.byteSize() - unusedOutput));
                        if(unusedInput != 0L) {
                            in = in.asSlice(in.byteSize() - unusedInput, unusedInput);
                        }
                    }
                    default -> throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
                }
            }
        } finally {
            BrotliBinding.decoderDestroyInstance(state);
        }
    }

    public static MemorySegment decompressUsingDeflate(MemorySegment input, MemApi memApi) {
        return decompressUsingDeflate(input, Long.MIN_VALUE, memApi);
    }

    public static MemorySegment decompressUsingDeflate(MemorySegment input, long originalSize, MemApi memApi) {
        return decompressUsingLibDeflate(input, originalSize, memApi, (decompressor, in, out, actualOutBytes) -> DeflateBinding.deflateDecompress(decompressor, in, in.byteSize(), out, out.byteSize(), actualOutBytes));
    }

    public static MemorySegment decompressUsingGzip(MemorySegment input, MemApi memApi) {
        return decompressUsingGzip(input, Long.MIN_VALUE, memApi);
    }

    public static MemorySegment decompressUsingGzip(MemorySegment input, long originalSize, MemApi memApi) {
        return decompressUsingLibDeflate(input, originalSize, memApi, (decompressor, in, out, actualOutBytes) -> DeflateBinding.gzipDecompress(decompressor, in, in.byteSize(), out, out.byteSize(), actualOutBytes));
    }


    @FunctionalInterface
    interface LibDeflateDecompressor {
        int decompress(MemorySegment decompressor, MemorySegment in, MemorySegment out, MemorySegment actualOutBytes);
    }

    /**
     *   original size is the data-length of uncompressed segment, if unknown before, should be set as 0 or a negative number
     */
    private static MemorySegment decompressUsingLibDeflate(MemorySegment input, long originalSize, MemApi memApi, LibDeflateDecompressor libDeflateDecompressor) {
        if(!input.isNative()) {
            throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
        }
        if(originalSize > 0L) {
            return decompressUsingLibDeflateWithKnownSize(input, originalSize, memApi, libDeflateDecompressor);
        } else {
            return decompressUsingLibDeflateWithUnKnownSize(input, originalSize, memApi, libDeflateDecompressor);
        }
    }

    private static MemorySegment decompressUsingLibDeflateWithKnownSize(MemorySegment input, long originalSize, MemApi memApi, LibDeflateDecompressor libDeflateDecompressor) {
        MemorySegment decompressor = DeflateBinding.allocDecompressor();
        MemorySegment out = memApi.allocateMemory(originalSize).reinterpret(originalSize);
        try {
            switch (libDeflateDecompressor.decompress(decompressor, input, out, MemorySegment.NULL)) {
                case DeflateBinding.LIBDEFLATE_SUCCESS -> {
                    return NativeUtil.toHeap(out);
                }
                case DeflateBinding.LIBDEFLATE_BAD_DATA -> throw new FrameworkException(ExceptionType.COMPRESS, "Bad data");
                case DeflateBinding.LIBDEFLATE_SHORT_OUTPUT -> throw new FrameworkException(ExceptionType.COMPRESS, "Fewer originalSize expected");
                default -> throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
            }
        }finally {
            memApi.freeMemory(out);
            DeflateBinding.freeDecompressor(decompressor);
        }
    }

    private static MemorySegment decompressUsingLibDeflateWithUnKnownSize(MemorySegment input, long originalSize, MemApi memApi, LibDeflateDecompressor libDeflateDecompressor) {
        MemorySegment decompressor = DeflateBinding.allocDecompressor();
        long initialOutSize = input.byteSize() * ESTIMATE_RATIO; // we could just estimate a size, and grow it on demand
        MemorySegment out = memApi.allocateMemory(initialOutSize).reinterpret(initialOutSize);
        MemorySegment actualOutBytes = memApi.allocateMemory(ValueLayout.JAVA_LONG.byteSize()).reinterpret(ValueLayout.JAVA_LONG.byteSize());
        try {
            for( ; ; ) {
                switch (libDeflateDecompressor.decompress(decompressor, input, out, actualOutBytes)) {
                    case DeflateBinding.LIBDEFLATE_SUCCESS -> {
                        long written = actualOutBytes.get(ValueLayout.JAVA_LONG, 0L);
                        return NativeUtil.toHeap(out.byteSize() == written ? out : out.asSlice(0L, written));
                    }
                    case DeflateBinding.LIBDEFLATE_BAD_DATA -> throw new FrameworkException(ExceptionType.COMPRESS, "Bad data");
                    case DeflateBinding.LIBDEFLATE_INSUFFICIENT_SPACE -> {
                        long newOutSize = out.byteSize() + (out.byteSize() >> 1);
                        out = memApi.reallocateMemory(out, newOutSize).reinterpret(newOutSize);
                    }
                    default -> throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
                }
            }
        }finally {
            memApi.freeMemory(actualOutBytes);
            memApi.freeMemory(out);
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
