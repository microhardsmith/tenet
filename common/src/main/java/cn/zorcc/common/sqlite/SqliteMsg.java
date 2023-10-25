package cn.zorcc.common.sqlite;

import cn.zorcc.common.Carrier;

public record SqliteMsg (
    SqliteMsgType type,
    Object entity,
    Carrier carrier
) {


}
