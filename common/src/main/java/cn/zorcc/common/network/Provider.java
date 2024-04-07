package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.bindings.SslBinding;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.structure.Allocator;
import cn.zorcc.common.structure.MemApi;
import cn.zorcc.common.util.NativeUtil;
import cn.zorcc.common.util.SslUtil;

import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;

/**
 *   Sentry factory with customized deallocated procedure provided
 */
@FunctionalInterface
public interface Provider {
    /**
     *   Return a newly created sentry instance
     */
    Sentry create(Channel channel);

    /**
     *   Release current provider's resources, implementation could choose to override it
     */
    default void close() {

    }

    static Provider newTcpProvider() {
        return new TcpProvider();
    }

    record TcpProvider() implements Provider {
        @Override
        public Sentry create(Channel channel) {
            return Sentry.newTcpSentry(channel);
        }
    }

    static SslProvider newSslClientProvider(String caFiles, String caPaths) {
        MemorySegment ctx = createCtx();
        try (Allocator allocator = Allocator.newDirectAllocator(MemApi.DEFAULT)) {
            if(caFiles != null && !caFiles.isBlank()) {
                for (String caFile : caFiles.split(",")) {
                    MemorySegment file = allocator.allocateFrom(caFile.trim());
                    if(SslBinding.loadVerifyLocations(ctx, file, MemorySegment.NULL) != 1) {
                        throw new FrameworkException(ExceptionType.NETWORK, STR."Can't load verify file : \{caFile}");
                    }
                }
            }
            if(caPaths != null && !caPaths.isBlank()) {
                for (String caPath : caPaths.split(",")) {
                    MemorySegment path = allocator.allocateFrom(caPath.trim());
                    if(SslBinding.loadVerifyLocations(ctx, MemorySegment.NULL, path) != 1) {
                        throw new FrameworkException(ExceptionType.NETWORK, STR."Can't load verify dir : \{caPath}");
                    }
                }
            }
        }
        if(SslBinding.setDefaultVerifyPath(ctx) != 1) {
            throw new FrameworkException(ExceptionType.NETWORK, "Can't set default verify path");
        }
        SslBinding.setVerify(ctx, Constants.SSL_VERIFY_PEER, MemorySegment.NULL);
        return new SslProvider(true, ctx);
    }

    static SslProvider newSslServerProvider(String publicKeyFile, String privateKeyFile) {
        MemorySegment ctx = createCtx();
        try(Allocator allocator = Allocator.newDirectAllocator(MemApi.DEFAULT)) {
            MemorySegment publicKey = allocator.allocateFrom(publicKeyFile, StandardCharsets.UTF_8);
            if (SslBinding.setPublicKey(ctx, publicKey, Constants.SSL_FILETYPE_PEM) <= 0) {
                throw new FrameworkException(ExceptionType.NETWORK, "SSL server public key is invalid");
            }
            MemorySegment privateKey = allocator.allocateFrom(privateKeyFile, StandardCharsets.UTF_8);
            if (SslBinding.setPrivateKey(ctx, privateKey, Constants.SSL_FILETYPE_PEM) <= 0) {
                throw new FrameworkException(ExceptionType.NETWORK, "SSL server private key is invalid");
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
        SslUtil.configureCtx(ctx);
        return ctx;
    }

    record SslProvider(
            boolean clientSide,
            MemorySegment ctx
    ) implements Provider {

        @Override
        public Sentry create(Channel channel) {
            return Sentry.newSslSentry(channel, clientSide, SslBinding.sslNew(ctx));
        }

        @Override
        public void close() {
            SslBinding.sslCtxFree(ctx);
        }
    }
}
