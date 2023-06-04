package cn.zorcc.orm.pg;

public record PgAuthSaslResponseMsg(
        int len,
        byte[] bytes
) {
}
