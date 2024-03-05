package cn.zorcc.common.network;

import cn.zorcc.common.State;

public record ProtoAndState(
        Protocol protocol,
        State state
) {
}
