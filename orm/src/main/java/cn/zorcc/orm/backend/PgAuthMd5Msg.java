package cn.zorcc.orm.backend;

public record PgAuthMd5Msg(
        byte[] salt
) {
}
