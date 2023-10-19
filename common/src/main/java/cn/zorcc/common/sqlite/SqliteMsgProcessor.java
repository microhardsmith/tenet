package cn.zorcc.common.sqlite;

public interface SqliteMsgProcessor {
    /**
     *   Processing the incoming sqlite msg
     */
    void handle(SqliteMsg msg);

    /**
     *   Shutdown current sqlite msg processor
     */
    void shutdown(SqliteConn conn);
}
