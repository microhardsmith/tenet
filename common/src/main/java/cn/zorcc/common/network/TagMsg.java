package cn.zorcc.common.network;

import java.lang.foreign.MemorySegment;

public record TagMsg(
        MemorySegment tag,
        Object msg
) {
}
