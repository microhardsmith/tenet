package cn.zorcc.orm.backend;

import cn.zorcc.common.Pair;

import java.util.List;

/**
 *   Could be NoticeResponse or ErrorResponse
 */
public record PgStatusResponseMsg(
    boolean isErr,
    List<Pair<Byte, String>> items
) {
}
