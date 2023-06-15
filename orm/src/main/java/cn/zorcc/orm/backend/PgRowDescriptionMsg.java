package cn.zorcc.orm.backend;

import cn.zorcc.orm.pg.PgRowDescription;

public record PgRowDescriptionMsg(
    short len,
    PgRowDescription[] descriptions
) {
}
