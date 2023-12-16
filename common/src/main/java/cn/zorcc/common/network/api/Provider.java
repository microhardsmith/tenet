package cn.zorcc.common.network.api;

import cn.zorcc.common.network.Channel;

/**
 *   Sentry factory with customized deallocated procedure provided
 */
@FunctionalInterface
public interface Provider {
    /**
     *   Return a newly created sentry instance
     */
    Sentry create(Channel channel);

    /**
     *   Release current provider's resources, implementation could choose to override it
     */
    default void close() {

    }
}
