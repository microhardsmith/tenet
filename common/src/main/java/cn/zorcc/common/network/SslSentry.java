package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.State;
import cn.zorcc.common.bindings.SslBinding;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.network.api.Protocol;
import cn.zorcc.common.network.api.Sentry;
import cn.zorcc.common.network.lib.OsNetworkLibrary;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.MemorySegment;

public record SslSentry(
        Channel channel,
        boolean clientSide,
        MemorySegment ssl,
        State sslState
) implements Sentry {
    private static final OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;
    private static final int WANT_READ = 1 << 1;
    private static final int WANT_WRITE = 1 << 2;
    public SslSentry(Channel channel, boolean clientSide, MemorySegment ssl) {
        this(channel, clientSide, ssl, new State(Constants.INITIAL));
    }

    @Override
    public int onReadableEvent(MemorySegment reserved, int len) {
        if(sslState.unregister(WANT_READ)) {
            return handshake();
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public int onWritableEvent() {
        if(sslState.unregister(WANT_WRITE)) {
            return handshake();
        }else {
            Socket socket = channel.socket();
            int errOpt = osNetworkLibrary.getErrOpt(socket);
            if(errOpt == 0) {
                int r = SslBinding.sslSetFd(ssl, socket.intValue());
                if(r == 1) {
                    return handshake();
                }else {
                    return SslBinding.throwException(SslBinding.sslGetErr(ssl, r), "SSL_set_fd()");
                }
            }else {
                throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to establish connection, err opt : \{errOpt}");
            }
        }
    }

    @Override
    public Protocol toProtocol() {
        return new SslProtocol(channel, ssl, sslState);
    }

    @Override
    public void doClose() {
        SslBinding.sslFree(ssl);
        if(osNetworkLibrary.closeSocket(channel.socket()) != 0) {
            throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to close socket, errno : \{osNetworkLibrary.errno()}");
        }
    }

    private int handshake() {
        int r = clientSide ? SslBinding.sslConnect(ssl) : SslBinding.sslAccept(ssl);
        if(r == 1) {
            return verifyCertificate();
        }else {
            int err = SslBinding.sslGetErr(ssl, r);
            if(err == Constants.SSL_ERROR_WANT_READ) {
                sslState.register(WANT_READ);
                return Constants.NET_R;
            }else if(err == Constants.SSL_ERROR_WANT_WRITE) {
                sslState.register(WANT_WRITE);
                return Constants.NET_W;
            }else {
                return SslBinding.throwException(err, "SSL_handshake()");
            }
        }
    }

    private int verifyCertificate() {
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
        return Constants.NET_UPDATE;
    }
}
