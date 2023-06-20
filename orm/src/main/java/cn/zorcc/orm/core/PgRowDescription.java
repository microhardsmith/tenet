package cn.zorcc.orm.core;

public record PgRowDescription(
        String fieldName,
        int tableOid,
        short attr,
        int fieldOid,
        short typeSize,
        int modifier,
        short format
) {

}
