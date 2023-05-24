package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.wheel.Job;
import lombok.extern.slf4j.Slf4j;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class SslConnector implements Connector {
    private static final Native n = Native.n;
    private static final int INITIAL = 0;
    private static final int WANT_READ = 1;
    private static final int WANT_WRITE = 2;
    /**
     *   determine whether the connector was from client-side
     */
    private final boolean clientSide;
    private final MemorySegment ssl;
    private final AtomicBoolean race = new AtomicBoolean(false);
    private final AtomicInteger status = new AtomicInteger(INITIAL);

    public SslConnector(boolean clientSide, MemorySegment ssl) {
        this.clientSide = clientSide;
        this.ssl = ssl;
    }

    @Override
    public void shouldCancel(Socket socket) {
        if(race.compareAndSet(false, true)) {
            shouldClose(socket);
        }
    }

    @Override
    public void shouldClose(Socket socket) {
        Openssl.sslFree(ssl);
        n.closeSocket(socket);
    }

    @Override
    public void shouldRead(Acceptor acceptor) {
        int currentStatus = status.get();
        if (currentStatus == WANT_READ) {
            doHandshake(acceptor);
        } else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public void shouldWrite(Acceptor acceptor) {
        int currentStatus = status.get();
        if (currentStatus == INITIAL) {
            Socket socket = acceptor.socket();
            int errOpt = n.getErrOpt(acceptor.socket());
            if (errOpt == 0) {
                // whether cancel succeed or fail will not matter, race will make sure only one succeed
                acceptor.cancelTimeout();
                Openssl.sslSetFd(ssl, socket.intValue());
                doHandshake(acceptor);
            } else {
                log.error("Failed to establish ssl connection, errno : {}", n.errno());
                acceptor.unbind();
            }
        } else if (currentStatus == WANT_WRITE) {
            doHandshake(acceptor);
        } else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    /**
     *   Performing actual SSL handshake
     */
    private void doHandshake(Acceptor acceptor) {
        int r = clientSide ? Openssl.sslConnect(ssl) : Openssl.sslAccept(ssl);
        if(r == 1) {
            acceptor.toChannel(new SslProtocol(clientSide, ssl));
        }else if(r <= 0) {
            int err = Openssl.sslGetErr(ssl, r);
            if(err == Constants.SSL_ERROR_WANT_READ) {
                status.set(WANT_READ);
                Native.tryRegisterRead(acceptor.state(), acceptor.worker().state().mux(), acceptor.socket());
            }else if(err == Constants.SSL_ERROR_WANT_WRITE) {
                status.set(WANT_WRITE);
                Native.tryRegisterWrite(acceptor.state(), acceptor.worker().state().mux(), acceptor.socket());
            }else {
                log.error("Failed to perform ssl handshake, err : {}", err);
                acceptor.unbind();
            }
        }
    }
}
