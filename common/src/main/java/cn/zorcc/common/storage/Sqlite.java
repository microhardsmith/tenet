package cn.zorcc.common.storage;

import cn.zorcc.common.Clock;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.concurrent.TimeUnit;

/**
 *   RocksDB native bindings
 */
@Slf4j
public final class Sqlite {
    /**
     *   Environment variable that must be configured when launching the application
     */
    public static final String SQLITE_LIB = "sqlite";
    /**
     *   Err pointer, initialized as NULL pointer, the actual memory pointed will be modified by sqlite, thus need to be explicitly freed
     */
    private static final MemorySegment err = NativeUtil.NULL_POINTER;
    /**
     *   Err dual pointer, used for modifying Err pointer
     */
    private static final MemorySegment errPtr = MemorySegment.ofAddress(err.address());
    private static final String version;
    private static final MethodHandle openHandle;
    private static final MethodHandle configHandle; // TODO variadic variables still in-process for Project-Panama
    private static final MethodHandle initializeHandle;

    private static final MethodHandle prepareHandle;
    private static final MethodHandle bindIntHandle;
    private static final MethodHandle bindLongHandle;
    private static final MethodHandle bindDoubleHandle;
    private static final MethodHandle bindTextHandle;
    private static final MethodHandle bindBlobHandle;
    private static final MethodHandle stepHandle;
    private static final MethodHandle columnBytesHandle;
    private static final MethodHandle columnIntHandle;
    private static final MethodHandle columnLongHandle;
    private static final MethodHandle columnDoubleHandle;
    private static final MethodHandle columnTextHandle;
    private static final MethodHandle columnBlobHandle;
    private static final MethodHandle resetHandle;
    private static final MethodHandle clearBindingsHandle;
    private static final MethodHandle finalizeHandle;
    private static final MethodHandle changesHandle;

    private static final MethodHandle freeHandle;
    private static final MethodHandle shutdownHandle;
    private static final MethodHandle closeHandle;

