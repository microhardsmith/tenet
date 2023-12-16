package cn.zorcc.common.network;

public record PollerTask(
        PollerTaskType type,
        Channel channel,
        Object msg
) {
}
