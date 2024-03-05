package cn.zorcc.common.network;

import cn.zorcc.common.structure.ReadBuffer;

import java.util.List;

/**
 *   Channel Decoder interface determines how the data received could be transformed into a new created Java Object for parsing
 */
@FunctionalInterface
public interface Decoder {

    /**
     *   Decode a memory-segment into an object, could return null indicating that the readBuffer is currently incomplete for parsing
     *   If readBuffer's readerIndex is not equal to writeIndex after decoding, or null is returned, which means there are still some data remaining in the socket
     *   In that case remaining-data will be retrieved in the next loop, the buffer will be shortly cached in channel's local storage
     *   This function would be invoked only in poller thread, developers should take care about not blocking the poller thread by doing time-consuming logic
     *   If a RuntimeException was thrown in this function, possibly a corrupted request format or decoding logic, the channel would be closed
     */
    void decode(ReadBuffer readBuffer, List<Object> entityList);
}
