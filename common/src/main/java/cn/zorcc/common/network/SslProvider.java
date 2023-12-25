package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.bindings.SslBinding;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.api.Provider;
import cn.zorcc.common.network.api.Sentry;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

public record SslProvider(
        boolean clientSide,
        MemorySegment ctx
) implements Provider {

    public static SslProvider newClientProvider(String caFiles, String caPaths) {
        MemorySegment ctx = createCtx();
        try (Arena arena = Arena.ofConfined()) {
            if(caFiles != null && !caFiles.isBlank()) {
                for (String caFile : caFiles.split(",")) {
                    MemorySegment file = NativeUtil.allocateStr(arena, caFile);
                    if(SslBinding.loadVerifyLocations(ctx, file, NativeUtil.NULL_POINTER) != 1) {
                        throw new FrameworkException(ExceptionType.NETWORK, STR."Can't load verify file : \{caFile}");
                    }
                }
            }
            if(caPaths != null && !caPaths.isBlank()) {
                for (String caPath : caPaths.split(",")) {
                    MemorySegment path = NativeUtil.allocateStr(arena, caPath);
                    if(SslBinding.loadVerifyLocations(ctx, NativeUtil.NULL_POINTER, path) != 1) {
                        throw new FrameworkException(ExceptionType.NETWORK, STR."Can't load verify dir : \{caPath}");
                    }
                }
            }
        }
        if(SslBinding.setDefaultVerifyPath(ctx) != 1) {
            throw new FrameworkException(ExceptionType.NETWORK, "Can't set default verify path");
        }
        SslBinding.setVerify(ctx, Constants.SSL_VERIFY_PEER, NativeUtil.NULL_POINTER);
        return new SslProvider(true, ctx);
    }

    public static SslProvider newServerProvider(String publicKeyFile, String privateKeyFile) {
        MemorySegment ctx = createCtx();
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
        return new SslProvider(false, ctx);
    }

    private static MemorySegment createCtx() {
        MemorySegment ctx = SslBinding.sslCtxNew(SslBinding.tlsMethod());
        if(NativeUtil.checkNullPointer(ctx)) {
            throw new FrameworkException(ExceptionType.NETWORK, "SSL ctx initialization failed");
        }
        SslBinding.configureCtx(ctx);
        return ctx;
    }

    @Override
    public Sentry create(Channel channel) {
        return new SslSentry(channel, clientSide, SslBinding.sslNew(ctx));
    }

    @Override
    public void close() {
        SslBinding.sslCtxFree(ctx);
    }
}
