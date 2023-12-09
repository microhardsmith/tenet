package cn.zorcc.common.network;

import cn.zorcc.common.network.api.Channel;

public record PollerTask(
        PollerTaskType type,
        Channel channel,
        Object msg
) {
}
