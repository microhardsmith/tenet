package cn.zorcc.common.network.api;

import cn.zorcc.common.WriteBuffer;

/**
 *   Channel Encoder interface determines how a Java Object should be encoded into the target WriteBuffer
 */
@FunctionalInterface
public interface Encoder {

    /**
     *   Encode a specific object into a WriteBuffer
     *   This function would be invoked only in worker's writer thread, developers should take care about not blocking the writer thread by doing time-consuming logic
     *   Encoder mode is not designed for transferring large data or zero-copy, you should consider other mechanism when you need to deal with large-file-transfer, such as using a separated BIO channel
     *   If a RuntimeException was thrown in this function, the channel would be closed
     */
    void encode(WriteBuffer writeBuffer, Object o);
}
