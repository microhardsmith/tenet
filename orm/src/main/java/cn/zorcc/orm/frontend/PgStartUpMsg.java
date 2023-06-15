package cn.zorcc.orm.frontend;

import cn.zorcc.orm.PgConfig;

public record PgStartUpMsg(
        PgConfig pgConfig
) {

}
