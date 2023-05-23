package cn.zorcc.common.network;

import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.WriteBuffer;

import java.util.concurrent.TimeUnit;

/**
 *   Network protocol interface, protocol determines the operation that master and worker will perform on the target channel
 *   Default will be TcpProtocol, if using TLS, then SslProtocol could be used.
 */
public interface Protocol {

    /**
     *   if current channel is available (could recv, send and not shutdown)
     */
    boolean available();
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
    void doShutdown(Channel channel, long timeout, TimeUnit timeUnit);

    /**
     *   Perform the actual close operation, this method is a internal procedure made for convenience
     *   in fact, there should be two scenarios where doClose() would be invoked:
     *   1. a normally shutdown() was called (PS: could be from both sides) and the worker thread read EOF from peer, then doClose() would be invoked to release the fd
     *   2. after a doShutdown() called, the remote peer is still not willing to close the connection, which is abnormal and could be an attack, the WheelJob will close the fd anyway after a timeout
     */
    void doClose(Channel channel);
}
