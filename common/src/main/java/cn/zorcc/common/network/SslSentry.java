package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.bindings.SslBinding;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.api.Protocol;
import cn.zorcc.common.network.api.Sentry;
import cn.zorcc.common.network.lib.OsNetworkLibrary;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.MemorySegment;

public final class SslSentry implements Sentry {
    private static final OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;
    private static final int INITIAL = 0;
    private static final int WANT_READ = 1;
    private static final int WANT_WRITE = 2;
    private final Channel channel;
    private final boolean clientSide;
    private final MemorySegment ssl;
    private int state = INITIAL;

    public SslSentry(Channel channel, boolean clientSide, MemorySegment ssl) {
        this.channel = channel;
        this.clientSide = clientSide;
        this.ssl = ssl;
    }

    @Override
    public int onReadableEvent(MemorySegment reserved, int len) {
        if(state == WANT_READ) {
            return handshake();
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public int onWritableEvent() {
        if(state == INITIAL) {
            Socket socket = channel.socket();
            int errOpt = osNetworkLibrary.getErrOpt(socket);
            if(errOpt == 0) {
                int r = SslBinding.sslSetFd(ssl, socket.intValue());
                if(r == 1) {
                    return handshake();
                }else {
                    throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to perform sslSetFd(), err code : \{SslBinding.sslGetErr(ssl, r)}");
                }
            }else {
                throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to establish connection, err opt : \{errOpt}");
            }
        }else if(state == WANT_WRITE) {
            return handshake();
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public Protocol toProtocol() {
        return new SslProtocol(channel, ssl);
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
                state = WANT_READ;
                return Constants.NET_R;
            }else if(err == Constants.SSL_ERROR_WANT_WRITE) {
                state = WANT_WRITE;
                return Constants.NET_W;
            }else {
                throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to perform SSL_handshake(), err code : \{err}");
            }
        }
    }

    private int verifyCertificate() {
        if(!clientSide) {
            MemorySegment certificate = SslBinding.sslGetPeerCertificate(ssl);
            if(NativeUtil.checkNullPointer(certificate)) {
                throw new FrameworkException(ExceptionType.NETWORK, "Server certificate not provided");
            }
            long verifyResult = SslBinding.sslGetVerifyResult(ssl);
            if(verifyResult != 0) {
                throw new FrameworkException(ExceptionType.NETWORK, STR."Server certificate cannot be verified, verify result : \{verifyResult}");
            }
        }
        return Constants.NET_UPDATE;
    }
}
