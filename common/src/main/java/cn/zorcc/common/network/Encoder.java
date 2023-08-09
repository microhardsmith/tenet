package cn.zorcc.common.network;

import cn.zorcc.common.WriteBuffer;

/**
 *   Channel Encoder interface determines how the a Java Object can be encoded into the target WriteBuffer
 */
@FunctionalInterface
public interface Encoder {

    /**
     *   Encode a specific object into a WriteBuffer, the returned writeBuffer should be the given one in most cases
     *   This function should only be invoked in its worker's writer thread
     */
    WriteBuffer encode(WriteBuffer writeBuffer, Object o);
}
