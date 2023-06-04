package cn.zorcc.orm.pg;

public record PgParseMsg(
    String name,
    String sql,
    short len,
    int[] objectIds
) {
}
