package cn.zorcc.orm.core;

import cn.zorcc.common.Pair;

import java.util.List;

public record PgErrorResponseMsg(
        List<Pair<Byte, String>> items
) {
}
