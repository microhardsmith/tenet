package cn.zorcc.common.network;

import cn.zorcc.common.Ref;

import java.lang.foreign.MemorySegment;

public record TagWithRef(
        MemorySegment tag,
        Ref ref
) {
    public TagWithRef(MemorySegment tag) {
        this(tag, new Ref());
    }
}
