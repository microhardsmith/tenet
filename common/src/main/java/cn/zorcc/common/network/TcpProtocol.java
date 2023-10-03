package cn.zorcc.common.network;


import cn.zorcc.common.Constants;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;

import java.lang.foreign.MemorySegment;

/**
 *   Protocol for normal TCP connection
 */
public final class TcpProtocol implements Protocol {
    private static final Logger log = new Logger(TcpProtocol.class);
    private static final OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;

    @Override
    public void canRead(Channel channel, MemorySegment segment) {
        int len = (int) segment.byteSize();
        int received = osNetworkLibrary.recv(channel.socket(), segment, len);
        if(received > Constants.ZERO) {
            channel.onReadBuffer(new ReadBuffer(received == len ? segment : segment.asSlice(Constants.ZERO, received)));
        }else {
            if(received < Constants.ZERO) {
                // usually connection reset by peer
                log.debug(STR."\{channel.loc()} tcp recv err, errno : \{ osNetworkLibrary.errno()}");
            }
            channel.close();
        }
    }

    @Override
    public void canWrite(Channel channel) {
        if(channel.state().compareAndSet(OsNetworkLibrary.REGISTER_READ_WRITE, OsNetworkLibrary.REGISTER_READ)) {
            osNetworkLibrary.ctl(channel.worker().mux(), channel.socket(), OsNetworkLibrary.REGISTER_READ_WRITE, OsNetworkLibrary.REGISTER_READ);
            channel.worker().submitWriterTask(new WriterTask(WriterTask.WriterTaskType.WRITABLE, channel, null, null));
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public WriteStatus doWrite(Channel channel, WriteBuffer writeBuffer) {
        Socket socket = channel.socket();
        MemorySegment segment = writeBuffer.toSegment();
        int len = (int) segment.byteSize();
        int bytes = osNetworkLibrary.send(socket, segment, len);
        if(bytes == -1) {
            // error occurred
            int errno = osNetworkLibrary.errno();
            if(errno == osNetworkLibrary.sendBlockCode()) {
                // current TCP write buffer is full, wait until writable again
                if(channel.state().compareAndSet(OsNetworkLibrary.REGISTER_READ, OsNetworkLibrary.REGISTER_READ_WRITE)) {
                    osNetworkLibrary.ctl(channel.worker().mux(), socket, OsNetworkLibrary.REGISTER_READ, OsNetworkLibrary.REGISTER_READ_WRITE);
                    return WriteStatus.PENDING;
                }else {
                    throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
                }
            }else {
                log.error(STR."Failed to write, errno : \{errno}");
                return WriteStatus.FAILURE;
            }
        }else if(bytes < len){
            // only write a part of the segment, wait for next loop
            return doWrite(channel, writeBuffer.truncate(bytes));
        }
        return WriteStatus.SUCCESS;
    }

    @Override
    public void doShutdown(Channel channel) {
        osNetworkLibrary.shutdownWrite(channel.socket());
    }

    @Override
    public void doClose(Channel channel) {
        osNetworkLibrary.closeSocket(channel.socket());
    }
}
