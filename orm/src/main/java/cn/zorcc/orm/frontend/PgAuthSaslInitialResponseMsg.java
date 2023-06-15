package cn.zorcc.orm.frontend;

public record PgAuthSaslInitialResponseMsg(
        String mechanism,
        String clientFirstMsg
) {
}
