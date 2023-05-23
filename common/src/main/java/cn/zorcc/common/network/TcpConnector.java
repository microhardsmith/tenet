package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 *   Acceptor for Tcp protocol
 */
@Slf4j
public class TcpConnector implements Connector {
    private static final Native n = Native.n;
    private final AtomicBoolean race = new AtomicBoolean(false);
    @Override
    public void shouldCancel(Socket socket) {
        if(race.compareAndSet(false, true)) {
            shouldClose(socket);
        }
    }

    @Override
    public void shouldClose(Socket socket) {
        n.closeSocket(socket);
    }

    @Override
    public void shouldRead(Acceptor acceptor) {
        throw new FrameworkException(ExceptionType.NETWORK , Constants.UNREACHED);
    }

    @Override
    public void shouldWrite(Acceptor acceptor) {
        if(race.compareAndSet(false, true)) {
            int errOpt = n.getErrOpt(acceptor.socket());
            if(errOpt == 0) {
                // whether cancel succeed or fail will not matter, race will make sure only one succeed
                acceptor.cancelJob().cancel();
                acceptor.toChannel(new TcpProtocol());
            }else {
                log.error("Failed to establish tcp connection, errno : {}", n.errno());
                acceptor.unbind();
            }
        }
    }
}
