package cn.zorcc.common.network;

import cn.zorcc.common.ReadBuffer;

/**
 *   Channel Decoder interface determines how the data received could be transformed into a new created Java Object
 */
@FunctionalInterface
public interface Decoder {

    /**
     *   Decode a memory-segment into a object, could return null indicating that the readBuffer is incomplete for parsing
     *   if after decoding, readBuffer's readerIndex is not equal to writeIndex, that means there are some data remaining in the socket, or null is returned,
     *   then they will be retrieved in the next loop, the buffer will be shortly cached in channel's local storage
     *   This function should only be invoked in its worker's writer thread
     *   If FrameworkException was thrown when decoding, possibly a corrupted request format or decoding logic, channel would be immediately shutdown
     */
    Object decode(ReadBuffer readBuffer);
}
