package cn.zorcc.orm.backend;

import cn.zorcc.common.postgre.PgStatus;

public record PgReadyMsg(
        PgStatus status
) {
}
