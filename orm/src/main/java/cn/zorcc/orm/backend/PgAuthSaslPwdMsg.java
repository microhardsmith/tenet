package cn.zorcc.orm.backend;

import java.util.List;

public record PgAuthSaslPwdMsg(
    List<String> mechanisms
) {
}
