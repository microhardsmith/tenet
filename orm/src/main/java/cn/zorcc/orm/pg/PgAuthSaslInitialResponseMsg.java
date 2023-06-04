package cn.zorcc.orm.pg;

public record PgAuthSaslInitialResponseMsg(
        String mechanism,
        String clientFirstMsg
) {
}
