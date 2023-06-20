package cn.zorcc.common.network;

import cn.zorcc.common.WriteBuffer;

/**
 *   Channel Encoder interface determines how the a Java Object can be encoded into the target WriteBuffer
 */
@FunctionalInterface
public interface Encoder {

    /**
     *   Encode a specific object into a memory-segment
     *   This function should only be invoked in its worker's writer thread
     */
    void encode(WriteBuffer writeBuffer, Object o);
}
