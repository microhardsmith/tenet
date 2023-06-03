package cn.zorcc.orm.pg;

import java.util.List;

public record PgAuthSaslPwdMsg(
    List<String> mechanisms
) {
}
