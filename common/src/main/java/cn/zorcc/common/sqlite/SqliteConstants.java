package cn.zorcc.common.sqlite;

public class SqliteConstants {
    public static final String CREATE_DISCOVERY_TABLE_SQL = """
            CREATE TABLE IF NOT EXIST discovery (
                id INTEGER PRIMARY KEY,
                app_id INTEGER,
                node_id INTEGER,
                ip_type INTEGER,
                ip TEXT,
                port INTEGER,
                weight INTEGER,
                created_at INTEGER,
                modified_at INTEGER
            )
            """;
    public static final String INSERT_DISCOVERY_SQL = """
            INSERT INTO discovery (app_id, node_id, ip_type, ip, port, weight, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
    public static final String SELECT_ID_DISCOVERY_SQL = """
            SELECT id FROM discovery where app_id = ? and node_id = ?
            """;
    public static final String UPDATE_DISCOVERY_SQL = """
            UPDATE discovery SET weight = ?, modified_at = ? WHERE id = ?
            """;
    public static final String DELETE_DISCOVERY_SQL = """
            DELETE FROM discovery where app_id = ? and node_id = ?
            """;
    public static final String SELECT_MULTI_DISCOVERY_SQL = """
            SELECT app_id, node_id, ip_type, ip, port, weight, created_at, modified_at FROM discovery where app_id = ?
            """;
    public static final String SELECT_ONE_DISCOVERY_SQL = """
            SELECT app_id, node_id, ip_type, ip, port, weight, created_at, modified_at FROM discovery where app_id = ? and node_id = ?
            """;
}
