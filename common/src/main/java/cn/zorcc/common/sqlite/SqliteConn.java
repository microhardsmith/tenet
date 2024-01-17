package cn.zorcc.common.sqlite;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.bindings.SqliteBinding;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.structure.Allocator;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 *   Delegate a sqlite connection, all the read-write operations should be controlled inside the sqliteConn object rather than directly using SqliteBindings
 *   TODO completely refactor needed
 */
public final class SqliteConn implements AutoCloseable {
    private static final AtomicLong counter = new AtomicLong(0);
    private static final int DEFAULT_OPEN_FLAGS = Constants.SQLITE_OPEN_READWRITE |
            Constants.SQLITE_OPEN_CREATE | Constants.SQLITE_OPEN_PRIVATECACHE |
            Constants.SQLITE_OPEN_NOFOLLOW | Constants.SQLITE_OPEN_NOMUTEX;
    private static final String FETCH_METADATA = "SELECT * FROM sqlite_master";
    private static final MemorySegment transactionBegin = NativeUtil.globalArena.allocateFrom("BEGIN", StandardCharsets.UTF_8);
    private static final MemorySegment transactionRollback = NativeUtil.globalArena.allocateFrom("ROLLBACK", StandardCharsets.UTF_8);
    private static final MemorySegment transactionCommit = NativeUtil.globalArena.allocateFrom("COMMIT", StandardCharsets.UTF_8);
    private final MemorySegment sqlite;
    private final Arena reservedArena = Arena.ofConfined();
    private final MemorySegment ppErr = reservedArena.allocate(ValueLayout.ADDRESS).reinterpret(ValueLayout.ADDRESS.byteSize());
    public SqliteConn(String filePath) {
        if(counter.getAndIncrement() == 0) {
            SqliteBinding.check(SqliteBinding.config(), "config");
            SqliteBinding.check(SqliteBinding.initialize(), "initialize");
            if(SqliteBinding.threadSafe() == 0) {
                throw new FrameworkException(ExceptionType.SQLITE, "Sqlite library was not compiled in multi thread mode");
            }
        }
        this.sqlite = openDatabase(filePath);
    }

    private static MemorySegment openDatabase(String filePath) {
        try(Allocator allocator = Allocator.newDirectAllocator()) {
            MemorySegment ppDb = allocator.allocate(ValueLayout.ADDRESS);
            MemorySegment f = allocator.allocateFrom(filePath);
            int r = SqliteBinding.open(f, ppDb, SqliteConn.DEFAULT_OPEN_FLAGS);
            MemorySegment sqlite = ppDb.get(ValueLayout.ADDRESS, 0);
            if(r != 0) {
                String err = SqliteBinding.errMsg(sqlite).getString(0L, StandardCharsets.UTF_8);
                int close = SqliteBinding.close(sqlite);
                if(close != 0) {
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

    public List<SqliteMetadata> fetchingMetadata() {
        try(Allocator allocator = Allocator.newDirectAllocator()) {
            List<SqliteMetadata> result = new ArrayList<>();
            MemorySegment stmt = prepareNormalizeStatement(allocator.allocateFrom(FETCH_METADATA));
            for( ; ; ) {
                int r = SqliteBinding.step(stmt);
                if(r == Constants.SQLITE_DONE) {
                    return result;
                }else if(r == Constants.SQLITE_ROW) {
                    String type = columnText(stmt, 1);
                    String name = columnText(stmt, 2);
                    String tblName = columnText(stmt, 3);
                    Integer rootPage = columnInt(stmt, 4);
                    String sql = columnText(stmt, 5);
                    result.add(new SqliteMetadata(type, name, tblName, rootPage, sql));
                }else {
                    throw new FrameworkException(ExceptionType.SQLITE, Constants.UNREACHED);
                }
            }
        }
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
            return ppStmt.get(ValueLayout.ADDRESS, 0);
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

    public Integer columnInt(MemorySegment stmt, int index) {
        int r = SqliteBinding.columnInt(stmt, index);
        if(r == 0 && SqliteBinding.columnType(stmt, index) == Constants.SQLITE_NULL) {
            return null;
        }else {
            return r;
        }
    }
    public Long columnLong(MemorySegment stmt, int index) {
        long r = SqliteBinding.columnLong(stmt, index);
        if(r == 0 && SqliteBinding.columnType(stmt, index) == Constants.SQLITE_NULL) {
            return null;
        }else {
            return r;
        }
    }

    public Double columnDouble(MemorySegment stmt, int index) {
        double d = SqliteBinding.columnDouble(stmt, index);
        if(Math.abs(d) < Double.MIN_VALUE && SqliteBinding.columnType(stmt, index) == Constants.SQLITE_NULL) {
            return null;
        }else {
            return d;
        }
    }

    public String columnText(MemorySegment stmt, int index) {
        MemorySegment r = SqliteBinding.columnText(stmt, index);
        if(NativeUtil.checkNullPointer(r) && SqliteBinding.columnType(stmt, index) == Constants.SQLITE_DONE) {
            return null;
        }else {
            int len = SqliteBinding.columnBytes(stmt, index);
            return new String(r.reinterpret(len).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8);
        }
    }

    public MemorySegment columnBlob(MemorySegment stmt, int index) {
        MemorySegment r = SqliteBinding.columnBlob(stmt, index);
        if(NativeUtil.checkNullPointer(r) && SqliteBinding.columnType(stmt, index) == Constants.SQLITE_DONE) {
            return null;
        }else {
            int len = SqliteBinding.columnBytes(stmt, index);
            r = r.reinterpret(len);
            byte[] bytes = new byte[len];
            MemorySegment m = MemorySegment.ofArray(bytes);
            MemorySegment.copy(r, 0, m, 0, len);
            return m;
        }
    }

    public void exec(MemorySegment sql) {
        int r = SqliteBinding.exec(sqlite, sql, ppErr);
        if(r != 0) {
            String actualSql = sql.getString(0L);
            MemorySegment pErr = ppErr.get(ValueLayout.ADDRESS, 0).reinterpret(Long.MAX_VALUE);
            String err = pErr.getString(0L);
            SqliteBinding.free(pErr);
            throw new FrameworkException(ExceptionType.SQLITE, STR."Unable to exec sql : [\{actualSql}], err : [\{err}]");
        }
    }

    @Override
    public void close() {
        SqliteBinding.close(sqlite);
        if(counter.decrementAndGet() == 0) {
            SqliteBinding.check(SqliteBinding.shutdown(), "shutdown");
        }
    }
}
