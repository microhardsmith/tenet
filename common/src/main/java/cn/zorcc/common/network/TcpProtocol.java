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
    private static final Native n = Native.n;

    @Override
    public void canRead(Channel channel, MemorySegment segment) {
        long len = segment.byteSize();
        long received = n.recv(channel.socket(), segment, len);
        if(received > Constants.ZERO) {
            channel.onReadBuffer(new ReadBuffer(received == len ? segment : segment.asSlice(Constants.ZERO, received)));
        }else {
            if(received < Constants.ZERO) {
                // usually connection reset by peer
                log.debug(STR."\{channel.loc()} tcp recv err, errno : \{n.errno()}");
            }
            channel.close();
        }
    }

    @Override
    public void canWrite(Channel channel) {
        if(channel.state().compareAndSet(Native.REGISTER_READ_WRITE, Native.REGISTER_READ)) {
            n.ctl(channel.worker().mux(), channel.socket(), Native.REGISTER_READ_WRITE, Native.REGISTER_READ);
            channel.worker().submitWriterTask(new WriterTask(WriterTask.WriterTaskType.WRITABLE, channel, null, null));
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public WriteStatus doWrite(Channel channel, WriteBuffer writeBuffer) {
        Socket socket = channel.socket();
        MemorySegment segment = writeBuffer.toSegment();
        long len = segment.byteSize();
        long bytes = n.send(socket, segment, len);
        if(bytes == -1L) {
            // error occurred
            int errno = n.errno();
            if(errno == n.sendBlockCode()) {
                // current TCP write buffer is full, wait until writable again
                if(channel.state().compareAndSet(Native.REGISTER_READ, Native.REGISTER_READ_WRITE)) {
                    n.ctl(channel.worker().mux(), socket, Native.REGISTER_READ, Native.REGISTER_READ_WRITE);
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
        n.shutdownWrite(channel.socket());
    }

    @Override
    public void doClose(Channel channel) {
        n.closeSocket(channel.socket());
    }
}
