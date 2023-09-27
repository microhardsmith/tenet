package cn.zorcc.common.binding;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 *   Sqlite bindings for the underlying dynamic library
 *   The binding class shouldn't be directly used unless developers knows perfectly what they want to achieve
 */
public final class SqliteBinding {
    private static final long SQLITE_TRANSIENT = -1L;
    private static final MemorySegment sqliteTransient = MemorySegment.ofAddress(SQLITE_TRANSIENT);
    private static final MethodHandle threadHandle;
    private static final MethodHandle openHandle;
    private static final MethodHandle configHandle;
    private static final MethodHandle initializeHandle;
    private static final MethodHandle prepareHandle;
    private static final MethodHandle bindIntHandle;
    private static final MethodHandle bindLongHandle;
    private static final MethodHandle bindDoubleHandle;
    private static final MethodHandle bindTextHandle;
    private static final MethodHandle bindBlobHandle;
    private static final MethodHandle bindNullHandle;
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
    private static final MethodHandle backupInitHandle;
    private static final MethodHandle backupStepHandle;
    private static final MethodHandle backupFinishHandle;
    private static final MethodHandle freeHandle;
    private static final MethodHandle shutdownHandle;
    private static final MethodHandle closeHandle;
    private static final MethodHandle errMsgHandle;
    private static final MethodHandle execHandle;

    static {
        SymbolLookup symbolLookup = NativeUtil.loadLibrary(Constants.SQLITE);
        threadHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_threadsafe", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        openHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_open_v2", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        configHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_config", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        initializeHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_initialize", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        prepareHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_prepare_v3", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        bindIntHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_bind_int", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        bindLongHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_bind_int64", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));
        bindDoubleHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_bind_double", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_DOUBLE));
        bindTextHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_bind_text64", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS,  ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_BYTE));
        bindBlobHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_bind_blob64", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
        bindNullHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_bind_null", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
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
        changesHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_changes64", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
        backupInitHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_backup_init", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        backupStepHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_backup_step", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        backupFinishHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_backup_finish", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        freeHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_free", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        shutdownHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_shutdown", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        closeHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_close_v2", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        errMsgHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_errmsg", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        execHandle = NativeUtil.methodHandle(symbolLookup, "sqlite3_exec", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
    }

    public static void check(int r, String errMsg) {
        if(r != Constants.ZERO) {
            throw new FrameworkException(ExceptionType.SQLITE, STR."Failed to \{errMsg} with err code : \{r}");
        }
    }

    public static int threadSafe() {
        try{
            return (int) threadHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Unable to call threadsafe", throwable);
        }
    }

    public static int config() {
        try{
            return (int) configHandle.invokeExact(Constants.SQLITE_CONFIG_MULTITHREAD);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to config");
        }
    }

    public static int initialize() {
        try{
            return (int) initializeHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to initialize");
        }
    }

    public static int open(MemorySegment fileName, MemorySegment ppDb, int flags) {
        try{
            return (int) openHandle.invokeExact(fileName, ppDb, flags, NativeUtil.NULL_POINTER);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to open");
        }
    }

    public static int prepare(MemorySegment sqlite, MemorySegment sql, int len, int flags, MemorySegment ppStmt) {
        try{
            return (int) prepareHandle.invokeExact(sqlite, sql, len, flags, ppStmt, NativeUtil.NULL_POINTER);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to prepare");
        }
    }

    public static int bindInt(MemorySegment stmt, int index, int value) {
        try{
            return (int) bindIntHandle.invokeExact(stmt, index, value);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to bind int");
        }
    }

    public static int bindLong(MemorySegment stmt, int index, long value) {
        try{
            return (int) bindLongHandle.invokeExact(stmt, index, value);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to bind long");
        }
    }

    public static int bindDouble(MemorySegment stmt, int index, double value) {
        try{
            return (int) bindDoubleHandle.invokeExact(stmt, index, value);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to bind double");
        }
    }

