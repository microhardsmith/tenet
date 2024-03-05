package cn.zorcc.common.postgre;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.bindings.SslBinding;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.Channel;
import cn.zorcc.common.network.Protocol;
import cn.zorcc.common.network.Sentry;
import cn.zorcc.common.structure.Allocator;
import cn.zorcc.common.structure.IntHolder;
import cn.zorcc.common.structure.ReadBuffer;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public final class PgSentry implements Sentry {
    private static final int WAITING_SSL = 1;
    private static final int WANT_READ = 1 << 1;
    private static final int WANT_WRITE = 1 << 2;
    private final Channel channel;
    private final MemorySegment sslCtx;
    private final PgConfig pgConfig;
    private final IntHolder sslState = new IntHolder(0);
    private MemorySegment ssl = MemorySegment.NULL;
    public PgSentry(Channel channel, MemorySegment sslCtx, PgConfig pgConfig) {
        this.channel = channel;
        this.sslCtx = sslCtx;
        this.pgConfig = pgConfig;
    }

    @Override
    public int onReadableEvent(MemorySegment reserved, long len) {
        int current = sslState.getValue();
        if((current & WAITING_SSL) != 0) {
            sslState.setValue(current & ~(WAITING_SSL));
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
        }else if((current & WANT_READ) != 0) {
            sslState.setValue(current & (~WANT_READ));
            return handshake();
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public int onWritableEvent() {
        int current = sslState.getValue();
        if((current & WANT_WRITE) != 0) {
            sslState.setValue(current & (~WANT_WRITE));
            return handshake();
        }else {
            int errOpt = osNetworkLibrary.getErrOpt(channel.socket());
            if(errOpt != 0) {
                throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to establish postgresql connection, err opt : \{errOpt}");
            }
            MemorySegment segment = Allocator.HEAP.allocate(ValueLayout.JAVA_INT, 2);
            segment.set(ValueLayout.JAVA_INT, 0L, 8);
            segment.set(ValueLayout.JAVA_INT, 4L, 80877103);
            long len = segment.byteSize();
            long sent = osNetworkLibrary.send(channel.socket(), segment, len);
            if(sent == len) {
                sslState.setValue(current | WAITING_SSL);
                return Constants.NET_IGNORED;
            }else if(sent < len) {
                throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to send SSL request, only \{sent} bytes got written, consider this connection unfunctionally");
            }else if(sent < 0L) {
                throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to send SSL request, errno : \{Math.abs(sent)}");
            }else {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
        }
    }

    @Override
    public Protocol toProtocol() {
        if(ssl != MemorySegment.NULL) {
            return Protocol.newSslProtocol(channel, ssl, sslState);
        }else {
            return Protocol.newTcpProtocol(channel);
        }
    }

    @Override
    public void doClose() {
        if(ssl != MemorySegment.NULL) {
            SslBinding.sslFree(ssl);
        }
        int r = osNetworkLibrary.closeSocket(channel.socket());
        if(r < 0) {
            throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to close socket, errno : \{Math.abs(r)}");
        }
    }

    private int handshake() {
        int r = SslBinding.sslConnect(ssl);
        if(r == 1) {
            verifyCertificate();
            return Constants.NET_UPDATE;
        }else {
            int err = SslBinding.sslGetErr(ssl, r);
            if(err == Constants.SSL_ERROR_WANT_READ) {
                sslState.setValue(sslState.getValue() | WANT_READ);
                return Constants.NET_R;
            }else if(err == Constants.SSL_ERROR_WANT_WRITE) {
                sslState.setValue(sslState.getValue() | WANT_WRITE);
                return Constants.NET_W;
            }else {
                throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to perform SSL_handshake(), err code : \{err}");
            }
        }
    }

    private void verifyCertificate() {
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
    }
}
