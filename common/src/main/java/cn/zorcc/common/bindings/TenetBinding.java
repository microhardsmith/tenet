package cn.zorcc.common.bindings;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

/**
 *   Tenet shared bindings for all platforms
 */
@SuppressWarnings("unused")
public final class TenetBinding {
    private static final MethodHandle getStdoutHandle;
    private static final MethodHandle getStderrHandle;
    private static final MethodHandle getFbfHandle;
    private static final MethodHandle getLbfHandle;
    private static final MethodHandle getNbfHandle;
    private static final MethodHandle rpInitializeHandle;
    private static final MethodHandle rpFinalizeHandle;
    private static final MethodHandle rpThreadInitializeHandle;
    private static final MethodHandle rpThreadFinalizeHandle;
    private static final MethodHandle rpMalloc;
    private static final MethodHandle rpRealloc;
    private static final MethodHandle rpFree;

    static {
        SymbolLookup symbolLookup = NativeUtil.loadLibrary(Constants.TENET);
        getStdoutHandle = NativeUtil.methodHandle(symbolLookup, "get_stdout", FunctionDescriptor.of(ValueLayout.ADDRESS), Linker.Option.critical(false));
        getStderrHandle = NativeUtil.methodHandle(symbolLookup, "get_stderr", FunctionDescriptor.of(ValueLayout.ADDRESS), Linker.Option.critical(false));
        getFbfHandle = NativeUtil.methodHandle(symbolLookup, "get_fbf", FunctionDescriptor.of(ValueLayout.JAVA_INT), Linker.Option.critical(false));
        getLbfHandle = NativeUtil.methodHandle(symbolLookup, "get_lbf", FunctionDescriptor.of(ValueLayout.JAVA_INT), Linker.Option.critical(false));
        getNbfHandle = NativeUtil.methodHandle(symbolLookup, "get_nbf", FunctionDescriptor.of(ValueLayout.JAVA_INT), Linker.Option.critical(false));
        rpInitializeHandle = NativeUtil.methodHandle(symbolLookup, "rp_initialize", FunctionDescriptor.of(ValueLayout.JAVA_INT), Linker.Option.critical(false));
        rpFinalizeHandle= NativeUtil.methodHandle(symbolLookup, "rp_finalize", FunctionDescriptor.ofVoid(), Linker.Option.critical(false));
        rpThreadInitializeHandle = NativeUtil.methodHandle(symbolLookup, "rp_tinitialize", FunctionDescriptor.ofVoid(), Linker.Option.critical(false));
        rpThreadFinalizeHandle = NativeUtil.methodHandle(symbolLookup, "rp_tfinalize", FunctionDescriptor.ofVoid(), Linker.Option.critical(false));
        rpMalloc = NativeUtil.methodHandle(symbolLookup, "rp_malloc", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), Linker.Option.critical(false));
        rpRealloc = NativeUtil.methodHandle(symbolLookup, "rp_realloc", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), Linker.Option.critical(false));
        rpFree = NativeUtil.methodHandle(symbolLookup, "rp_free", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS), Linker.Option.critical(false));
    }

    private TenetBinding() {
        throw new UnsupportedOperationException();
    }

    public static MemorySegment stdout() {
        try{
            return (MemorySegment) getStdoutHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static MemorySegment stderr() {
        try{
            return (MemorySegment) getStderrHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int fbf() {
        try{
            return (int) getFbfHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int lbf() {
        try{
            return (int) getLbfHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int nbf() {
        try{
            return (int) getNbfHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int rpInitialize() {
        try{
            return (int) rpInitializeHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static void rpFinalize() {
        try{
            rpFinalizeHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static void rpThreadInitialize() {
        try{
            rpThreadInitializeHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static void rpThreadFinalize() {
        try{
            rpThreadFinalizeHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static MemorySegment rpMalloc(long size) {
        try{
            return (MemorySegment) rpMalloc.invokeExact(size);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static MemorySegment rpRealloc(MemorySegment ptr, long size) {
        try{
            return (MemorySegment) rpRealloc.invokeExact(ptr, size);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static void rpFree(MemorySegment ptr) {
        try{
            rpFree.invokeExact(ptr);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

}
