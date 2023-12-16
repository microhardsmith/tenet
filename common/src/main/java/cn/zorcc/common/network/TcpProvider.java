package cn.zorcc.common.network;

import cn.zorcc.common.log.Logger;
import cn.zorcc.common.network.api.Provider;
import cn.zorcc.common.network.api.Sentry;

public final class TcpProvider implements Provider {
    private static final Logger log = new Logger(TcpProvider.class);

    public TcpProvider() {
        log.debug("TcpProvider instance created");
    }

    @Override
    public Sentry create(Channel channel) {
        return new TcpSentry(channel);
    }

    @Override
    public void close() {
        log.debug("TcpProvider instance released");
    }
}
