package cn.zorcc.common.network;

import cn.zorcc.common.network.api.Provider;
import cn.zorcc.common.network.api.Sentry;

public record TcpProvider() implements Provider {
    @Override
    public Sentry create(Channel channel) {
        return new TcpSentry(channel);
    }
}
