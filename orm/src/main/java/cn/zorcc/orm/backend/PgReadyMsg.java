package cn.zorcc.orm.backend;

import cn.zorcc.orm.core.PgStatus;

public record PgReadyMsg(
        PgStatus status
) {
}
