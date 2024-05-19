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
 *   Adding support for Brotli compression/decompression using <a href="https://github.com/google/brotli"> brotli </a>
 */
public final class BrotliBinding {
    /**
     *   BROTLI_BOOL definition
     */
    public static final int BROTLI_BOOL_FALSE = 0;
    public static final int BROTLI_BOOL_TRUE = 1;
    /**
     *   lgwin range
     */
    public static final int BROTLI_MIN_WINDOW_BITS = 10;
    public static final int BROTLI_DEFAULT_WINDOW_BITS = 22;
    public static final int BROTLI_MAX_WINDOW_BITS = 24;
    /**
     *   BrotliEncoderMode
     */
    public static final int BROTLI_MODE_GENERIC = 0;
    public static final int BROTLI_MODE_TEXT = 1;
    public static final int BROTLI_MODE_FONT = 2;
    /**
     *   BROTLI_PARAM_QUALITY
     */
    public static final int BROTLI_MIN_QUALITY = 0;
    public static final int BROTLI_DEFAULT_QUALITY = 11;
    public static final int BROTLI_NAX_QUALITY = 11;
    /**
     *   BROTLI_DECODER_RESULT
     */
    public static final int BROTLI_DECODER_RESULT_ERROR = 0;
    public static final int BROTLI_DECODER_RESULT_SUCCESS = 1;
    public static final int BROTLI_DECODER_RESULT_NEEDS_MORE_INPUT = 2;
    public static final int BROTLI_DECODER_RESULT_NEEDS_MORE_OUTPUT = 3;


    private static final MethodHandle encoderMaxCompressedSize;
    private static final MethodHandle encoderCompress;
    private static final MethodHandle decoderCreateInstance;
    private static final MethodHandle decoderDestroyInstance;
    private static final MethodHandle decoderDecompressStream;

    static {
        SymbolLookup _ = Dyn.loadDynLibrary("libbrotlicommon");
        SymbolLookup brotliEnc = Dyn.loadDynLibrary("libbrotlienc");
        SymbolLookup brotliDec = Dyn.loadDynLibrary("libbrotlidec");
        encoderMaxCompressedSize = Dyn.mh(brotliEnc, "BrotliEncoderMaxCompressedSize",
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG));
        encoderCompress = Dyn.mh(brotliEnc, "BrotliEncoderCompress",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        decoderCreateInstance = Dyn.mh(brotliDec, "BrotliDecoderCreateInstance",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        decoderDestroyInstance = Dyn.mh(brotliDec, "BrotliDecoderDestroyInstance",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        decoderDecompressStream = Dyn.mh(brotliDec, "BrotliDecoderDecompressStream",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
    }

    /**
     *   BrotliBinding shouldn't be initialized
     */
    private BrotliBinding() {
        throw new UnsupportedOperationException();
    }

    public static long encoderMaxCompressedSize(long inputSize) {
        try {
            return (long) encoderMaxCompressedSize.invokeExact(inputSize);
        } catch (Throwable e) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static int encoderCompress(int quality, int lgwin, int mode, long inputSize, MemorySegment input, MemorySegment outputSizePtr, MemorySegment output) {
        try {
            return (int) encoderCompress.invokeExact(quality, lgwin, mode, inputSize, input, outputSizePtr, output);
        } catch (Throwable e) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static MemorySegment decoderCreateInstance(MemorySegment allocFunc, MemorySegment freeFunc, MemorySegment opaque) {
        try {
            return (MemorySegment) decoderCreateInstance.invokeExact(allocFunc, freeFunc, opaque);
        } catch (Throwable e) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static void decoderDestroyInstance(MemorySegment state) {
        try {
            decoderDestroyInstance.invokeExact(state);
        } catch (Throwable e) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static int decoderDecompressStream(MemorySegment state, MemorySegment availableIn, MemorySegment nextIn, MemorySegment availableOut, MemorySegment nextOut, MemorySegment totalOut) {
        try {
            return (int) decoderDecompressStream.invokeExact(state, availableIn, nextIn, availableOut, nextOut, totalOut);
        } catch (Throwable e) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }
}
