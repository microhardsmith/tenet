package cn.zorcc.common.sqlite;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.Peer;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.structure.Allocator;
import cn.zorcc.common.structure.MemApi;

import java.lang.foreign.MemorySegment;
import java.util.function.Consumer;

/**
 *   TODO removal
 */
public final class DiscoveryReadMsgHandler implements Consumer<SqliteMsg> {
    private final SqliteConn conn;
    private final MemorySegment selectMulti;
    private final MemorySegment selectOne;
    public DiscoveryReadMsgHandler(SqliteConn conn) {
        this.conn = conn;
        try(Allocator allocator = Allocator.newDirectAllocator(MemApi.DEFAULT)) {
            this.selectMulti = conn.preparePersistentStatement(allocator.allocateFrom(SqliteConstants.SELECT_MULTI_DISCOVERY_SQL));
            this.selectOne = conn.preparePersistentStatement(allocator.allocateFrom(SqliteConstants.SELECT_ONE_DISCOVERY_SQL));
        }
    }
    @Override
    public void accept(SqliteMsg msg) {
        SqliteMsgType type = msg.type();
        if(type == SqliteMsgType.Select_Discovery) {
            executeSelectMulti(msg);
        }else if(type == SqliteMsgType.Shutdown) {
            executeSelectOne(msg);
        }
    }

    private void executeSelectOne(SqliteMsg msg) {

    }

    private void executeSelectMulti(SqliteMsg msg) {
        if (msg.entity() instanceof Peer peer) {

        }else {
            throw new FrameworkException(ExceptionType.SQLITE, Constants.UNREACHED);
        }
    }
}
