package cn.zorcc.common.network;

import java.lang.foreign.MemorySegment;

/**
 *   Network connector interface, define the behavior of the acceptor
 *   Note that since canRead() and canWrite() both happen in the same reader thread, so no need for external synchronization
 */
public interface Connector {
    /**
     *   Indicates that worker can now read from this channel
     *   This method will only be invoked in worker's reader thread
     */
    void canRead(MemorySegment reserved);

    /**
     *   Indicates that worker can now write to this channel
     *   This method will only be invoked in worker's reader thread
     */
    void canWrite();

    /**
     *   Perform the actual close operation, this method should only be invoked by Acceptor.close() in its worker's reader thread
     */
    void doClose();
}
