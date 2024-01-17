package cn.zorcc.common.postgre;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.State;
import cn.zorcc.common.bindings.SslBinding;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.Channel;
import cn.zorcc.common.network.SslProtocol;
import cn.zorcc.common.network.TcpProtocol;
import cn.zorcc.common.network.api.Protocol;
import cn.zorcc.common.network.api.Sentry;
import cn.zorcc.common.network.lib.OsNetworkLibrary;
import cn.zorcc.common.structure.Allocator;
import cn.zorcc.common.structure.Holder;
import cn.zorcc.common.structure.ReadBuffer;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public record PgSentry(
        Channel channel,
        MemorySegment sslCtx,
        PgConfig pgConfig,
        State sslState,
        Holder<MemorySegment> sslHolder
) implements Sentry {
    private static final OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;
    private static final long WAITING_SSL = 1L;
    private static final long WANT_READ = 1L << 1;
    private static final long WANT_WRITE = 1L << 2;
    public PgSentry(Channel channel, MemorySegment sslCtx, PgConfig pgConfig) {
        this(channel, sslCtx, pgConfig, new State(0L), new Holder<>());
    }

    @Override
    public long onReadableEvent(MemorySegment reserved, long len) {
        if(sslState.unregister(WAITING_SSL)) {
            byte b = new ReadBuffer(reserved).readByte();
            if(b == Constants.PG_SSL_OK) {
                sslHolder.setValue(SslBinding.sslNew(sslCtx));
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
    public long onWritableEvent() {
        if(sslState.unregister(WANT_WRITE)) {
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
                sslState.register(WAITING_SSL);
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
        MemorySegment ssl = sslHolder.getValue();
        if(ssl != null) {
            return new SslProtocol(channel, ssl, sslState);
        }else {
            return new TcpProtocol(channel);
        }
    }

    @Override
    public void doClose() {
        MemorySegment ssl = sslHolder.getValue();
        if(ssl != null) {
            SslBinding.sslFree(ssl);
        }
        int r = osNetworkLibrary.closeSocket(channel.socket());
        if(r < 0) {
            throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to close socket, errno : \{Math.abs(r)}");
        }
    }

    private long handshake() {
        MemorySegment ssl = sslHolder.getValue();
        int r = SslBinding.sslConnect(ssl);
        if(r == 1) {
            verifyCertificate();
            return Constants.NET_UPDATE;
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

    private void verifyCertificate() {
        if(!pgConfig.getSslMode().equals(Constants.PG_SSL_PREFER)) {
            MemorySegment ssl = sslHolder.getValue();
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
