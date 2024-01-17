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

public final class DuckdbBinding {
    public static final int DUCKDB_SUCCESS = 0;
    public static final int DUCKDB_ERROR = 1;
    private static final MethodHandle duckdbOpen;
    private static final MethodHandle duckdbOpenExt;
    private static final MethodHandle duckdbClose;
    private static final MethodHandle duckdbConnect;
    private static final MethodHandle duckdbDisconnect;
    private static final MethodHandle duckdbConfigCount;
    private static final MethodHandle duckdbGetConfigFlag;
    private static final MethodHandle duckdbCreateConfig;
    private static final MethodHandle duckdbSetConfig;
    private static final MethodHandle duckdbQuery;
    private static final MethodHandle duckdbDestroyResult;

    static {
        SymbolLookup symbolLookup = NativeUtil.loadLibrary(Constants.DUCKDB);
        duckdbOpen = NativeUtil.methodHandle(symbolLookup, "duckdb_open", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        duckdbOpenExt = NativeUtil.methodHandle(symbolLookup, "duckdb_open_ext", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        duckdbClose = NativeUtil.methodHandle(symbolLookup, "duckdb_close", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        duckdbConnect = NativeUtil.methodHandle(symbolLookup, "duckdb_connect", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        duckdbDisconnect = NativeUtil.methodHandle(symbolLookup, "duckdb_disconnect", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        duckdbConfigCount = NativeUtil.methodHandle(symbolLookup, "duckdb_config_count", FunctionDescriptor.of(ValueLayout.JAVA_LONG));
        duckdbGetConfigFlag = NativeUtil.methodHandle(symbolLookup, "duckdb_get_config_flag", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        duckdbCreateConfig = NativeUtil.methodHandle(symbolLookup, "duckdb_create_config", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        duckdbSetConfig = NativeUtil.methodHandle(symbolLookup, "duckdb_set_config", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        duckdbQuery = NativeUtil.methodHandle(symbolLookup, "duckdb_query", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        duckdbDestroyResult = NativeUtil.methodHandle(symbolLookup, "duckdb_destroy_result", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
    }

    private DuckdbBinding() {
        throw new UnsupportedOperationException();
    }

    public static int open(MemorySegment path, MemorySegment db) {
        try{
            return (int) duckdbOpen.invokeExact(path, db);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.DUCKDB, Constants.UNREACHED, throwable);
        }
    }

    public static int openExt(MemorySegment path, MemorySegment db, MemorySegment config, MemorySegment outErr) {
        try{
            return (int) duckdbOpenExt.invokeExact(path, db, config, outErr);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.DUCKDB, Constants.UNREACHED, throwable);
        }
    }

    public static void close(MemorySegment db) {
        try{
            duckdbClose.invokeExact(db);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.DUCKDB, Constants.UNREACHED, throwable);
        }
    }
}
