package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.bindings.SslBinding;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.network.api.Channel;
import cn.zorcc.common.network.api.Provider;
import cn.zorcc.common.network.api.Sentry;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

public final class SslProvider implements Provider {
    private static final Logger log = new Logger(SslProvider.class);
    private final boolean clientSide;
    private final MemorySegment ctx;

    public static SslProvider newClientProvider() {
        return new SslProvider(true, null, null);
    }

    public static SslProvider newServerProvider(String publicKeyFile, String privateKeyFile) {
        return new SslProvider(false, publicKeyFile, privateKeyFile);
    }

    private SslProvider(boolean clientSide, String publicKeyFile, String privateKeyFile) {
        log.debug(STR."SslProvider \{clientSide ? "clientSide" : "serverSide" } instance created");
        this.clientSide = clientSide;
        this.ctx = SslBinding.sslCtxNew(SslBinding.tlsMethod());
        if(NativeUtil.checkNullPointer(ctx)) {
            throw new FrameworkException(ExceptionType.NETWORK, "SSL ctx initialization failed");
        }
        SslBinding.configureCtx(ctx);
        if(clientSide) {
            SslBinding.setVerify(ctx, Constants.SSL_VERIFY_PEER, NativeUtil.NULL_POINTER);
        }else {
            configureServerSideCtx(publicKeyFile, privateKeyFile);
        }
    }

    private void configureServerSideCtx(String publicKeyFile, String privateKeyFile) {
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment publicKey = NativeUtil.allocateStr(arena, publicKeyFile);
            if (SslBinding.setPublicKey(ctx, publicKey, Constants.SSL_FILETYPE_PEM) <= 0) {
                throw new FrameworkException(ExceptionType.NETWORK, "SSL server public key err");
            }
            MemorySegment privateKey = NativeUtil.allocateStr(arena, privateKeyFile);
            if (SslBinding.setPrivateKey(ctx, privateKey, Constants.SSL_FILETYPE_PEM) <= 0) {
                throw new FrameworkException(ExceptionType.NETWORK, "SSL server private key err");
            }
            if (SslBinding.checkPrivateKey(ctx) <= 0) {
                throw new FrameworkException(ExceptionType.NETWORK, "SSL server private key and public key doesn't match");
            }
        }
    }

    @Override
    public Sentry create(Channel channel) {
        return new SslSentry(channel, clientSide, SslBinding.sslNew(ctx));
    }

    @Override
    public void close() {
        log.debug(STR."SslProvider instance released for \{clientSide ? "clientSide" : "serverSide"}");
        SslBinding.sslCtxFree(ctx);
    }
}
