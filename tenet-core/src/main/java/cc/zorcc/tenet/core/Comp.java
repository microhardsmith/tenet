package cc.zorcc.tenet.core;

import cc.zorcc.tenet.core.bindings.BrotliBinding;
import cc.zorcc.tenet.core.bindings.DeflateBinding;
import cc.zorcc.tenet.core.bindings.ZstdBinding;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.ValueLayout;
import java.util.function.Consumer;

/**
 *   Comp is short for compression and decompression utility
 */
public final class Comp {
    /**
     *   Comp shouldn't be directly initialized
     */
    private Comp() {
        throw new UnsupportedOperationException();
    }

    /**
     *   Default compression chunk size, grow on demand
     */
    private static final int CHUNK_SIZE = 4 * Constants.KB;

    /**
     *   Estimate compression ratio, grow on demand
     */
    private static final int ESTIMATE_RATIO = 3;

    /**
     *   Maximum operations per compression or decompression
     */
    private static final int MAXIMUM_OPERATIONS = 8 * Constants.KB;

    /**
     * Compresses the input data using the Zstd algorithm with the default compression level.
     *
     * @param input the MemorySegment containing the data to compress, which must be native memory
     * @param mem the memory manager used to allocate and free memory
     * @param consumerFunction a Consumer function to process the compressed MemorySegment;
     *                         note: do not alter the lifecycle of MemorySegment within this function to avoid memory leaks
     * @throws TenetException if the input MemorySegment is not native or if compression fails
     */
    public static void compressUsingZstd(MemorySegment input, Mem mem, Consumer<MemorySegment> consumerFunction) {
        compressUsingZstd(input, ZstdBinding.ZSTD_DEFAULT_LEVEL, mem, consumerFunction);
    }

    /**
     * Compresses the input data using the Zstd algorithm with the specified compression level.
     *
     * @param input the MemorySegment containing the data to compress, which must be native memory
     * @param level the compression level; if outside valid range, the default level is used
     * @param mem the memory manager used to allocate and free memory
     * @param consumerFunction a Consumer function to process the compressed MemorySegment;
     *                         note: do not alter the lifecycle of MemorySegment within this function to avoid memory leaks
     * @throws TenetException if the input MemorySegment is not native or if compression fails
     */
    public static void compressUsingZstd(MemorySegment input, int level, Mem mem, Consumer<MemorySegment> consumerFunction) {
        Std.requireNonNull(input, () -> new TenetException(ExceptionType.COMPRESSION, Constants.UNREACHED));
        level = level < ZstdBinding.ZSTD_MIN_LEVEL || level > ZstdBinding.ZSTD_MAX_LEVEL ? ZstdBinding.ZSTD_DEFAULT_LEVEL : level;
        long upperBound = ZstdBinding.zstdCompressBound(input.byteSize());
        if(ZstdBinding.zstdIsError(upperBound) == 1) {
            throw new TenetException(ExceptionType.COMPRESSION, "Failed to fetch ZSTD compression upper bound");
        }
        MemorySegment out = mem.allocateMemory(upperBound).reinterpret(upperBound);
        try{
            long decompressedSize = ZstdBinding.zstdCompress(out, out.byteSize(), input, input.byteSize(), level);
            consumerFunction.accept(decompressedSize == out.byteSize() ? out : out.asSlice(0L, decompressedSize));
        } finally {
            mem.freeMemory(out);
        }
    }

    /**
     * Compresses the input data using the Brotli algorithm with default settings.
     *
     * @param input the MemorySegment containing the data to compress; must be non-null and native
     * @param mem the memory manager used to allocate and free memory
     * @param consumerFunction a Consumer function to process the compressed MemorySegment;
     *                         note: do not alter the lifecycle of MemorySegment within this function to avoid memory leaks
     * @throws TenetException if compression fails
     */
    public static void compressUsingBrotli(MemorySegment input, Mem mem, Consumer<MemorySegment> consumerFunction) {
        compressUsingBrotli(input, BrotliBinding.BROTLI_DEFAULT_QUALITY, BrotliBinding.BROTLI_DEFAULT_WINDOW_BITS, BrotliBinding.BROTLI_MODE_GENERIC, mem, consumerFunction);
    }

