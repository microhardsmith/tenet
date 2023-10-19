package cn.zorcc.common.sqlite;

import cn.zorcc.common.Carrier;

public final class SqliteMsg extends Carrier {
    private final SqliteMsgType type;
    private final Object entity;

    public static final SqliteMsg shutdownMsg = new SqliteMsg(SqliteMsgType.Shutdown, null);

    public SqliteMsg(SqliteMsgType type, Object entity) {
        this.type = type;
        this.entity = entity;
    }

    public SqliteMsgType type() {
        return type;
    }

    public Object entity() {
        return entity;
    }
}
