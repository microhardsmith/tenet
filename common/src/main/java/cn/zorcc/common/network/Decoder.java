package cn.zorcc.common.network;

import cn.zorcc.common.ReadBuffer;

@FunctionalInterface
public interface Decoder {

    /**
     *   Decode a memory-segment into a object, could return null indicates the readBuffer is not big enough for reading
     *   if after decoding, readBuffer's readerIndex is not equal to writeIndex, that means there are some data remaining in the socket, then they will be retrieved in the next loop
     *   This function runs in channel's writer thread
     */
    Object decode(ReadBuffer readBuffer);
}