    /**
     * Compresses the input data using the Brotli algorithm with specified settings.
     *
     * @param input the MemorySegment containing the data to compress; must be non-null and native
     * @param level the compression quality level; if outside valid range, the default level is used
     * @param windowSize the window size for compression; if outside valid range, the default size is used
     * @param mode the compression mode; if outside valid range, the default mode is used
     * @param mem the memory manager used to allocate and free memory
     * @param consumerFunction a Consumer function to process the compressed MemorySegment;
     *                         note: do not alter the lifecycle of MemorySegment within this function to avoid memory leaks
     * @throws TenetException if compression fails
     */
    public static void compressUsingBrotli(MemorySegment input, int level, int windowSize, int mode, Mem mem, Consumer<MemorySegment> consumerFunction) {
        Std.requireNonNull(input, () -> new TenetException(ExceptionType.COMPRESSION, Constants.UNREACHED));
        try(Allocator allocator = Allocator.newDirectAllocator(mem)) {
            level = level >= BrotliBinding.BROTLI_MIN_QUALITY && level <= BrotliBinding.BROTLI_NAX_QUALITY ? level : BrotliBinding.BROTLI_DEFAULT_QUALITY;
            windowSize = windowSize >= BrotliBinding.BROTLI_MIN_WINDOW_BITS && windowSize <= BrotliBinding.BROTLI_MAX_WINDOW_BITS ? windowSize : BrotliBinding.BROTLI_DEFAULT_WINDOW_BITS;
            mode = mode == BrotliBinding.BROTLI_MODE_GENERIC || mode == BrotliBinding.BROTLI_MODE_TEXT || mode == BrotliBinding.BROTLI_MODE_FONT ? mode : BrotliBinding.BROTLI_MODE_GENERIC;
            long inBytes = input.byteSize();
            long outBytes = BrotliBinding.encoderMaxCompressedSize(inBytes);
            if(outBytes <= 0L) {
                throw new TenetException(ExceptionType.COMPRESSION, Constants.UNREACHED);
            }
            MemorySegment outBytesPtr = allocator.allocate(ValueLayout.JAVA_LONG);
            Std.setLong(outBytesPtr, 0L, outBytes);
            MemorySegment out = allocator.allocate(ValueLayout.JAVA_BYTE, outBytes);
            switch (BrotliBinding.encoderCompress(level, windowSize, mode, inBytes, input, outBytesPtr, out)) {
                case BrotliBinding.BROTLI_BOOL_TRUE -> {
                    long compressed = Std.getLong(outBytesPtr, 0L);
                    consumerFunction.accept(compressed == out.byteSize() ? out : out.asSlice(0L, compressed));
                }
                case BrotliBinding.BROTLI_BOOL_FALSE -> throw new TenetException(ExceptionType.COMPRESSION, "Output buffer is too small, which should never be reached");
                default -> throw new TenetException(ExceptionType.COMPRESSION, Constants.UNREACHED);
            }
        }
    }

    /**
     * Compresses the input data using the Deflate algorithm with default settings.
     *
     * @param input the MemorySegment containing the data to compress; must be non-null and native
     * @param memApi the memory manager used to allocate and free memory
     * @param consumerFunction a Consumer function to process the compressed MemorySegment;
     *                         note: do not alter the lifecycle of MemorySegment within this function to avoid memory leaks
     * @throws TenetException if compression fails
     */
    public static void compressUsingDeflate(MemorySegment input, Mem memApi, Consumer<MemorySegment> consumerFunction) {
        compressUsingDeflate(input, DeflateBinding.LIBDEFLATE_DEFAULT_LEVEL, memApi, consumerFunction);
    }

