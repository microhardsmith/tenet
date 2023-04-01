package cn.zorcc.common.network;

import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.WriteBuffer;

import java.lang.foreign.MemorySegment;
import java.util.Optional;

/**
 *   Network codec interface
 *   Net should always provide server-side channel with a default codec
 *   Client can assign a target codec when establishing connections
 */
public interface Codec {

    /**
     *   Encode a specific object into a memory-segment
     *   This function runs in worker thread
     */
    void encode(WriteBuffer buffer, Object o);

    /**
     *   Decode a memory-segment into a object, could be null
     *   if after decoding, readBuffer's readerIndex is not equal to writeIndex, that means there is data remains in the socket, will be retrieved in the next loop
     *   This function runs in worker thread
     */
    Object decode(ReadBuffer readBuffer);
}
