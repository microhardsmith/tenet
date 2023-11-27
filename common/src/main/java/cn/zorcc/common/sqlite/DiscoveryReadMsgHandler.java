package cn.zorcc.common.sqlite;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.Peer;
import cn.zorcc.common.exception.FrameworkException;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.function.Consumer;

public final class DiscoveryReadMsgHandler implements Consumer<SqliteMsg> {
    private final SqliteConn conn;
    private final MemorySegment selectMulti;
    private final MemorySegment selectOne;
    public DiscoveryReadMsgHandler(SqliteConn conn) {
        this.conn = conn;
        try(Arena arena = Arena.ofConfined()) {
            this.selectMulti = conn.preparePersistentStatement(arena.allocateUtf8String(SqliteConstants.SELECT_MULTI_DISCOVERY_SQL));
            this.selectOne = conn.preparePersistentStatement(arena.allocateUtf8String(SqliteConstants.SELECT_ONE_DISCOVERY_SQL));
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
