package cn.zorcc.orm.pg;

public record PgRowDescriptionMsg(
    short len,
    PgRowDescription[] descriptions
) {
}
