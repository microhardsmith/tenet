package cn.zorcc.common.network;

import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.WriteBuffer;

/**
 *   Network protocol interface, protocol determines the operation that master and worker will perform on the target channel
 *   Default will be TcpProtocol, if using TLS, then SslProtocol could be used.
 */
public interface Protocol {

    /**
     *   Indicates that worker can now read from this channel
     *   This method will only be invoked in worker thread
     */
    void canRead(Channel channel, ReadBuffer readBuffer);

    /**
     *   Indicates that worker can now write to this channel
     *   This method will only be invoked in worker thread
     */
    void canWrite(Channel channel);

    /**
     *   Perform the actual write operation, when this method returns, the writeBuffer's data are transferred to the OS
     *   This method will only be invoked in writer thread
     */
    void doWrite(Channel channel, WriteBuffer writeBuffer);

    /**
     *   Perform the actual shutdown operation, this method could only be invoked from channel's writerThread
     *   A timeout close would be scheduled for closing the socket to ensure a total release.
     *   Note that shutdown current channel doesn't block the recv operation, remote peer will recv EOF and close the socket
     *   shutdown is not much irrelevant to close operation, since shutdown and recv EOF could both happen at any time
     */
    void doShutdown(Channel channel);

    /**
     *   Perform the actual close operation, this method is a internal procedure made for convenience
     *   Actual close operation should only be invoked in worker thread
     */
    void doClose(Channel channel);
}
