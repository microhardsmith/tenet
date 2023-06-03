package cn.zorcc.orm.pg;

public record PgAuthSaslInitialMsg(
        String mechanism,
        String clientFirstMsg
) {
}
