package cn.zorcc.common.network;

import cn.zorcc.common.WriteBuffer;

@FunctionalInterface
public interface Encoder {

    /**
     *   Encode a specific object into a memory-segment
     *   This function runs in channel's worker thread
     */
    void encode(WriteBuffer writeBuffer, Object o);
}
