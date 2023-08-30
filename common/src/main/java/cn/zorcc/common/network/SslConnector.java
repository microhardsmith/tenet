package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *   Connector using SSL encryption
 */
public class SslConnector implements Connector {
    private static final Logger log = LoggerFactory.getLogger(SslConnector.class);
    private static final Native n = Native.n;
    private static final int INITIAL = 0;
    private static final int WANT_READ = 1;
    private static final int WANT_WRITE = 2;
    private final boolean clientSide;
    private final MemorySegment ssl;
    private final AtomicInteger status = new AtomicInteger(INITIAL);

    public SslConnector(boolean clientSide, MemorySegment ssl) {
        this.clientSide = clientSide;
        this.ssl = ssl;
    }

    @Override
    public void doClose(Acceptor acceptor) {
        Ssl.sslFree(ssl);
        n.closeSocket(acceptor.socket());
    }

    @Override
    public void canRead(Acceptor acceptor, MemorySegment buffer) {
        int currentStatus = status.get();
        if (currentStatus == WANT_READ) {
            unregisterState(acceptor, Native.REGISTER_READ);
            doHandshake(acceptor);
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public void canWrite(Acceptor acceptor) {
        int currentStatus = status.get();
        if (currentStatus == INITIAL) {
            unregisterState(acceptor, Native.REGISTER_WRITE);
            Socket socket = acceptor.socket();
            int errOpt = n.getErrOpt(acceptor.socket());
            if(errOpt == 0) {
                int r = Ssl.sslSetFd(ssl, socket.intValue());
                if(r == 1) {
                    doHandshake(acceptor);
                }else {
                    log.error("Failed to set fd for ssl, err : {}", Ssl.sslGetErr(ssl, r));
                    acceptor.close();
                }
            }else {
                log.error("Failed to establish ssl connection, errno : {}", n.errno());
                acceptor.close();
            }
        }else if (currentStatus == WANT_WRITE) {
            unregisterState(acceptor, Native.REGISTER_WRITE);
            doHandshake(acceptor);
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    /**
     *   Register read or write state from target acceptor
     */
    private static void registerState(Acceptor acceptor, int val) {
        int current = acceptor.state().getAndUpdate(i -> (i & val) == 0 ? i + val : i);
        if((current & val) == 0) {
            n.ctl(acceptor.worker().mux(), acceptor.socket(), current, current + val);
        }
    }

    /**
     *   Unregister read or write state from target acceptor
     */
    private static void unregisterState(Acceptor acceptor, int val) {
        int current = acceptor.state().getAndUpdate(i -> (i & val) != 0 ? i - val : i);
        if((current & val) != 0) {
            n.ctl(acceptor.worker().mux(), acceptor.socket(), current, current - val);
        }
    }

    /**
     *   Performing actual SSL handshake
     */
    private void doHandshake(Acceptor acceptor) {
        int r = clientSide ? Ssl.sslConnect(ssl) : Ssl.sslAccept(ssl);
        if(r == 1) {
            acceptor.toChannel(new SslProtocol(ssl));
        }else if(r <= 0) {
            int err = Ssl.sslGetErr(ssl, r);
            if(err == Constants.SSL_ERROR_WANT_READ) {
                status.set(WANT_READ);
                registerState(acceptor, Native.REGISTER_READ);
            }else if(err == Constants.SSL_ERROR_WANT_WRITE) {
                status.set(WANT_WRITE);
                registerState(acceptor, Native.REGISTER_WRITE);
            }else {
                log.error("Failed to perform ssl handshake, ssl err : {}", err);
                acceptor.close();
            }
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }
}
