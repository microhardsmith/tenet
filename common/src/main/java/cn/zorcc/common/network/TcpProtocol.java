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
    public int onReadableEvent(MemorySegment reserved, int len) {
        int r = osNetworkLibrary.recv(channel.socket(), reserved, len);
        if(r < 0) {
            throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to perform recv(), errno : \{Math.abs(r)}");
        }else {
            return r;
        }
    }

    @Override
    public int onWritableEvent() {
        channel.writer().submit(new WriterTask(WriterTaskType.WRITABLE, channel, null, null));
        return Constants.NET_R;
    }

    @Override
    public int doWrite(MemorySegment data, int len) {
        Socket socket = channel.socket();
        int r = osNetworkLibrary.send(socket, data, len);
        if(r < 0) {
            int errno = Math.abs(r);
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
