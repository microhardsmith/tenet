package cn.zorcc.common.sqlite;

import cn.zorcc.common.structure.Allocator;
import cn.zorcc.common.structure.MemApi;

import java.lang.foreign.MemorySegment;
import java.util.function.Consumer;

/**
 *   TODO removal
 */
public final class DiscoveryWriteMsgHandler implements Consumer<SqliteMsg> {
    private final SqliteConn conn;
    private final MemorySegment insertStmt;
    private final MemorySegment selectIdStmt;
    private final MemorySegment updateStmt;
    private final MemorySegment deleteStmt;
    public DiscoveryWriteMsgHandler(SqliteConn conn) {
        this.conn = conn;
        try(Allocator allocator = Allocator.newDirectAllocator(MemApi.DEFAULT)) {
            conn.exec(allocator.allocateFrom(SqliteConstants.CREATE_DISCOVERY_TABLE_SQL));
            this.insertStmt = conn.preparePersistentStatement(allocator.allocateFrom(SqliteConstants.INSERT_DISCOVERY_SQL));
            this.selectIdStmt = conn.preparePersistentStatement(allocator.allocateFrom(SqliteConstants.SELECT_ID_DISCOVERY_SQL));
            this.updateStmt = conn.preparePersistentStatement(allocator.allocateFrom(SqliteConstants.UPDATE_DISCOVERY_SQL));
            this.deleteStmt = conn.preparePersistentStatement(allocator.allocateFrom(SqliteConstants.DELETE_DISCOVERY_SQL));
        }
    }

    @Override
    public void accept(SqliteMsg msg) {
        switch (msg.type()) {

        }
    }
}
