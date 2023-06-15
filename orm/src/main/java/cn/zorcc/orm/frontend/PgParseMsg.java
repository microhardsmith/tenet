package cn.zorcc.orm.frontend;

public record PgParseMsg(
    String name,
    String sql,
    short len,
    int[] objectIds
) {
}
