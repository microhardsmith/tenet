package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *   Connector for normal TCP connection
 */
public class TcpConnector implements Connector {
    private static final Logger log = LoggerFactory.getLogger(TcpConnector.class);
    private static final Native n = Native.n;

    @Override
    public void doClose(Acceptor acceptor) {
        n.closeSocket(acceptor.socket());
    }

    @Override
    public void shouldRead(Acceptor acceptor) {
        throw new FrameworkException(ExceptionType.NETWORK , Constants.UNREACHED);
    }

    @Override
    public void shouldWrite(Acceptor acceptor) {
        int errOpt = n.getErrOpt(acceptor.socket());
        if(errOpt == 0) {
            acceptor.toChannel(new TcpProtocol());
        }else {
            log.error("Failed to establish tcp connection, errno : {}", errOpt);
            acceptor.close();
        }
    }
}
