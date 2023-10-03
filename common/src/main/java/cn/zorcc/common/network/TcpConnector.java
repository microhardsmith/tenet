package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;

import java.lang.foreign.MemorySegment;

/**
 *   Connector for normal TCP connection
 */
public class TcpConnector implements Connector {
    private static final Logger log = new Logger(TcpConnector.class);
    private static final OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;

    @Override
    public void doClose(Acceptor acceptor) {
        osNetworkLibrary.closeSocket(acceptor.socket());
    }

    @Override
    public void canRead(Acceptor acceptor, MemorySegment buffer) {
        throw new FrameworkException(ExceptionType.NETWORK , Constants.UNREACHED);
    }

    @Override
    public void canWrite(Acceptor acceptor) {
        int errOpt = osNetworkLibrary.getErrOpt(acceptor.socket());
        if(errOpt == Constants.ZERO) {
            acceptor.toChannel(new TcpProtocol());
        }else {
            log.error(STR."Failed to establish tcp connection, errno : \{errOpt}");
            acceptor.close();
        }
    }
}
