package cn.zorcc.common.network;

public record TaggedResult(
        int tag,
        Object entity
) {
}
