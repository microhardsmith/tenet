package cn.zorcc.common.network;

/**
 *   Used as poller msg
 */
public record PollerTask(
        PollerTaskType type,
        Channel channel,
        Object msg
) {
}