    /**
     * Compresses the input data using the Deflate algorithm with specified settings.
     *
     * @param input the MemorySegment containing the data to compress; must be non-null and native
     * @param level the compression level; if outside valid range, the default level is used
     * @param memApi the memory manager used to allocate and free memory
     * @param consumerFunction a Consumer function to process the compressed MemorySegment;
     *                         note: do not alter the lifecycle of MemorySegment within this function to avoid memory leaks
     * @throws TenetException if compression fails
     */
    public static void compressUsingDeflate(MemorySegment input, int level, Mem memApi, Consumer<MemorySegment> consumerFunction) {
        Std.requireNonNull(input, () -> new TenetException(ExceptionType.COMPRESSION, Constants.UNREACHED));
        MemorySegment compressor = DeflateBinding.allocCompressor(level >= DeflateBinding.LIBDEFLATE_FASTEST_LEVEL && level <= DeflateBinding.LIBDEFLATE_SLOWEST_LEVEL ? level : DeflateBinding.LIBDEFLATE_DEFAULT_LEVEL);
        try(Allocator allocator = Allocator.newDirectAllocator(memApi)) {
            long inBytes = input.byteSize();
            long outBytes = DeflateBinding.deflateCompressBound(compressor, inBytes);
            if(outBytes <= 0L) {
                throw new TenetException(ExceptionType.COMPRESSION, Constants.UNREACHED);
            }
            MemorySegment out = allocator.allocate(ValueLayout.JAVA_BYTE, outBytes);
            long compressed = DeflateBinding.deflateCompress(compressor, input, inBytes, out, outBytes);
            if(compressed <= 0L) {
                throw new TenetException(ExceptionType.COMPRESSION, Constants.UNREACHED);
            }
            consumerFunction.accept(compressed == out.byteSize() ? out : out.asSlice(0L, compressed));
        }finally {
            DeflateBinding.freeCompressor(compressor);
        }
    }

    /**
     * Compresses the input data using the Gzip algorithm with default settings.
     *
     * @param input the MemorySegment containing the data to compress; must be non-null and native
     * @param memApi the memory manager used to allocate and free memory
     * @param consumerFunction a Consumer function to process the compressed MemorySegment;
     *                         note: do not alter the lifecycle of MemorySegment within this function to avoid memory leaks
     * @throws TenetException if compression fails
     */
    public static void compressUsingGzip(MemorySegment input, Mem memApi, Consumer<MemorySegment> consumerFunction) {
        compressUsingGzip(input, DeflateBinding.LIBDEFLATE_DEFAULT_LEVEL, memApi, consumerFunction);
    }

    /**
     * Compresses the input data using the Gzip algorithm with specified settings.
     *
     * @param input the MemorySegment containing the data to compress; must be non-null and native
     * @param level the compression level; if outside valid range, the default level is used
     * @param memApi the memory manager used to allocate and free memory
     * @param consumerFunction a Consumer function to process the compressed MemorySegment;
     *                         note: do not alter the lifecycle of MemorySegment within this function to avoid memory leaks
     * @throws TenetException if compression fails
     */
    public static void compressUsingGzip(MemorySegment input, int level, Mem memApi, Consumer<MemorySegment> consumerFunction) {
        Std.requireNonNull(input, () -> new TenetException(ExceptionType.COMPRESSION, Constants.UNREACHED));
        MemorySegment compressor = DeflateBinding.allocCompressor(level >= DeflateBinding.LIBDEFLATE_FASTEST_LEVEL && level <= DeflateBinding.LIBDEFLATE_SLOWEST_LEVEL ? level : DeflateBinding.LIBDEFLATE_DEFAULT_LEVEL);
        try(Allocator allocator = Allocator.newDirectAllocator(memApi)) {
            long inBytes = input.byteSize();
            long outBytes = DeflateBinding.gzipCompressBound(compressor, inBytes);
            if(outBytes <= 0L) {
                throw new TenetException(ExceptionType.COMPRESSION, Constants.UNREACHED);
            }
            MemorySegment out = allocator.allocate(ValueLayout.JAVA_BYTE, outBytes);
            long compressed = DeflateBinding.gzipCompress(compressor, input, inBytes, out, outBytes);
            if(compressed <= 0L) {
                throw new TenetException(ExceptionType.COMPRESSION, Constants.UNREACHED);
            }
            consumerFunction.accept(compressed == out.byteSize() ? out : out.asSlice(0L, compressed));
        }finally {
            DeflateBinding.freeCompressor(compressor);
        }
    }

