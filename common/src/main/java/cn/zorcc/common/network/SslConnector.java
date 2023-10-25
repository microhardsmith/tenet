package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.binding.SslBinding;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;

import java.lang.foreign.MemorySegment;

/**
 *   Connector using SSL encryption
 */
public final class SslConnector implements Connector {
    private static final Logger log = new Logger(SslConnector.class);
    private static final OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;
    private static final int INITIAL = 0;
    private static final int WANT_READ = 1;
    private static final int WANT_WRITE = 2;
    private final Receiver receiver;
    private final Channel channel;
    private final boolean clientSide;
    private final MemorySegment ssl;
    private int status = INITIAL;

    public SslConnector(Receiver receiver, boolean clientSide, MemorySegment ssl) {
        this.receiver = receiver;
        this.channel = receiver.getChannel();
        this.clientSide = clientSide;
        this.ssl = ssl;
    }

    @Override
    public void canRead(MemorySegment buffer) {
        if (status == WANT_READ) {
            unregisterState(OsNetworkLibrary.REGISTER_READ);
            doHandshake();
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public void canWrite() {
        if (status == INITIAL) {
            unregisterState(OsNetworkLibrary.REGISTER_WRITE);
            Socket socket = channel.socket();
            int errOpt = osNetworkLibrary.getErrOpt(socket);
            if(errOpt == 0) {
                int r = SslBinding.sslSetFd(ssl, socket.intValue());
                if(r == 1) {
                    doHandshake();
                }else {
                    log.error(STR."Failed to set fd for ssl, err : \{ SslBinding.sslGetErr(ssl, r)}");
                    receiver.close();
                }
            }else {
                log.error(STR."Failed to establish ssl connection, errno : \{ osNetworkLibrary.errno()}");
                receiver.close();
            }
        }else if (status == WANT_WRITE) {
            unregisterState(OsNetworkLibrary.REGISTER_WRITE);
            doHandshake();
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public void doClose() {
        SslBinding.sslFree(ssl);
        osNetworkLibrary.closeSocket(channel.socket());
    }

    /**
     *   Register read or write state from target acceptor
     */
    private void registerState(int val) {
        int current = channel.state().getAndUpdate(i -> (i & val) == 0 ? i + val : i);
        if((current & val) == 0) {
            osNetworkLibrary.ctl(channel.worker().mux(), channel.socket(), current, current + val);
        }
    }

    /**
     *   Unregister read or write state from target acceptor
     */
    private void unregisterState(int val) {
        int current = channel.state().getAndUpdate(i -> (i & val) != 0 ? i - val : i);
        if((current & val) != 0) {
            osNetworkLibrary.ctl(channel.worker().mux(), channel.socket(), current, current - val);
        }
    }

    /**
     *   Performing actual SSL handshake
     */
    private void doHandshake() {
        int r = clientSide ? SslBinding.sslConnect(ssl) : SslBinding.sslAccept(ssl);
        if(r == 1) {
            receiver.upgradeToChannel(new SslProtocol(receiver, ssl));
        }else if(r <= 0) {
            int err = SslBinding.sslGetErr(ssl, r);
            if(err == Constants.SSL_ERROR_WANT_READ) {
                status = WANT_READ;
                registerState(OsNetworkLibrary.REGISTER_READ);
            }else if(err == Constants.SSL_ERROR_WANT_WRITE) {
                status = WANT_WRITE;
                registerState(OsNetworkLibrary.REGISTER_WRITE);
            }else {
                log.error(STR."Failed to perform ssl handshake, ssl err : \{err}");
                receiver.close();
            }
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }
}
