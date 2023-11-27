package cn.zorcc.orm.backend;

import cn.zorcc.common.OldPair;

import java.util.List;

/**
 *   Could be NoticeResponse or ErrorResponse
 */
public record PgStatusResponseMsg(
    boolean isErr,
    List<OldPair<Byte, String>> items
) {
}
