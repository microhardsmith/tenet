package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import lombok.extern.slf4j.Slf4j;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class SslConnector implements Connector {
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
        Openssl.sslFree(ssl);
        n.closeSocket(acceptor.socket());
    }

    @Override
    public void shouldRead(Acceptor acceptor) {
        int currentStatus = status.get();
        if (currentStatus == WANT_READ) {
            unregisterState(acceptor, Native.REGISTER_READ);
            doHandshake(acceptor);
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public void shouldWrite(Acceptor acceptor) {
        int currentStatus = status.get();
        if (currentStatus == INITIAL) {
            unregisterState(acceptor, Native.REGISTER_WRITE);
            Socket socket = acceptor.socket();
            int errOpt = n.getErrOpt(acceptor.socket());
            if(errOpt == 0) {
                int r = Openssl.sslSetFd(ssl, socket.intValue());
                if(r == 1) {
                    doHandshake(acceptor);
                }else {
                    log.error("Failed to set fd for ssl, err : {}", Openssl.sslGetErr(ssl, r));
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
     *   Unregister read or write state from current acceptor
     */
    private void unregisterState(Acceptor acceptor, int state) {
        int current = acceptor.state().getAndUpdate(i -> i ^ state);
        if((current & state) != 0) {
            n.ctl(acceptor.worker().mux(), acceptor.socket(), current, current ^ state);
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    /**
     *   Performing actual SSL handshake
     */
    private void doHandshake(Acceptor acceptor) {
        int r = clientSide ? Openssl.sslConnect(ssl) : Openssl.sslAccept(ssl);
        if(r == 1) {
            acceptor.toChannel(new SslProtocol(ssl));
        }else if(r <= 0) {
            int err = Openssl.sslGetErr(ssl, r);
            if(err == Constants.SSL_ERROR_WANT_READ) {
                status.set(WANT_READ);
                int current = acceptor.state().getAndUpdate(i -> (i & Native.REGISTER_READ) == 0 ? i + Native.REGISTER_READ : i);
                if((current & Native.REGISTER_READ) == 0) {
                    n.ctl(acceptor.worker().mux(), acceptor.socket(), current, current + Native.REGISTER_READ);
                }
            }else if(err == Constants.SSL_ERROR_WANT_WRITE) {
                status.set(WANT_WRITE);
                int current = acceptor.state().getAndUpdate(i -> (i & Native.REGISTER_WRITE) == 0 ? i + Native.REGISTER_WRITE : i);
                if((current & Native.REGISTER_WRITE) == 0) {
                    n.ctl(acceptor.worker().mux(), acceptor.socket(), current, current + Native.REGISTER_READ);
                }
            }else {
                log.error("Failed to perform ssl handshake, ssl err : {}", err);
                acceptor.close();
            }
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }
}