    static {
        long nano = Clock.nano();
        SymbolLookup symbolLookup = NativeUtil.loadLibrary(SQLITE_LIB);
        MethodHandle versionHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_libversion", FunctionDescriptor.of(NativeUtil.UNBOUNDED_PTR_LAYOUT));
        try{
            MemorySegment versionPtr = (MemorySegment) versionHandle.invokeExact();
            version = NativeUtil.getStr(versionPtr);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Unable to get sqlite version", throwable);
        }
        openHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_open_v2", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        configHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_config", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        initializeHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_initialize", FunctionDescriptor.of(ValueLayout.JAVA_INT));

        prepareHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_prepare_v3", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        bindIntHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_bind_int", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        bindLongHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_bind_int64", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));
        bindDoubleHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_bind_double", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_DOUBLE));
        bindTextHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_bind_text", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS,  ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        bindBlobHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_bind_blob", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        stepHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_step", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

        columnBytesHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_column_bytes", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        columnIntHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_column_int", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        columnLongHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_column_int64", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        columnDoubleHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_column_double", FunctionDescriptor.of(ValueLayout.JAVA_DOUBLE, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        columnTextHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_column_text", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        columnBlobHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_column_blob", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

        resetHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_reset", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        clearBindingsHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_clear_bindings", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

        finalizeHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_finalize", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        changesHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_changes", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        freeHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_free", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        shutdownHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_shutdown", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        closeHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_close_v2", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        log.info("Initializing Sqlite successfully, cost : {} ms", TimeUnit.NANOSECONDS.toMillis(Clock.elapsed(nano)));
    }

    private Sqlite() {
        throw new UnsupportedOperationException();
    }

    public static String version() {
        return version;
    }

    public static MemorySegment errPtr() {
        return errPtr;
    }

    public static String errStr() {
        return NativeUtil.checkNullPointer(err) ? null : NativeUtil.getStr(err);
    }

    /**
     *   Corresponding to int sqlite3_open_v2(const char *filename, sqlite3 **ppDb, int flags, const char *zVfs)
     */
    public static int open(MemorySegment fileName, MemorySegment ppDb, int flags, MemorySegment zVfs) {
        try{
            return (int) openHandle.invokeExact(fileName, ppDb, flags, zVfs);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to open");
        }
    }

    /**
     *   Corresponding to int sqlite3_close_v2(sqlite3*)
     *   See <a href="https://www.sqlite.org/c3ref/config.html">...</a>
     */
    public static int config(int option) {
        try{
            return (int) configHandle.invokeExact(option);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to config");
        }
    }

    /**
     *   Corresponding to int sqlite3_initialize(void)
     *   See <a href="https://www.sqlite.org/c3ref/initialize.html">...</a>
     */
    public static int initialize() {
        try{
            return (int) initializeHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to initialize");
        }
    }

    /**
     *   Corresponding to int sqlite3_prepare_v3(sqlite3 *db, const char *zSql, int nByte, unsigned int prepFlags, sqlite3_stmt **ppStmt, const char **pzTail)
     *   See <a href="https://www.sqlite.org/c3ref/initialize.html">...</a>
     */
    public static int prepare(MemorySegment sqlite, MemorySegment sql, int len, int flags, MemorySegment ppStmt, MemorySegment pzTail) {
        try{
            return (int) prepareHandle.invokeExact(sqlite, sql, len, flags, ppStmt, pzTail);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to prepare");
        }
    }

    /**
     *   Corresponding to int sqlite3_bind_int(sqlite3_stmt*, int, int)
     *   See <a href="https://www.sqlite.org/c3ref/bind_blob.html">...</a>
     */
    public static int bindInt(MemorySegment stmt, int index, int value) {
        try{
            return (int) bindIntHandle.invokeExact(stmt, index, value);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to bind int");
        }
    }

    /**
     *   Corresponding to int sqlite3_bind_int(sqlite3_stmt*, int, int)
     *   See <a href="https://www.sqlite.org/c3ref/bind_blob.html">...</a>
     */
    public static int bindLong(MemorySegment stmt, int index, long value) {
        try{
            return (int) bindLongHandle.invokeExact(stmt, index, value);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to bind long");
        }
    }

    /**
     *   Corresponding to int sqlite3_bind_double(sqlite3_stmt*, int, double)
     *   See <a href="https://www.sqlite.org/c3ref/bind_blob.html">...</a>
     */
    public static int bindDouble(MemorySegment stmt, int index, double value) {
        try{
            return (int) bindDoubleHandle.invokeExact(stmt, index, value);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to bind double");
        }
    }

    /**
     *   Corresponding to int sqlite3_bind_text(sqlite3_stmt*,int,const char*,int,void(*)(void*))
     *   See <a href="https://www.sqlite.org/c3ref/bind_blob.html">...</a>
     */
    public static int bindText(MemorySegment stmt, int index, MemorySegment ptr, int len, MemorySegment finalizer) {
        try{
            return (int) bindTextHandle.invokeExact(stmt, index, ptr, len, finalizer);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to bind text");
        }
    }

    /**
     *   Corresponding to int sqlite3_bind_blob(sqlite3_stmt*, int, const void*, int n, void(*)(void*))
     *   See <a href="https://www.sqlite.org/c3ref/bind_blob.html">...</a>
     */
    public static int bindBlob(MemorySegment stmt, int index, MemorySegment ptr, int len, MemorySegment finalizer) {
        try{
            return (int) bindBlobHandle.invokeExact(stmt, index, ptr, len, finalizer);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to bind blob");
        }
    }

    /**
     *   Corresponding to int sqlite3_bind_blob(sqlite3_stmt*, int, const void*, int n, void(*)(void*))
     *   See <a href="https://www.sqlite.org/c3ref/step.html">...</a>
     */
    public static int step(MemorySegment stmt) {
        try{
            return (int) stepHandle.invokeExact(stmt);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to step");
        }
    }

    /**
     *   Corresponding to int sqlite3_column_bytes(sqlite3_stmt*, int iCol)
     *   See <a href="https://www.sqlite.org/c3ref/column_blob.html">...</a>
     */
    public static int columnBytes(MemorySegment stmt, int index) {
        try{
            return (int) columnBytesHandle.invokeExact(stmt, index);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to columnBytes");
        }
    }

    /**
     *   Corresponding to int sqlite3_column_int(sqlite3_stmt*, int iCol)
     *   See <a href="https://www.sqlite.org/c3ref/column_blob.html">...</a>
     */
    public static int columnInt(MemorySegment stmt, int index) {
        try{
            return (int) columnIntHandle.invokeExact(stmt, index);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to columnInt");
        }
    }

    /**
     *   Corresponding to sqlite3_int64 sqlite3_column_int64(sqlite3_stmt*, int iCol)
     *   See <a href="https://www.sqlite.org/c3ref/column_blob.html">...</a>
     */
    public static long columnLong(MemorySegment stmt, int index) {
        try{
            return (long) columnLongHandle.invokeExact(stmt, index);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to columnLong");
        }
    }

    /**
     *   Corresponding to double sqlite3_column_double(sqlite3_stmt*, int iCol)
     *   See <a href="https://www.sqlite.org/c3ref/column_blob.html">...</a>
     */
    public static double columnDouble(MemorySegment stmt, int index) {
        try{
            return (double) columnDoubleHandle.invokeExact(stmt, index);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to columnDouble");
        }
    }

    /**
     *   Corresponding to const unsigned char *sqlite3_column_text(sqlite3_stmt*, int iCol)
     *   See <a href="https://www.sqlite.org/c3ref/column_blob.html">...</a>
     */
    public static MemorySegment columnText(MemorySegment stmt, int index) {
        try{
            return (MemorySegment) columnTextHandle.invokeExact(stmt, index);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to columnText");
        }
    }

    /**
     *   Corresponding to const void *sqlite3_column_blob(sqlite3_stmt*, int iCol)
     *   See <a href="https://www.sqlite.org/c3ref/column_blob.html">...</a>
     */
    public static MemorySegment columnBlob(MemorySegment stmt, int index) {
        try{
            return (MemorySegment) columnBlobHandle.invokeExact(stmt, index);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to columnBlob");
        }
    }

    /**
     *   Corresponding to int sqlite3_reset(sqlite3_stmt *pStmt)
     *   See <a href="https://www.sqlite.org/c3ref/reset.html">...</a>
     */
    public static int reset(MemorySegment pStmt) {
        try{
            return (int) resetHandle.invokeExact(pStmt);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to reset");
        }
    }

    /**
     *   Corresponding to int sqlite3_clear_bindings(sqlite3_stmt*)
     *   See <a href="https://www.sqlite.org/c3ref/clear_bindings.html">...</a>
     */
    public static int clearBindings(MemorySegment pStmt) {
        try{
            return (int) clearBindingsHandle.invokeExact(pStmt);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to clearBindings");
        }
    }

    /**
     *   Corresponding to int sqlite3_changes(sqlite3*)
     *   See <a href="http://www.sqlite.org/c3ref/changes.html">...</a>
     */
    public static int changes(MemorySegment sqlite) {
        try{
            return (int) changesHandle.invokeExact(sqlite);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to get changes");
        }
    }

    /**
     *   Corresponding to int sqlite3_finalize(sqlite3_stmt *pStmt)
     *   See <a href="https://www.sqlite.org/c3ref/finalize.html">...</a>
     */
    public static int finalize(MemorySegment pStmt) {
        try{
            return (int) finalizeHandle.invokeExact(pStmt);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to finalize");
        }
    }

    /**
     *   Corresponding to void sqlite3_free(void*)
     *   See <a href="https://www.sqlite.org/c3ref/free.html">...</a>
     */
    public static void free(MemorySegment ptr) {
        try{
            freeHandle.invokeExact(ptr);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to free");
        }
    }

    /**
     *   Corresponding to int sqlite3_shutdown(void)
     *   See <a href="https://www.sqlite.org/c3ref/initialize.html">...</a>
     */
    public static int shutdown() {
        try{
            return (int) shutdownHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to shutdown");
        }
    }

    /**
     *   Corresponding to int sqlite3_close_v2(sqlite3*)
     *   See <a href="https://www.sqlite.org/c3ref/close.html">...</a>
     */
    public static int close(MemorySegment sqlite) {
        try{
            return (int) closeHandle.invokeExact(sqlite);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to close");
        }
    }

}
