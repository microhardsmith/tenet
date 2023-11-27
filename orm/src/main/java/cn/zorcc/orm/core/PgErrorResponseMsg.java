package cn.zorcc.orm.core;

import cn.zorcc.common.OldPair;

import java.util.List;

public record PgErrorResponseMsg(
        List<OldPair<Byte, String>> items
) {
}
