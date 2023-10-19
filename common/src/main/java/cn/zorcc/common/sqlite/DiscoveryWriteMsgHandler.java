package cn.zorcc.common.sqlite;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.function.Consumer;

public final class DiscoveryWriteMsgHandler implements Consumer<SqliteMsg> {
    private final SqliteConn conn;
    private final MemorySegment insertStmt;
    private final MemorySegment selectIdStmt;
    private final MemorySegment updateStmt;
    private final MemorySegment deleteStmt;
    public DiscoveryWriteMsgHandler(SqliteConn conn) {
        this.conn = conn;
        try(Arena arena = Arena.ofConfined()) {
            conn.exec(arena.allocateUtf8String(SqliteConstants.CREATE_DISCOVERY_TABLE_SQL));
            this.insertStmt = conn.preparePersistentStatement(arena.allocateUtf8String(SqliteConstants.INSERT_DISCOVERY_SQL));
            this.selectIdStmt = conn.preparePersistentStatement(arena.allocateUtf8String(SqliteConstants.SELECT_ID_DISCOVERY_SQL));
            this.updateStmt = conn.preparePersistentStatement(arena.allocateUtf8String(SqliteConstants.UPDATE_DISCOVERY_SQL));
            this.deleteStmt = conn.preparePersistentStatement(arena.allocateUtf8String(SqliteConstants.DELETE_DISCOVERY_SQL));
        }
    }

    @Override
    public void accept(SqliteMsg msg) {
        switch (msg.type()) {

        }
    }
}
