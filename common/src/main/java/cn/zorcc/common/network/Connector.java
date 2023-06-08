package cn.zorcc.common.network;

/**
 *   Network connector interface, define the behavior of the acceptor
 *   Note that since shouldRead() and shouldWrite() both happen in the same worker thread, so no need for external synchronization
 */
public interface Connector {
    /**
     *   Perform the actual close operation, this method should only be invoked by Acceptor.close() in its worker thread
     */
    void doClose(Acceptor acceptor);

    /**
     *   Indicating that current acceptor could read from socket, this method should only be invoked in its worker thread
     */
    void shouldRead(Acceptor acceptor);

    /**
     *   Indicating that current acceptor could read from socket, this method should only be invoked in its worker thread
     */
    void shouldWrite(Acceptor acceptor);
}
