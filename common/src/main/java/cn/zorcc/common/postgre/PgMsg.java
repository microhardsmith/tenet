package cn.zorcc.common.postgre;

import java.lang.foreign.MemorySegment;

/**
 *   Representing a postgresql client-server communicating msg
 */
public record PgMsg(
        byte type,
        MemorySegment data
) {
}
