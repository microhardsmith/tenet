package cn.zorcc.common.network;

import cn.zorcc.common.LifeCycle;

public sealed interface ChannelState extends LifeCycle permits TcpChannelState, SslChannelState {

}
