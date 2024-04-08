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
import java.util.function.Consumer;
import java.util.zip.*;

/**
 *   Compression and Decompression utilities for deflate, gzip, brotli and zstd
 *   All the methods require input to be native memory, the lifecycle of input memory are not managed, it's the caller's duty to keep them safe
 *   Don't try to expose the compressed or decompressed segment when using the consumerFunction, which would cause memory leak
 */
@SuppressWarnings("unused")
public final class CompressUtil {
    /**
     *   Default compression chunk size, grow on demand
     */
    private static final int CHUNK_SIZE = 4 * Constants.KB;

    /**
     *   Estimate compression ratio, grow on demand
     */
    private static final int ESTIMATE_RATIO = 3;

    private CompressUtil() {
        throw new UnsupportedOperationException();
    }

    public static void compressUsingZstd(MemorySegment input, MemApi memApi, Consumer<MemorySegment> consumerFunction) {
        compressUsingZstd(input, ZstdBinding.ZSTD_DEFAULT_LEVEL, memApi, consumerFunction);
    }

    public static void compressUsingZstd(MemorySegment input, int level, MemApi memApi, Consumer<MemorySegment> consumerFunction) {
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
            consumerFunction.accept(decompressedSize == out.byteSize() ? out : out.asSlice(0L, decompressedSize));
        } finally {
            memApi.freeMemory(out);
        }
    }

    public static void compressUsingBrotli(MemorySegment input, MemApi memApi, Consumer<MemorySegment> consumerFunction) {
        compressUsingBrotli(input, BrotliBinding.BROTLI_DEFAULT_QUALITY, BrotliBinding.BROTLI_DEFAULT_WINDOW_BITS, BrotliBinding.BROTLI_MODE_GENERIC, memApi, consumerFunction);
    }

    public static void compressUsingBrotli(MemorySegment input, int level, int windowSize, int mode, MemApi memApi, Consumer<MemorySegment> consumerFunction) {
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
            consumerFunction.accept(compressed == out.byteSize() ? out : out.asSlice(0L, compressed));
        }
    }

    public static void compressUsingDeflate(MemorySegment input, MemApi memApi, Consumer<MemorySegment> consumerFunction) {
        compressUsingDeflate(input, DeflateBinding.LIBDEFLATE_DEFAULT_LEVEL, memApi, consumerFunction);
    }

    public static void compressUsingDeflate(MemorySegment input, int level, MemApi memApi, Consumer<MemorySegment> consumerFunction) {
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
            consumerFunction.accept(compressed == out.byteSize() ? out : out.asSlice(0L, compressed));
        }finally {
            DeflateBinding.freeCompressor(compressor);
        }
    }

    public static void compressUsingGzip(MemorySegment input, MemApi memApi, Consumer<MemorySegment> consumerFunction) {
        compressUsingGzip(input, DeflateBinding.LIBDEFLATE_DEFAULT_LEVEL, memApi, consumerFunction);
    }

    public static void compressUsingGzip(MemorySegment input, int level, MemApi memApi, Consumer<MemorySegment> consumerFunction) {
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
            consumerFunction.accept(compressed == out.byteSize() ? out : out.asSlice(0L, compressed));
        }finally {
            DeflateBinding.freeCompressor(compressor);
        }
    }

    public static void decompressUsingZstd(MemorySegment input, MemApi memApi, Consumer<MemorySegment> consumerFunction) {
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
                consumerFunction.accept(out);
            }else {
                throw new FrameworkException(ExceptionType.COMPRESS, "Failed to decompress ZSTD frame, the uncompressed length is unknown");
            }
        } finally {
            memApi.freeMemory(out);
        }
    }

    public static void decompressUsingBrotli(MemorySegment input, MemApi memApi, Consumer<MemorySegment> consumerFunction) {
        decompressUsingBrotli(input, Long.MIN_VALUE, memApi, consumerFunction);
    }

    public static void decompressUsingBrotli(MemorySegment input, long chunkSize, MemApi memApi, Consumer<MemorySegment> consumerFunction) {
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
                            consumerFunction.accept(data);
                            return ;
                        } else {
                            writeBuffer.writeSegment(data);
                            consumerFunction.accept(writeBuffer.asSegment());
                            return ;
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

    public static void decompressUsingDeflate(MemorySegment input, MemApi memApi, Consumer<MemorySegment> consumerFunction) {
        decompressUsingDeflate(input, Long.MIN_VALUE, memApi, consumerFunction);
    }

    public static void decompressUsingDeflate(MemorySegment input, long originalSize, MemApi memApi, Consumer<MemorySegment> consumerFunction) {
        decompressUsingLibDeflate(input, originalSize, memApi,
                (decompressor, in, out, actualOutBytes) -> DeflateBinding.deflateDecompress(decompressor, in, in.byteSize(), out, out.byteSize(), actualOutBytes), consumerFunction);
    }

    public static void decompressUsingGzip(MemorySegment input, MemApi memApi, Consumer<MemorySegment> consumerFunction) {
        decompressUsingGzip(input, Long.MIN_VALUE, memApi, consumerFunction);
    }

    public static void decompressUsingGzip(MemorySegment input, long originalSize, MemApi memApi, Consumer<MemorySegment> consumerFunction) {
        decompressUsingLibDeflate(input, originalSize, memApi,
                (decompressor, in, out, actualOutBytes) -> DeflateBinding.gzipDecompress(decompressor, in, in.byteSize(), out, out.byteSize(), actualOutBytes), consumerFunction);
    }


    @FunctionalInterface
    interface LibDeflateDecompressor {
        int decompress(MemorySegment decompressor, MemorySegment in, MemorySegment out, MemorySegment actualOutBytes);
    }

    /**
     *   original size is the data-length of uncompressed segment, if unknown before, should be set as 0 or a negative number
     */
    private static void decompressUsingLibDeflate(MemorySegment input, long originalSize, MemApi memApi, LibDeflateDecompressor libDeflateDecompressor, Consumer<MemorySegment> consumerFunction) {
        if(!input.isNative()) {
            throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
        }
        if(originalSize > 0L) {
            decompressUsingLibDeflateWithKnownSize(input, originalSize, memApi, libDeflateDecompressor, consumerFunction);
        } else {
            decompressUsingLibDeflateWithUnKnownSize(input, originalSize, memApi, libDeflateDecompressor, consumerFunction);
        }
    }

    private static void decompressUsingLibDeflateWithKnownSize(MemorySegment input, long originalSize, MemApi memApi, LibDeflateDecompressor libDeflateDecompressor, Consumer<MemorySegment> consumerFunction) {
        MemorySegment decompressor = DeflateBinding.allocDecompressor();
        MemorySegment out = memApi.allocateMemory(originalSize).reinterpret(originalSize);
        try {
            switch (libDeflateDecompressor.decompress(decompressor, input, out, MemorySegment.NULL)) {
                case DeflateBinding.LIBDEFLATE_SUCCESS -> consumerFunction.accept(out);
                case DeflateBinding.LIBDEFLATE_BAD_DATA -> throw new FrameworkException(ExceptionType.COMPRESS, "Bad data");
                case DeflateBinding.LIBDEFLATE_SHORT_OUTPUT -> throw new FrameworkException(ExceptionType.COMPRESS, "Fewer originalSize expected");
                default -> throw new FrameworkException(ExceptionType.COMPRESS, Constants.UNREACHED);
            }
        }finally {
            memApi.freeMemory(out);
            DeflateBinding.freeDecompressor(decompressor);
        }
    }

    private static void decompressUsingLibDeflateWithUnKnownSize(MemorySegment input, long originalSize, MemApi memApi, LibDeflateDecompressor libDeflateDecompressor, Consumer<MemorySegment> consumerFunction) {
        MemorySegment decompressor = DeflateBinding.allocDecompressor();
        long initialOutSize = input.byteSize() * ESTIMATE_RATIO; // we could just estimate a size, and grow it on demand
        MemorySegment out = memApi.allocateMemory(initialOutSize).reinterpret(initialOutSize);
        MemorySegment actualOutBytes = memApi.allocateMemory(ValueLayout.JAVA_LONG.byteSize()).reinterpret(ValueLayout.JAVA_LONG.byteSize());
        try {
            for( ; ; ) {
                switch (libDeflateDecompressor.decompress(decompressor, input, out, actualOutBytes)) {
                    case DeflateBinding.LIBDEFLATE_SUCCESS -> {
                        long written = actualOutBytes.get(ValueLayout.JAVA_LONG, 0L);
                        consumerFunction.accept(out.byteSize() == written ? out : out.asSlice(0L, written));
                        return ;
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
            return writeBuffer.asByteArray();
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
            return writeBuffer.asByteArray();
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
            return writeBuffer.asByteArray();
        }catch (DataFormatException e) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Unable to perform deflate decompression", e);
        }finally {
            inflater.end();
        }
    }
}
