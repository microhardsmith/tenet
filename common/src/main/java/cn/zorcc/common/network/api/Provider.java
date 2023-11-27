package cn.zorcc.common.network.api;

import cn.zorcc.common.network.Channel;

/**
 *   Connector factory with customized deallocated procedure provided
 *   If developers want to use other Provider implementation rather than default TcpProvider or SslProvider, use Net.registerProvider() to inject it
 */
public interface Provider {
    /**
     *   Return a newly created connector instance
     */
    Sentry create(Channel channel);

    /**
     *   Release current provider's resources
     */
    void close();
}
