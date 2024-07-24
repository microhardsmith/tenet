package cc.zorcc.tenet.core.bindings;

import cc.zorcc.tenet.core.Constants;
import cc.zorcc.tenet.core.Dyn;
import cc.zorcc.tenet.core.ExceptionType;
import cc.zorcc.tenet.core.TenetException;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

/**
 *   Binding from tenet shared library, for log and rpmalloc
 */
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
        SymbolLookup tenet = Dyn.loadDynLibrary("libtenet");
        getStdoutHandle = Dyn.mh(tenet, "get_stdout", FunctionDescriptor.of(ValueLayout.ADDRESS), Linker.Option.critical(false));
        getStderrHandle = Dyn.mh(tenet, "get_stderr", FunctionDescriptor.of(ValueLayout.ADDRESS), Linker.Option.critical(false));
        getFbfHandle = Dyn.mh(tenet, "get_fbf", FunctionDescriptor.of(ValueLayout.JAVA_INT), Linker.Option.critical(false));
        getLbfHandle = Dyn.mh(tenet, "get_lbf", FunctionDescriptor.of(ValueLayout.JAVA_INT), Linker.Option.critical(false));
        getNbfHandle = Dyn.mh(tenet, "get_nbf", FunctionDescriptor.of(ValueLayout.JAVA_INT), Linker.Option.critical(false));
        rpInitializeHandle = Dyn.mh(tenet, "rp_initialize", FunctionDescriptor.of(ValueLayout.JAVA_INT), Linker.Option.critical(false));
        rpFinalizeHandle= Dyn.mh(tenet, "rp_finalize", FunctionDescriptor.ofVoid(), Linker.Option.critical(false));
        rpThreadInitializeHandle = Dyn.mh(tenet, "rp_tinitialize", FunctionDescriptor.ofVoid(), Linker.Option.critical(false));
        rpThreadFinalizeHandle = Dyn.mh(tenet, "rp_tfinalize", FunctionDescriptor.ofVoid(), Linker.Option.critical(false));
        rpMalloc = Dyn.mh(tenet, "rp_malloc", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), Linker.Option.critical(false));
        rpRealloc = Dyn.mh(tenet, "rp_realloc", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), Linker.Option.critical(false));
        rpFree = Dyn.mh(tenet, "rp_free", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS), Linker.Option.critical(false));
    }

    /**
     *   TenetBinding shouldn't be initialized
     */
    private TenetBinding() {
        throw new UnsupportedOperationException();
    }

    public static MemorySegment stdout() {
        try{
            return (MemorySegment) getStdoutHandle.invokeExact();
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static MemorySegment stderr() {
        try{
            return (MemorySegment) getStderrHandle.invokeExact();
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static int fbf() {
        try{
            return (int) getFbfHandle.invokeExact();
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static int lbf() {
        try{
            return (int) getLbfHandle.invokeExact();
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static int nbf() {
        try{
            return (int) getNbfHandle.invokeExact();
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static int rpInitialize() {
        try{
            return (int) rpInitializeHandle.invokeExact();
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static void rpFinalize() {
        try{
            rpFinalizeHandle.invokeExact();
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static void rpThreadInitialize() {
        try{
            rpThreadInitializeHandle.invokeExact();
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static void rpThreadFinalize() {
        try{
            rpThreadFinalizeHandle.invokeExact();
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static MemorySegment rpMalloc(long size) {
        try{
            return (MemorySegment) rpMalloc.invokeExact(size);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static MemorySegment rpRealloc(MemorySegment ptr, long size) {
        try{
            return (MemorySegment) rpRealloc.invokeExact(ptr, size);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static void rpFree(MemorySegment ptr) {
        try{
            rpFree.invokeExact(ptr);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }
}
