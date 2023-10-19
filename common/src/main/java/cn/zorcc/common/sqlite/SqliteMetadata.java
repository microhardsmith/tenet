package cn.zorcc.common.sqlite;

public record SqliteMetadata(
        String type,
        String name,
        String tblName,
        Integer rootPage,
        String sql
) {
}
