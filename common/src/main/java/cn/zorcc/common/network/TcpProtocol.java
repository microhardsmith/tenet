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
    private final Receiver receiver;
    private final Channel channel;

    public TcpProtocol(Receiver receiver) {
        this.receiver = receiver;
        this.channel = receiver.getChannel();
    }

    @Override
    public void canRead(MemorySegment reserved) {
        int len = (int) reserved.byteSize();
        int received = osNetworkLibrary.recv(channel.socket(), reserved, len);
        if(received > 0) {
            receiver.onChannelBuffer(new ReadBuffer(received == len ? reserved : reserved.asSlice(0, received)));
        }else {
            if(received < 0) {
                log.warn(STR."\{channel.loc()} tcp recv err, errno : \{ osNetworkLibrary.errno()}");
            }
            receiver.close();
        }
    }

    @Override
    public void canWrite() {
        if(channel.state().compareAndSet(OsNetworkLibrary.REGISTER_READ_WRITE, OsNetworkLibrary.REGISTER_READ)) {
            osNetworkLibrary.ctl(channel.worker().mux(), channel.socket(), OsNetworkLibrary.REGISTER_READ_WRITE, OsNetworkLibrary.REGISTER_READ);
            channel.worker().submitWriterTask(new WriterTask(WriterTask.WriterTaskType.WRITABLE, channel, null, null));
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public void doClose() {
        osNetworkLibrary.closeSocket(channel.socket());
    }

    @Override
    public WriteStatus doWrite(WriteBuffer writeBuffer) {
        Socket socket = channel.socket();
        MemorySegment segment = writeBuffer.toSegment();
        int len = (int) segment.byteSize();
        int bytes = osNetworkLibrary.send(socket, segment, len);
        if(bytes == -1) {
            int errno = osNetworkLibrary.errno();
            if(errno == osNetworkLibrary.sendBlockCode()) {
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
            return doWrite(writeBuffer.truncate(bytes));
        }
        return WriteStatus.SUCCESS;
    }

    @Override
    public void doShutdown() {
        osNetworkLibrary.shutdownWrite(channel.socket());
    }
}
