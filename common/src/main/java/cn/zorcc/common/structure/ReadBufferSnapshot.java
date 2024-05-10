package cn.zorcc.common.structure;

import java.lang.foreign.MemorySegment;

public record ReadBufferSnapshot(
        MemorySegment segment,
        long currentIndex
) {
}
