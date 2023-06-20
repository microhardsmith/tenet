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
     *   This method will only be invoked in worker's reader thread
     */
    void canRead(Channel channel, ReadBuffer readBuffer);

    /**
     *   Indicates that worker can now write to this channel
     *   This method will only be invoked in worker's reader thread
     */
    void canWrite(Channel channel);

    /**
     *   Perform the actual write operation, return the actual bytes written, if not bytes written then return 0, if write err occurred then return -1
     *   This method will only be invoked in worker's writer thread, if not successfully written, the protocol itself is due to register write events
     */
    WriteStatus doWrite(Channel channel, WriteBuffer writeBuffer);

    /**
     *   Perform the actual shutdown operation, a timeout close would be scheduled for closing the socket to ensure a total release.
     *   Note that shutdown current channel doesn't block the recv operation, remote peer will recv EOF and ought to close the socket
     *   This method could only be invoked from worker's writer thread
     */
    void doShutdown(Channel channel);

    /**
     *   Perform the actual close operation, this method is a internal implementation, and it should never be directly called by developers
     *   Actual close operation could only be invoked in workers reader thread
     */
    void doClose(Channel channel);

    enum WriteStatus {
        SUCCESS,
        PENDING,
        FAILURE
    }
}
