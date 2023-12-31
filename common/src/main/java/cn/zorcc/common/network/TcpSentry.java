package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.api.Protocol;
import cn.zorcc.common.network.api.Sentry;
import cn.zorcc.common.network.lib.OsNetworkLibrary;

import java.lang.foreign.MemorySegment;

public record TcpSentry(
        Channel channel
) implements Sentry {
    private static final OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;

    @Override
    public int onReadableEvent(MemorySegment reserved, int len) {
        throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
    }

    @Override
    public int onWritableEvent() {
        int errOpt = osNetworkLibrary.getErrOpt(channel.socket());
        if(errOpt == 0) {
            return Constants.NET_UPDATE;
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to establish connection, err opt : \{errOpt}");
        }
    }

    @Override
    public Protocol toProtocol() {
        return new TcpProtocol(channel);
    }

    @Override
    public void doClose() {
        int r = osNetworkLibrary.closeSocket(channel.socket());
        if(r < 0) {
            throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to close socket, errno : \{Math.abs(r)}");
        }
    }
}