    /**
     * Decompresses the input data using the Zstandard (Zstd) algorithm.
     *
     * @param input the MemorySegment containing the compressed data; must be non-null
     * @param mem the memory manager used to allocate and free memory
     * @param consumerFunction a Consumer function to process the decompressed MemorySegment;
     *                         this function should handle the MemorySegment appropriately and must not
     *                         alter the lifecycle of the MemorySegment to avoid memory leaks
     * @throws TenetException if decompression fails or the decompressed size cannot be determined
     */
    public static void decompressUsingZstd(MemorySegment input, Mem mem, Consumer<MemorySegment> consumerFunction) {
        Std.requireNonNull(input, () -> new TenetException(ExceptionType.COMPRESSION, Constants.UNREACHED));
        long decompressedSize = ZstdBinding.zstdGetFrameContentSize(input, input.byteSize());
        if(decompressedSize <= 0L) {
            throw new TenetException(ExceptionType.COMPRESSION, "Unable to fetch ZSTD decompressed size");
        }
        MemorySegment out = mem.allocateMemory(decompressedSize).reinterpret(decompressedSize);
        try{
            long len = ZstdBinding.zstdDecompress(out, out.byteSize(), input, input.byteSize());
            if(decompressedSize == len) {
                consumerFunction.accept(out);
            }else {
                throw new TenetException(ExceptionType.COMPRESSION, "Failed to decompress ZSTD frame, the uncompressed length is unknown");
            }
        } finally {
            mem.freeMemory(out);
        }
    }

