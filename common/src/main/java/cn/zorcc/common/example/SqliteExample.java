package cn.zorcc.common.example;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.LoggerConsumer;
import cn.zorcc.common.sqlite.Sqlite;
import cn.zorcc.common.sqlite.SqliteConfig;
import cn.zorcc.common.util.NativeUtil;
import cn.zorcc.common.wheel.Wheel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public class SqliteExample {
    private static final Logger log = LoggerFactory.getLogger(SqliteExample.class);
    public static void main(String[] args) {
        Wheel.wheel().init();
        LoggerConsumer loggerConsumer = new LoggerConsumer();
        loggerConsumer.init();
        SqliteConfig sqliteConfig = new SqliteConfig();
        sqliteConfig.setPath("C:/workspace/test.db");
        testSqlite(sqliteConfig);
    }

    private static void testSqlite(SqliteConfig sqliteConfig) {
        log.info("Sqlite version : {}", Sqlite.version());
        if (Sqlite.config(Constants.SQLITE_CONFIG_MULTITHREAD) > 0) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to config");
        }
        if (Sqlite.initialize() > 0) {
            throw new FrameworkException(ExceptionType.SQLITE, "Failed to initialize");
        }
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment ppSqlite = arena.allocate(ValueLayout.ADDRESS).reinterpret(ValueLayout.ADDRESS.byteSize());
            MemorySegment fileName = NativeUtil.allocateStr(arena, sqliteConfig.getPath());
            int open = Sqlite.open(fileName, ppSqlite, Constants.SQLITE_OPEN_READWRITE |
                    Constants.SQLITE_OPEN_CREATE | Constants.SQLITE_OPEN_NOMUTEX | Constants.SQLITE_OPEN_PRIVATECACHE | Constants.SQLITE_OPEN_NOFOLLOW, NativeUtil.NULL_POINTER);
            if(open > 0) {
                throw new FrameworkException(ExceptionType.SQLITE, "Failed to open");
            }
            MemorySegment sqlite = NativeUtil.accessPtr(ppSqlite, arena);
            log.info("sqlite : {}", sqlite.address());
            MemorySegment ppstmt = arena.allocate(ValueLayout.ADDRESS).reinterpret(ValueLayout.ADDRESS.byteSize());
            MemorySegment sql = NativeUtil.allocateStr(arena, "create table if not exists test_sqlite( id int primary key not null, str text not null);");
            int prepare = Sqlite.prepare(sqlite, sql, (int) sql.byteSize(), Constants.SQLITE_PREPARE_NORMALIZE, ppstmt, NativeUtil.NULL_POINTER);
            MemorySegment stmt = NativeUtil.accessPtr(ppstmt, arena);
            if(prepare > 0) {
                log.error("Prepare failed : {}", prepare);
                System.out.println(stmt.address());
                throw new FrameworkException(ExceptionType.SQLITE, "Failed to prepare");
            }
            for( ; ;) {
                int step = Sqlite.step(stmt);
                if(step == Constants.SQLITE_ROW) {
                    throw new FrameworkException(ExceptionType.SQLITE, "Row happen");
                }else if(step == Constants.SQLITE_DONE) {
                    int changes = Sqlite.changes(sqlite);
                    log.info("Done : {}", changes);
                    break;
                }else {
                    throw new FrameworkException(ExceptionType.SQLITE, "Failed to step : %d".formatted(step));
                }
            }
            if (Sqlite.finalize(stmt) > 0) {
                throw new FrameworkException(ExceptionType.SQLITE, "Failed to finalize");
            }
            if (Sqlite.close(sqlite) > 0) {
                throw new FrameworkException(ExceptionType.SQLITE, "Failed to close");
            }
            if(Sqlite.shutdown() > 0) {
                throw new FrameworkException(ExceptionType.SQLITE, "Failed to shutdown");
            }
        }

    }
}
