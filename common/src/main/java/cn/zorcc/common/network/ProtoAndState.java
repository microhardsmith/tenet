package cn.zorcc.common.network;

import cn.zorcc.common.network.api.Protocol;

public record ProtoAndState(
        Protocol protocol,
        State state
) {
}
