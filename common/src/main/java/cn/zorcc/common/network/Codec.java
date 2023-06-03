package cn.zorcc.common.network;

import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.WriteBuffer;

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
    void encode(WriteBuffer writeBuffer, Object o);

    /**
     *   Decode a memory-segment into a object, could return null indicates the readBuffer is not big enough for reading
     *   if after decoding, readBuffer's readerIndex is not equal to writeIndex, that means there are some data remaining in the socket, then they will be retrieved in the next loop
     *   This function runs in worker thread
     */
    Object decode(ReadBuffer readBuffer);
}
