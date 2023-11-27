package cn.zorcc.common.bindings;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

@SuppressWarnings("unused")
public final class TenetBinding {
    private static final MethodHandle getStdoutHandle;
    private static final MethodHandle getStderrHandle;
    private static final MethodHandle getFbfHandle;
    private static final MethodHandle getLbfHandle;
    private static final MethodHandle getNbfHandle;

    static {
        SymbolLookup symbolLookup = NativeUtil.loadLibrary(Constants.TENET);
        getStdoutHandle = NativeUtil.methodHandle(symbolLookup, "get_stdout", FunctionDescriptor.of(ValueLayout.ADDRESS));
        getStderrHandle = NativeUtil.methodHandle(symbolLookup, "get_stderr", FunctionDescriptor.of(ValueLayout.ADDRESS));
        getFbfHandle = NativeUtil.methodHandle(symbolLookup, "get_fbf", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        getLbfHandle = NativeUtil.methodHandle(symbolLookup, "get_lbf", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        getNbfHandle = NativeUtil.methodHandle(symbolLookup, "get_nbf", FunctionDescriptor.of(ValueLayout.JAVA_INT));
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
}
