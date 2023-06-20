package cn.zorcc.orm.backend;

import cn.zorcc.orm.core.PgRowDescription;

public record PgRowDescriptionMsg(
    short len,
    PgRowDescription[] descriptions
) {
}