    public static int bindText(MemorySegment stmt, int index, MemorySegment ptr) {
        try{
            return (int) bindTextHandle.invokeExact(stmt, index, ptr, ptr.byteSize(), sqliteTransient, Constants.SQLITE_UTF8);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to bind text");
        }
    }

    public static int bindBlob(MemorySegment stmt, int index, MemorySegment ptr) {
        try{
            return (int) bindBlobHandle.invokeExact(stmt, index, ptr, ptr.byteSize(), sqliteTransient);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to bind blob");
        }
    }

    public static int bindNull(MemorySegment stmt, int index) {
        try{
            return (int) bindNullHandle.invokeExact(stmt, index);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to bind null");
        }
    }

    public static int step(MemorySegment stmt) {
        try{
            return (int) stepHandle.invokeExact(stmt);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to step");
        }
    }

    public static int columnBytes(MemorySegment stmt, int index) {
        try{
            return (int) columnBytesHandle.invokeExact(stmt, index);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to columnBytes");
        }
    }

    public static int columnInt(MemorySegment stmt, int index) {
        try{
            return (int) columnIntHandle.invokeExact(stmt, index);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to columnInt");
        }
    }

    public static long columnLong(MemorySegment stmt, int index) {
        try{
            return (long) columnLongHandle.invokeExact(stmt, index);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to columnLong");
        }
    }

    public static double columnDouble(MemorySegment stmt, int index) {
        try{
            return (double) columnDoubleHandle.invokeExact(stmt, index);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to columnDouble");
        }
    }

    public static MemorySegment columnText(MemorySegment stmt, int index) {
        try{
            return (MemorySegment) columnTextHandle.invokeExact(stmt, index);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to columnText");
        }
    }

    public static MemorySegment columnBlob(MemorySegment stmt, int index) {
        try{
            return (MemorySegment) columnBlobHandle.invokeExact(stmt, index);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to columnBlob");
        }
    }

    public static int reset(MemorySegment pStmt) {
        try{
            return (int) resetHandle.invokeExact(pStmt);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to reset");
        }
    }

    public static int clearBindings(MemorySegment pStmt) {
        try{
            return (int) clearBindingsHandle.invokeExact(pStmt);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to clearBindings");
        }
    }

    public static long changes(MemorySegment sqlite) {
        try{
            return (long) changesHandle.invokeExact(sqlite);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to get changes");
        }
    }

    public static int finalize(MemorySegment pStmt) {
        try{
            return (int) finalizeHandle.invokeExact(pStmt);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to finalize");
        }
    }

    public static MemorySegment backupInit(MemorySegment dest, MemorySegment destName, MemorySegment source, MemorySegment sourceName) {
        try{
            return (MemorySegment) backupInitHandle.invokeExact(dest, destName, source, sourceName);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to backupInit");
        }
    }

    public static int backupStep(MemorySegment backup, int nPage) {
        try{
            return (int) backupStepHandle.invokeExact(backup, nPage);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to backupStep");
        }
    }

    public static int backupFinish(MemorySegment backup) {
        try{
            return (int) backupFinishHandle.invokeExact(backup);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to backupFinish");
        }
    }

    public static void free(MemorySegment ptr) {
        try{
            freeHandle.invokeExact(ptr);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to free");
        }
    }

    public static int close(MemorySegment sqlite) {
        try{
            return (int) closeHandle.invokeExact(sqlite);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to close");
        }
    }

    public static int shutdown() {
        try {
            return (int) shutdownHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to shutdown");
        }
    }

    public static MemorySegment errMsg(MemorySegment sqlite) {
        try{
            return (MemorySegment) errMsgHandle.invokeExact(sqlite);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to get errMsg");
        }
    }

    public static int exec(MemorySegment sqlite, MemorySegment sql, MemorySegment err) {
        try{
            return (int) execHandle.invokeExact(sqlite, sql, NativeUtil.NULL_POINTER, NativeUtil.NULL_POINTER, err);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to exec");
        }
    }
}
