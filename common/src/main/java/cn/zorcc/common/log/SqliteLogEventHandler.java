package cn.zorcc.common.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.sqlite.SqliteConn;
import cn.zorcc.common.util.FileUtil;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class SqliteLogEventHandler implements Consumer<LogEvent> {
    private static final String TABLE_CREATE_SQL = """
            CREATE TABLE IF NOT EXISTS log (
                id INTEGER PRIMARY KEY,
                timestamp INTEGER,
                level TEXT,
                class_name TEXT,
                thread_name TEXT,
                msg TEXT,
                throwable TEXT
            )
            """;
    private static final String LOG_INSERT_SQL = """
            INSERT INTO log (timestamp, level, class_name, thread_name, msg, throwable)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
    private final List<LogEvent> eventList = new ArrayList<>();
    private final String dir;
    private final int flushThreshold;
    private final long maxRowCount;
    private final long maxRecordingTime;
    private final Arena reservedArena = Arena.ofConfined();
    private final MemorySegment reserved;
    private long currentCreateTime;
    private long currentRowCount = 0;
    private SqliteConn sqliteConn;
    private MemorySegment stmt;

    public SqliteLogEventHandler(LogConfig logConfig) {
        try{
            SqliteLogConfig config = logConfig.getSqlite();
            dir = FileUtil.normalizePath(config.getDir() == null || config.getDir().isEmpty() ? System.getProperty("user.dir") : config.getDir());
            Path dirPath = Path.of(dir);
            if(!Files.exists(dirPath)) {
                Files.createDirectory(dirPath);
            }
            flushThreshold = config.getFlushThreshold() <= 0 ? Integer.MIN_VALUE : config.getFlushThreshold();
            maxRowCount = config.getMaxRowCount() <= 0 ? Long.MIN_VALUE : config.getMaxRowCount();
            maxRecordingTime = config.getMaxRecordingTime() <= 0 ? Long.MIN_VALUE : config.getMaxRecordingTime();
            reserved = reservedArena.allocate(config.getBuffer());
            openNewSqliteDatabase();
        }catch (IOException e) {
            throw new FrameworkException(ExceptionType.LOG, "Unable to create sqlite database");
        }
    }

    private void openNewSqliteDatabase() {
        Instant instant = Constants.SYSTEM_CLOCK.instant();
        LocalDateTime now = LocalDateTime.ofEpochSecond(instant.getEpochSecond(), instant.getNano(), Constants.LOCAL_ZONE_OFFSET);
        String path = dir +
                Constants.SEPARATOR +
                DateTimeFormatter.ofPattern(Constants.LOG_FILE_NAME_PATTERN).format(now) +
                Constants.SQLITE_FILE_TYPE;
        if(Files.exists(Path.of(path))) {
            throw new FrameworkException(ExceptionType.LOG, "Target sqlite database already exist");
        }
        final SqliteConn oldConn = sqliteConn;
        final MemorySegment oldStmt = stmt;
        try(Arena arena = Arena.ofConfined()) {
            sqliteConn = new SqliteConn(path);
            sqliteConn.exec(arena.allocateUtf8String(TABLE_CREATE_SQL));
            stmt = sqliteConn.preparePersistentStatement(arena.allocateUtf8String(LOG_INSERT_SQL));
        }
        if(oldStmt != null && oldConn != null) {
            oldConn.finalize(oldStmt);
            oldConn.close();
        }
        currentCreateTime = instant.toEpochMilli();
        currentRowCount = 0;
    }

    @Override
    public void accept(LogEvent event) {
        switch (event.eventType()) {
            case Msg -> {
                eventList.add(event);
                if(flushThreshold > 0 && eventList.size() > flushThreshold) {
                    flush();
                    checkDatabase();
                }
            }
            case Flush -> {
                if(!eventList.isEmpty()) {
                    flush();
                    checkDatabase();
                }
            }
            case Shutdown -> {
                flush();
                sqliteConn.finalize(stmt);
                sqliteConn.close();
                reservedArena.close();
            }
        }
    }

    private void flush() {
        if(!eventList.isEmpty()) {
            sqliteConn.begin();
            for (LogEvent logEvent : eventList) {
                try(WriteBuffer writeBuffer = WriteBuffer.newReservedWriteBuffer(reserved)) {
                    sqliteConn.bindLong(stmt, 1, logEvent.timestamp());
                    sqliteConn.bindText(stmt, 2, wrapText(writeBuffer, logEvent.level()));
                    sqliteConn.bindText(stmt, 3, wrapText(writeBuffer, logEvent.className()));
                    sqliteConn.bindText(stmt, 4, wrapText(writeBuffer, logEvent.threadName()));
                    sqliteConn.bindText(stmt, 5, wrapText(writeBuffer, logEvent.msg()));
                    if(logEvent.throwable() == null) {
                        sqliteConn.bindNull(stmt, 6);
                    }else {
                        sqliteConn.bindText(stmt, 6, wrapText(writeBuffer, logEvent.throwable()));
                    }
                    int r = sqliteConn.step(stmt);
                    if (r != Constants.SQLITE_DONE) {
                        throw new FrameworkException(ExceptionType.LOG, "Sqlite database state corrupted");
                    }
                    sqliteConn.clearBindings(stmt);
                    sqliteConn.reset(stmt);
                }
            }
            sqliteConn.commit();
            currentRowCount += eventList.size();
            eventList.clear();
        }
    }

    private void checkDatabase() {
        if((maxRowCount > 0 && currentRowCount > maxRowCount)
                || (maxRecordingTime > 0 && eventList.getLast().timestamp() - currentCreateTime > maxRecordingTime)) {
            openNewSqliteDatabase();
        }
    }

    private static MemorySegment wrapText(WriteBuffer writeBuffer, MemorySegment segment) {
        writeBuffer.writeSegment(segment);
        return writeBuffer.toSegment();
    }
}
