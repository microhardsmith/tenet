package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.api.Protocol;
import cn.zorcc.common.network.lib.OsNetworkLibrary;

import java.lang.foreign.MemorySegment;

public record TcpProtocol(
        Channel channel
) implements Protocol {
    private static final OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;

    @Override
    public long onReadableEvent(MemorySegment reserved, long len) {
        long r = osNetworkLibrary.recv(channel.socket(), reserved, len);
        if(r < 0L) {
            throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to perform recv(), errno : \{Math.abs(r)}");
        }else {
            return r;
        }
    }

    @Override
    public long onWritableEvent() {
        channel.writer().submit(new WriterTask(WriterTaskType.WRITABLE, channel, null, null));
        return Constants.NET_R;
    }

    @Override
    public long doWrite(MemorySegment data, long len) {
        Socket socket = channel.socket();
        long r = osNetworkLibrary.send(socket, data, len);
        if(r < 0L) {
            int errno = Math.toIntExact(-r);
            if(errno == osNetworkLibrary.sendBlockCode()) {
                return Constants.NET_PW;
            }else {
                throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to perform send(), errno : \{errno}");
            }
        }else {
            return r;
        }
    }

    @Override
    public void doShutdown() {
        int r = osNetworkLibrary.shutdownWrite(channel.socket());
        if(r < 0) {
            throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to perform shutdown(), errno : \{Math.abs(r)}");
        }
    }


    @Override
    public void doClose() {
        int r = osNetworkLibrary.closeSocket(channel.socket());
        if(r < 0) {
            throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to close socket, errno : \{Math.abs(r)}");
        }
    }

}
