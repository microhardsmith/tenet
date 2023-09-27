package cn.zorcc.common;

import cn.zorcc.common.binding.SqliteBinding;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.concurrent.atomic.AtomicLong;

public final class SqliteConn implements AutoCloseable {
    private static final AtomicLong counter = new AtomicLong(Constants.ZERO);
    private static final int DEFAULT_OPEN_FLAGS = Constants.SQLITE_OPEN_READWRITE |
            Constants.SQLITE_OPEN_CREATE | Constants.SQLITE_OPEN_PRIVATECACHE |
            Constants.SQLITE_OPEN_NOFOLLOW | Constants.SQLITE_OPEN_NOMUTEX;
    private final MemorySegment sqlite;
    private final Arena arena = Arena.ofConfined();
    private final MemorySegment transactionBegin = arena.allocateUtf8String("BEGIN");
    private final MemorySegment transactionRollback = arena.allocateUtf8String("ROLLBACK");
    private final MemorySegment transactionCommit = arena.allocateUtf8String("COMMIT");
    private final MemorySegment ppErr = arena.allocate(ValueLayout.ADDRESS).reinterpret(ValueLayout.ADDRESS.byteSize());
    public SqliteConn(String filePath) {
        if(counter.getAndIncrement() == Constants.ZERO) {
            SqliteBinding.check(SqliteBinding.config(), "config");
            SqliteBinding.check(SqliteBinding.initialize(), "initialize");
            if(SqliteBinding.threadSafe() == Constants.ZERO) {
                throw new FrameworkException(ExceptionType.SQLITE, "Sqlite library was not compiled in multi thread mode");
            }
        }
        this.sqlite = openDatabase(filePath);
    }

    private static MemorySegment openDatabase(String filePath) {
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment ppDb = arena.allocate(ValueLayout.ADDRESS);
            MemorySegment f = arena.allocateUtf8String(filePath);
            int r = SqliteBinding.open(f, ppDb, SqliteConn.DEFAULT_OPEN_FLAGS);
            MemorySegment sqlite = ppDb.get(ValueLayout.ADDRESS, Constants.ZERO);
            if(r != Constants.ZERO) {
                String err = NativeUtil.getStr(SqliteBinding.errMsg(sqlite));
                int close = SqliteBinding.close(sqlite);
                if(close != Constants.ZERO) {
                    throw new FrameworkException(ExceptionType.SQLITE, STR."Failed to close unopened sqlite database, open err : \{err}, close err : \{close}");
                }else {
                    throw new FrameworkException(ExceptionType.SQLITE, STR."Failed to open sqlite database, open err : \{err}");
                }
            }
            return sqlite;
        }
    }

    public void begin() {
        exec(transactionBegin);
    }

    public void commit() {
        exec(transactionCommit);
    }

    public void rollback() {
        exec(transactionRollback);
    }

    public MemorySegment prepareNormalizeStatement(MemorySegment sql) {
        return prepare(sql, Constants.SQLITE_PREPARE_NORMALIZE);
    }

    public MemorySegment preparePersistentStatement(MemorySegment sql) {
        return prepare(sql, Constants.SQLITE_PREPARE_PERSISTENT);
    }

    private MemorySegment prepare(MemorySegment sql, int flags) {
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment ppStmt = arena.allocate(ValueLayout.ADDRESS).reinterpret(ValueLayout.ADDRESS.byteSize());
            SqliteBinding.check(SqliteBinding.prepare(sqlite, sql, (int) sql.byteSize(), flags, ppStmt), "prepare statement");
            return ppStmt.get(ValueLayout.ADDRESS, Constants.ZERO);
        }
    }

    public int step(MemorySegment stmt) {
        return SqliteBinding.step(stmt);
    }

    public void clearBindings(MemorySegment stmt) {
        SqliteBinding.check(SqliteBinding.clearBindings(stmt), "clear bindings");
    }

    public void reset(MemorySegment stmt) {
        SqliteBinding.check(SqliteBinding.reset(stmt), "reset");
    }

    public void finalize(MemorySegment stmt) {
        SqliteBinding.check(SqliteBinding.finalize(stmt), "finalize");
    }

    public void bindInt(MemorySegment stmt, int index, int value) {
        SqliteBinding.check(SqliteBinding.bindInt(stmt, index, value), "Failed to bind int value");
    }

    public void bindLong(MemorySegment stmt, int index, long value) {
        SqliteBinding.check(SqliteBinding.bindLong(stmt, index, value), "Failed to bind long value");
    }
    public void bindDouble(MemorySegment stmt, int index, double value) {
        SqliteBinding.check(SqliteBinding.bindDouble(stmt, index, value), "Failed to bind double value");
    }
    public void bindText(MemorySegment stmt, int index, MemorySegment str) {
        SqliteBinding.check(SqliteBinding.bindText(stmt, index, str), "Failed to bind text value");
    }
    public void bindBlob(MemorySegment stmt, int index, MemorySegment blob) {
        SqliteBinding.check(SqliteBinding.bindBlob(stmt, index, blob), "Failed to bind blob value");
    }
    public void bindNull(MemorySegment stmt, int index) {
        SqliteBinding.check(SqliteBinding.bindNull(stmt, index), "Failed to bind null value");
    }

    public void exec(MemorySegment sql) {
        int r = SqliteBinding.exec(sqlite, sql, ppErr);
        if(r != Constants.ZERO) {
            String actualSql = sql.getUtf8String(Constants.ZERO);
            MemorySegment pErr = ppErr.get(ValueLayout.ADDRESS, Constants.ZERO).reinterpret(Long.MAX_VALUE);
            String err = NativeUtil.getStr(pErr);
            SqliteBinding.free(pErr);
            throw new FrameworkException(ExceptionType.SQLITE, STR."Unable to exec sql : [\{actualSql}], err : [\{err}]");
        }
    }

    @Override
    public void close() {
        SqliteBinding.close(sqlite);
        if(counter.decrementAndGet() == Constants.ZERO) {
            SqliteBinding.check(SqliteBinding.shutdown(), "shutdown");
        }
    }
}
