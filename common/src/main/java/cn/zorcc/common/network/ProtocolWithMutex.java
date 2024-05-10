package cn.zorcc.common.network;

import cn.zorcc.common.structure.Mutex;

/**
 *   Used as writer message
 */
public record ProtocolWithMutex(
        Protocol protocol,
        Mutex mutex
) {
}
