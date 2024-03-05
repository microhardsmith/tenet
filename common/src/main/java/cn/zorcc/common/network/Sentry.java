package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.bindings.SslBinding;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.structure.IntHolder;
import cn.zorcc.common.util.NativeUtil;
import cn.zorcc.common.util.SslUtil;

import java.lang.foreign.MemorySegment;

/**
 *   Sentry determines how a channel could upgrade to its Protocol
 *   All the methods will be executed in poller thread only, so there is no need to add external lock on it
 */
public interface Sentry {
    OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;
    /**
     *   This function would be invoked when channel become readable
     *   the parameter len will always be the exact length of data segment
     *   return an int flag to indicate a state change
     */
    int onReadableEvent(MemorySegment reserved, long len);

    /**
     *   This function would be invoked when channel become writable
     *   return an int flag to indicate a state change
     */
    int onWritableEvent();

    /**
     *   Update current sentry to protocol
     */
    Protocol toProtocol();

    /**
     *   This function would be invoked when Sentry is being closed
     *   Node that when sentry was upgrade to protocol, this function would not be invoked, which means doClose() may never get executed at all
     */
    void doClose();

    static Sentry newTcpSentry(Channel channel) {
        return new TcpSentry(channel);
    }

    record TcpSentry(
            Channel channel
    ) implements Sentry {

        @Override
        public int onReadableEvent(MemorySegment reserved, long len) {
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
            return new Protocol.TcpProtocol(channel);
        }

        @Override
        public void doClose() {
            int r = osNetworkLibrary.closeSocket(channel.socket());
            if(r < 0) {
                throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to close socket, errno : \{Math.abs(r)}");
            }
        }
    }

    static Sentry newSslSentry(Channel channel, boolean clientSide, MemorySegment ssl) {
        return new SslSentry(channel, clientSide, ssl, new IntHolder(0));
    }

    record SslSentry (
            Channel channel,
            boolean clientSide,
            MemorySegment ssl,
            IntHolder sslState
    ) implements Sentry {
        private static final int WANT_READ = 1 << 1;
        private static final int WANT_WRITE = 1 << 2;

        @Override
        public int onReadableEvent(MemorySegment reserved, long len) {
            int current = sslState.getValue();
            if((current & WANT_READ) != 0) {
                sslState.setValue(current & ~(WANT_READ));
                return handshake();
            }else {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
        }

        @Override
        public int onWritableEvent() {
            int current = sslState.getValue();
            if((current & WANT_WRITE) != 0) {
                sslState.setValue(current & ~(WANT_WRITE));
                return handshake();
            }else {
                Socket socket = channel.socket();
                int errOpt = osNetworkLibrary.getErrOpt(socket);
                if(errOpt == 0) {
                    int r = SslBinding.sslSetFd(ssl, socket.intValue());
                    if(r == 1) {
                        return handshake();
                    }else {
                        return SslUtil.throwException(SslBinding.sslGetErr(ssl, r), "SSL_set_fd()");
                    }
                }else {
                    throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to establish connection, err opt : \{errOpt}");
                }
            }
        }

        @Override
        public Protocol toProtocol() {
            return Protocol.newSslProtocol(channel, ssl, sslState);
        }

        @Override
        public void doClose() {
            SslBinding.sslFree(ssl);
            int r = osNetworkLibrary.closeSocket(channel.socket());
            if(r < 0) {
                throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to close socket, errno : \{Math.abs(r)}");
            }
        }

        private int handshake() {
            int r = clientSide ? SslBinding.sslConnect(ssl) : SslBinding.sslAccept(ssl);
            if(r == 1) {
                verifyCertificate();
                return Constants.NET_UPDATE;
            }else {
                int err = SslBinding.sslGetErr(ssl, r);
                if(err == Constants.SSL_ERROR_WANT_READ) {
                    sslState.setValue(sslState().getValue() | WANT_READ);
                    return Constants.NET_R;
                }else if(err == Constants.SSL_ERROR_WANT_WRITE) {
                    sslState.setValue(sslState().getValue() | WANT_WRITE);
                    return Constants.NET_W;
                }else {
                    return SslUtil.throwException(err, "SSL_handshake()");
                }
            }
        }

        private void verifyCertificate() {
            if(clientSide) {
                MemorySegment x509 = SslBinding.sslGetPeerCertificate(ssl);
                if(NativeUtil.checkNullPointer(x509)) {
                    throw new FrameworkException(ExceptionType.NETWORK, "Server certificate not provided");
                }
                long verifyResult = SslBinding.sslGetVerifyResult(ssl);
                if(verifyResult != 0) {
                    throw new FrameworkException(ExceptionType.NETWORK, STR."Server certificate cannot be verified, verify result : \{verifyResult}");
                }
                SslBinding.x509Free(x509);
            }
        }
    }
}
