package cn.zorcc.orm.pg;

import cn.zorcc.common.Pair;

import java.util.List;

public record PgErrorResponseMsg(
        List<Pair<Byte, String>> items
) {
}
