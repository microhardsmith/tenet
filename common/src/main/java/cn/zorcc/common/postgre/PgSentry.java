package cn.zorcc.common.postgre;

import cn.zorcc.common.*;
import cn.zorcc.common.bindings.SslBinding;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.Channel;
import cn.zorcc.common.network.SslProtocol;
import cn.zorcc.common.network.TcpProtocol;
import cn.zorcc.common.network.api.Protocol;
import cn.zorcc.common.network.api.Sentry;
import cn.zorcc.common.network.lib.OsNetworkLibrary;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

public final class PgSentry implements Sentry {
    private static final OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;
    private static final int WAITING_SSL = 1;
    private static final int WANT_READ = 1 << 1;
    private static final int WANT_WRITE = 1 << 2;
    private final Channel channel;
    private final MemorySegment sslCtx;
    private final PgConfig pgConfig;
    private final State sslState = new State(Constants.INITIAL);
    private MemorySegment ssl;
    public PgSentry(Channel channel, MemorySegment sslCtx, PgConfig pgConfig) {
        this.channel = channel;
        this.sslCtx = sslCtx;
        this.pgConfig = pgConfig;
    }

    @Override
    public int onReadableEvent(MemorySegment reserved, int len) {
        if(sslState.unregister(WAITING_SSL)) {
            byte b = new ReadBuffer(reserved).readByte();
            if(b == Constants.PG_SSL_OK) {
                ssl = SslBinding.sslNew(sslCtx);
                return handshake();
            }else if(b == Constants.PG_SSL_DISABLE) {
                if(pgConfig.getSslMode().equals(Constants.PG_SSL_VERIFY_CA) || pgConfig.getSslMode().equals(Constants.PG_SSL_VERIFY_FULL)) {
                    throw new FrameworkException(ExceptionType.POSTGRESQL, "Client require SSL connection, however the server doesn't support it");
                }else {
                    return Constants.NET_UPDATE;
                }
            }else {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
        }else if(sslState.unregister(WANT_READ)) {
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
            int errOpt = osNetworkLibrary.getErrOpt(channel.socket());
            if(errOpt != 0) {
                throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to establish postgresql connection, err opt : \{errOpt}");
            }
            try(WriteBuffer writeBuffer = WriteBuffer.newFixedWriteBuffer(Arena.ofConfined(), 8)) {
                writeBuffer.writeInt(8);
                writeBuffer.writeInt(80877103);
                MemorySegment segment = writeBuffer.toSegment();
                int len = (int) segment.byteSize();
                int sent = osNetworkLibrary.send(channel.socket(), segment, len);
                if(sent != len) {
                    throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
                }
                sslState.register(WAITING_SSL);
                return Constants.NET_IGNORED;
            }
        }
    }

    @Override
    public Protocol toProtocol() {
        if(ssl != null) {
            return new SslProtocol(channel, ssl, sslState);
        }else {
            return new TcpProtocol(channel);
        }
    }

    @Override
    public void doClose() {
        if(ssl != null) {
            SslBinding.sslFree(ssl);
        }
        if(osNetworkLibrary.closeSocket(channel.socket()) != 0) {
            throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to close socket, errno : \{osNetworkLibrary.errno()}");
        }
    }

    private int handshake() {
        int r = SslBinding.sslConnect(ssl);
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
                throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to perform SSL_handshake(), err code : \{err}");
            }
        }
    }

    private int verifyCertificate() {
        if(!pgConfig.getSslMode().equals(Constants.PG_SSL_PREFER)) {
            MemorySegment x509 = SslBinding.sslGetPeerCertificate(ssl);
            if(NativeUtil.checkNullPointer(x509)) {
                throw new FrameworkException(ExceptionType.NETWORK, "Postgresql server certificate not provided");
            }
            long verifyResult = SslBinding.sslGetVerifyResult(ssl);
            if(verifyResult != 0) {
                throw new FrameworkException(ExceptionType.NETWORK, STR."Postgresql server certificate cannot be verified, verify result : \{verifyResult}");
            }
            SslBinding.x509Free(x509);
        }
        return Constants.NET_UPDATE;
    }
}
