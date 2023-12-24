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
        int received = osNetworkLibrary.recv(channel.socket(), reserved, len);
        if(received < 0) {
            throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to perform recv(), errno : \{osNetworkLibrary.errno()}");
        }else {
            return received;
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
        if(r == -1) {
            int errno = osNetworkLibrary.errno();
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
        if(osNetworkLibrary.shutdownWrite(channel.socket()) != 0) {
            throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to perform shutdown(), errno : \{osNetworkLibrary.errno()}");
        }
    }


    @Override
    public void doClose() {
        if(osNetworkLibrary.closeSocket(channel.socket()) != 0) {
            throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to close socket, errno : \{osNetworkLibrary.errno()}");
        }
    }

}
