package cn.zorcc.orm.backend;

import cn.zorcc.common.postgre.PgRowDescription;

public record PgRowDescriptionMsg(
    short len,
    PgRowDescription[] descriptions
) {
}