    private static final MemoryLayout brotliLayout = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("pInput"),
            ValueLayout.ADDRESS.withName("pOutput"),
            ValueLayout.JAVA_LONG.withName("inputSize"),
            ValueLayout.JAVA_LONG.withName("outputSize")
    );
    private static final long pInputOffset = brotliLayout.byteOffset(MemoryLayout.PathElement.groupElement("pInput"));
    private static final long pOutputOffset = brotliLayout.byteOffset(MemoryLayout.PathElement.groupElement("pOutput"));
    private static final long inputSizeOffset = brotliLayout.byteOffset(MemoryLayout.PathElement.groupElement("inputSize"));
    private static final long outputSizeOffset = brotliLayout.byteOffset(MemoryLayout.PathElement.groupElement("outputSize"));

    /**
     * Decompresses the input data using the Brotli algorithm with an unspecified chunk size.
     *
     * @param input the MemorySegment containing the compressed data; must be non-null
     * @param mem the memory manager used to allocate and free memory
     * @param consumerFunction a Consumer function to process the decompressed MemorySegment;
     *                         this function should handle the MemorySegment appropriately and must not
     *                         alter the lifecycle of the MemorySegment to avoid memory leaks
     * @throws TenetException if decompression fails
     */
    public static void decompressUsingBrotli(MemorySegment input, Mem mem, Consumer<MemorySegment> consumerFunction) {
        decompressUsingBrotli(input, Long.MIN_VALUE, mem, consumerFunction);
    }

    /**
     * Decompresses the input data using the Brotli algorithm.
     *
     * @param input the MemorySegment containing the compressed data; must be non-null
     * @param chunkSize the size of chunks to be used for decompression; if Long.MIN_VALUE, an appropriate chunk size will be determined internally
     * @param mem the memory manager used to allocate and free memory
     * @param consumerFunction a Consumer function to process the decompressed MemorySegment;
     *                         this function should handle the MemorySegment appropriately and must not
     *                         alter the lifecycle of the MemorySegment to avoid memory leaks
     * @throws TenetException if decompression fails
     */
    public static void decompressUsingBrotli(MemorySegment input, long chunkSize, Mem mem, Consumer<MemorySegment> consumerFunction) {
        Std.requireNonNull(input, () -> new TenetException(ExceptionType.COMPRESSION, Constants.UNREACHED));
        MemorySegment state = Std.requireNonNull(BrotliBinding.decoderCreateInstance(MemorySegment.NULL, MemorySegment.NULL, MemorySegment.NULL), () -> new TenetException(ExceptionType.COMPRESSION, "Failed to initialize brotli decoder state"));
        try(Allocator allocator = Allocator.newDirectAllocator(mem); WriteBuffer writeBuffer = WriteBuffer.newNativeWriteBuffer(mem, CHUNK_SIZE)) {
            MemorySegment in = input;
            MemorySegment out = allocator.allocate(chunkSize < 0L ? CHUNK_SIZE : chunkSize);
            MemorySegment base = allocator.allocate(brotliLayout);
            MemorySegment pInput = base.asSlice(pInputOffset, 0L);
            MemorySegment pOutput = base.asSlice(pOutputOffset, 0L);
            MemorySegment inputSize = base.asSlice(inputSizeOffset, 0L);
            MemorySegment outputSize = base.asSlice(outputSizeOffset, 0L);
            for(int i = 0; i < MAXIMUM_OPERATIONS; i++) {
                Std.setAddress(base, pInputOffset, in);
                Std.setAddress(base, pOutputOffset, out);
                Std.setLong(base, inputSizeOffset, in.byteSize());
                Std.setLong(base, outputSizeOffset, out.byteSize());
                switch (BrotliBinding.decoderDecompressStream(state, inputSize, pInput, outputSize, pOutput, MemorySegment.NULL)) {
                    case BrotliBinding.BROTLI_DECODER_RESULT_ERROR -> throw new TenetException(ExceptionType.COMPRESSION, "Failed to perform brotli decompression, input is corrupted, or memory allocation failed");
                    case BrotliBinding.BROTLI_DECODER_RESULT_SUCCESS -> {
                        long unusedOutput = Std.getLong(base, outputSizeOffset);
                        MemorySegment data = unusedOutput == 0L ? out : out.asSlice(0L, out.byteSize() - unusedOutput);
                        if(writeBuffer.writeIndex() == 0L) {
                            consumerFunction.accept(data);
                        } else {
                            writeBuffer.writeSegment(data);
                            consumerFunction.accept(writeBuffer.content());
                        }
                        return ;
                    }
                    case BrotliBinding.BROTLI_DECODER_RESULT_NEEDS_MORE_INPUT -> throw new TenetException(ExceptionType.COMPRESSION, "Input not sufficient");
                    case BrotliBinding.BROTLI_DECODER_RESULT_NEEDS_MORE_OUTPUT -> {
                        long unusedInput = Std.getLong(base, inputSizeOffset);
                        long unusedOutput = Std.getLong(base, outputSizeOffset);
                        writeBuffer.writeSegment(unusedOutput == 0L ? out : out.asSlice(0L, out.byteSize() - unusedOutput));
                        if(unusedInput != 0L) {
                            in = in.asSlice(in.byteSize() - unusedInput, unusedInput);
                        }
                    }
                    default -> throw new TenetException(ExceptionType.COMPRESSION, Constants.UNREACHED);
                }
            }
        } finally {
            BrotliBinding.decoderDestroyInstance(state);
        }
    }

    /**
     * A sealed interface for decompressors using the LibDeflate library.
     * This interface defines a method for decompressing data and includes
     * specific implementations for GZIP and DEFLATE compression formats.
     */
    private sealed interface LibDeflateDecompressor {
        /**
         * DeflateDecompressor singleton instance.
         */
        LibDeflateDecompressor DEFLATE = new DeflateDecompressor();

        /**
         * GzipDecompressor singleton instance.
         */
        LibDeflateDecompressor GZIP = new GzipDecompressor();

        /**
         * Decompresses the input data.
         *
         * @param decompressor the MemorySegment representing the decompressor state; must be non-null
         * @param in the MemorySegment containing the compressed input data; must be non-null
         * @param out the MemorySegment where the decompressed data will be written; must be non-null
         * @param actualOutBytes the MemorySegment where the actual number of bytes written to
         * @return an integer indicating the status of the decompression operation
         */
        int decompress(MemorySegment decompressor, MemorySegment in, MemorySegment out, MemorySegment actualOutBytes);

        record DeflateDecompressor() implements LibDeflateDecompressor {
            @Override
            public int decompress(MemorySegment decompressor, MemorySegment in, MemorySegment out, MemorySegment actualOutBytes) {
                return DeflateBinding.deflateDecompress(decompressor, in, in.byteSize(), out, out.byteSize(), actualOutBytes);
            }
        }

        record GzipDecompressor() implements LibDeflateDecompressor {
            @Override
            public int decompress(MemorySegment decompressor, MemorySegment in, MemorySegment out, MemorySegment actualOutBytes) {
                return DeflateBinding.gzipDecompress(decompressor, in, in.byteSize(), out, out.byteSize(), actualOutBytes);
            }
        }
    }

    /**
     * Decompresses the input data using the LibDeflate library and deflate algorithm with a known original size.
     *
     * @param input the MemorySegment containing the compressed input data; must be native and non-null
     * @param originalSize the original size of the uncompressed data, must be positive
     * @param mem an instance of Mem to manage memory allocation
     * @param consumerFunction a Consumer function that processes the decompressed MemorySegment
     * @throws TenetException if the input MemorySegment is invalid
     */
    public static void decompressUsingDeflate(MemorySegment input, long originalSize, Mem mem, Consumer<MemorySegment> consumerFunction) {
        Std.requireNonNull(input, () -> new TenetException(ExceptionType.COMPRESSION, Constants.UNREACHED));
        decompressUsingLibDeflateWithKnownSize(input, originalSize, mem, LibDeflateDecompressor.DEFLATE, consumerFunction);
    }

    /**
     * Decompresses the input data using the LibDeflate library and deflate algorithm with unknown original size.
     *
     * @param input the MemorySegment containing the compressed input data; must be native and non-null
     * @param mem an instance of Mem to manage memory allocation
     * @param consumerFunction a Consumer function that processes the decompressed MemorySegment
     * @throws TenetException if the input MemorySegment is invalid
     */
    public static void decompressUsingDeflate(MemorySegment input, Mem mem, Consumer<MemorySegment> consumerFunction) {
        Std.requireNonNull(input, () -> new TenetException(ExceptionType.COMPRESSION, Constants.UNREACHED));
        decompressUsingLibDeflateWithUnKnownSize(input, mem, LibDeflateDecompressor.DEFLATE, consumerFunction);
    }

    /**
     * Decompresses the input data using the LibDeflate library and gzip algorithm with a known original size.
     *
     * @param input the MemorySegment containing the compressed input data; must be native and non-null
     * @param originalSize the original size of the uncompressed data, must be positive
     * @param mem an instance of Mem to manage memory allocation
     * @param consumerFunction a Consumer function that processes the decompressed MemorySegment
     * @throws TenetException if the input MemorySegment is invalid
     */
    public static void decompressUsingGzip(MemorySegment input, long originalSize, Mem mem, Consumer<MemorySegment> consumerFunction) {
        Std.requireNonNull(input, () -> new TenetException(ExceptionType.COMPRESSION, Constants.UNREACHED));
        decompressUsingLibDeflateWithKnownSize(input, originalSize, mem, LibDeflateDecompressor.GZIP, consumerFunction);
    }

    /**
     * Decompresses the input data using the LibDeflate library and gzip algorithm with unknown original size.
     *
     * @param input the MemorySegment containing the compressed input data; must be native and non-null
     * @param mem an instance of Mem to manage memory allocation
     * @param consumerFunction a Consumer function that processes the decompressed MemorySegment
     * @throws TenetException if the input MemorySegment is invalid
     */
    public static void decompressUsingGzip(MemorySegment input, Mem mem, Consumer<MemorySegment> consumerFunction) {
        Std.requireNonNull(input, () -> new TenetException(ExceptionType.COMPRESSION, Constants.UNREACHED));
        decompressUsingLibDeflateWithUnKnownSize(input, mem, LibDeflateDecompressor.GZIP, consumerFunction);
    }

    /**
     * Decompresses the input data using the LibDeflate library with a known original size.
     *
     * @param input the MemorySegment containing the compressed input data; must be non-null
     * @param originalSize the original size of the uncompressed data; must be greater than 0
     * @param mem an instance of Mem to manage memory allocation
     * @param libDeflateDecompressor the decompressor to use for the decompression process
     * @param consumerFunction a Consumer that processes the decompressed MemorySegment
     * @throws TenetException if the original size is less than or equal to 0, or if decompression fails
     */
    private static void decompressUsingLibDeflateWithKnownSize(MemorySegment input, long originalSize, Mem mem, LibDeflateDecompressor libDeflateDecompressor, Consumer<MemorySegment> consumerFunction) {
        if(originalSize <= 0L) {
            throw new TenetException(ExceptionType.COMPRESSION, Constants.UNREACHED);
        }
        MemorySegment decompressor = DeflateBinding.allocDecompressor();
        MemorySegment out = mem.allocateMemory(originalSize).reinterpret(originalSize);
        try {
            switch (libDeflateDecompressor.decompress(decompressor, input, out, MemorySegment.NULL)) {
                case DeflateBinding.LIBDEFLATE_SUCCESS -> consumerFunction.accept(out);
                case DeflateBinding.LIBDEFLATE_BAD_DATA -> throw new TenetException(ExceptionType.COMPRESSION, "Bad data");
                case DeflateBinding.LIBDEFLATE_SHORT_OUTPUT -> throw new TenetException(ExceptionType.COMPRESSION, "Fewer originalSize expected");
                default -> throw new TenetException(ExceptionType.COMPRESSION, Constants.UNREACHED);
            }
        }finally {
            mem.freeMemory(out);
            DeflateBinding.freeDecompressor(decompressor);
        }
    }

    /**
     * Decompresses the input data using the LibDeflate library without a known original size.
     *
     * @param input the MemorySegment containing the compressed input data; must be non-null
     * @param mem an instance of Mem to manage memory allocation
     * @param libDeflateDecompressor the decompressor to use for the decompression process
     * @param consumerFunction a Consumer that processes the decompressed MemorySegment
     * @throws TenetException if the original size is less than or equal to 0, or if decompression fails
     */
    private static void decompressUsingLibDeflateWithUnKnownSize(MemorySegment input, Mem mem, LibDeflateDecompressor libDeflateDecompressor, Consumer<MemorySegment> consumerFunction) {
        MemorySegment decompressor = DeflateBinding.allocDecompressor();
        long initialOutSize = input.byteSize() * ESTIMATE_RATIO; // we could just estimate a size, and grow it on demand
        MemorySegment out = mem.allocateMemory(initialOutSize).reinterpret(initialOutSize);
        MemorySegment actualOutBytes = mem.allocateMemory(ValueLayout.JAVA_LONG.byteSize()).reinterpret(ValueLayout.JAVA_LONG.byteSize());
        try {
            for(int i = 0; i < MAXIMUM_OPERATIONS; i++) {
                switch (libDeflateDecompressor.decompress(decompressor, input, out, actualOutBytes)) {
                    case DeflateBinding.LIBDEFLATE_SUCCESS -> {
                        long written = actualOutBytes.get(ValueLayout.JAVA_LONG, 0L);
                        consumerFunction.accept(out.byteSize() == written ? out : out.asSlice(0L, written));
                        return ;
                    }
                    case DeflateBinding.LIBDEFLATE_BAD_DATA -> throw new TenetException(ExceptionType.COMPRESSION, "Bad data");
                    case DeflateBinding.LIBDEFLATE_INSUFFICIENT_SPACE -> {
                        long newOutSize = out.byteSize() + (out.byteSize() >> 1);
                        out = mem.reallocateMemory(out, newOutSize).reinterpret(newOutSize);
                    }
                    default -> throw new TenetException(ExceptionType.COMPRESSION, Constants.UNREACHED);
                }
            }
        }finally {
            mem.freeMemory(actualOutBytes);
            mem.freeMemory(out);
            DeflateBinding.freeDecompressor(decompressor);
        }
    }
}
