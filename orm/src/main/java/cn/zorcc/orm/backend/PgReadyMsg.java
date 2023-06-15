package cn.zorcc.orm.backend;

import cn.zorcc.orm.pg.PgStatus;

public record PgReadyMsg(
        PgStatus status
) {
}
