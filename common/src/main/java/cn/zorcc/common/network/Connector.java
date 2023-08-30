package cn.zorcc.common.network;

import java.lang.foreign.MemorySegment;

/**
 *   Network connector interface, define the behavior of the acceptor
 *   Note that since shouldRead() and shouldWrite() both happen in the same worker thread, so no need for external synchronization
 */
public interface Connector {
    /**
     *   Perform the actual close operation, this method should only be invoked by Acceptor.close() in its worker's reader thread
     */
    void doClose(Acceptor acceptor);

    /**
     *   Indicating that current acceptor could read from socket, this method should only be invoked in its worker's reader thread
     */
    void canRead(Acceptor acceptor, MemorySegment buffer);

    /**
     *   Indicating that current acceptor could read from socket, this method should only be invoked in its worker's reader thread
     */
    void canWrite(Acceptor acceptor);
}
