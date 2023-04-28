package cn.zorcc.common.network;

import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.WriteBuffer;

/**
 *   Network protocol interface, protocol determines the operation that master and worker will perform on the target channel
 *   Default will be TcpProtocol, if using TLS, then SslProtocol could be used.
 */
public interface Protocol {
    /**
     *   If the master server socket detect read operation, this function would be invoked
     *   Note that if the socket connection is in-progress, master thread will register its write event so masterCanWrite() could detect it
     */
    void canAccept(Channel channel);

    /**
     *   If Net.connect() successfully connected to the remote peer, this function would be invoked
     *   Note that if the socket connection is in-progress, master thread will register its write event so masterCanWrite() could detect it
     */
    void canConnect(Channel channel);

    /**
     *   Indicates that master can now read from this channel
     */
    void masterCanRead(Channel channel);

    /**
     *   Indicates that master can now write to this channel
     */
    void masterCanWrite(Channel channel);

    /**
     *   Indicates that worker can now read from this channel
     */
    void workerCanRead(Channel channel, ReadBuffer readBuffer);

    /**
     *   Indicates that worker can now write to this channel
     */
    void workerCanWrite(Channel channel);

    /**
     *   Perform the actual write operation
     */
    void doWrite(WriteBuffer writeBuffer);
}
