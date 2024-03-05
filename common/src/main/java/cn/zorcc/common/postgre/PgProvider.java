package cn.zorcc.common.postgre;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.bindings.SslBinding;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.Channel;
import cn.zorcc.common.network.Provider;
import cn.zorcc.common.network.Sentry;
import cn.zorcc.common.util.NativeUtil;
import cn.zorcc.common.util.SslUtil;

import java.lang.foreign.MemorySegment;

/**
 *   PgProvider is just like SslProvider, using a separate ctx
 */
public final class PgProvider implements Provider {
    private final MemorySegment ctx;
    private final PgConfig pgConfig;
    public PgProvider(PgConfig pgConfig) {
        verifyPgConfig(pgConfig);
        this.pgConfig = pgConfig;
        this.ctx = SslBinding.sslCtxNew(SslBinding.tlsMethod());
        if(NativeUtil.checkNullPointer(ctx)) {
            throw new FrameworkException(ExceptionType.NETWORK, "SSL ctx initialization failed");
        }
        SslUtil.configureCtx(ctx);
        switch (pgConfig.getSslMode()) {
            case Constants.PG_SSL_PREFER -> SslBinding.setVerify(ctx, Constants.SSL_VERIFY_NONE, MemorySegment.NULL);
            case Constants.PG_SSL_VERIFY_CA -> SslBinding.setVerify(ctx, Constants.SSL_VERIFY_PEER, MemorySegment.NULL);
            case Constants.PG_SSL_VERIFY_FULL -> SslBinding.setVerify(ctx, Constants.SSL_VERIFY_PEER | Constants.SSL_VERIFY_FAIL_IF_NO_PEER_CERT, MemorySegment.NULL);
            default -> throw new FrameworkException(ExceptionType.POSTGRESQL, Constants.UNREACHED);
        }
    }

    private void verifyPgConfig(PgConfig pgConfig) {
        if(pgConfig.getLoc() == null) {
            throw new FrameworkException(ExceptionType.POSTGRESQL, "Empty postgresql server location spotted");
        }
        String userName = pgConfig.getUserName();
        if(userName == null || userName.isBlank()) {
            throw new FrameworkException(ExceptionType.POSTGRESQL, "Empty postgresql server username spotted");
        }
        String password = pgConfig.getPassword();
        if(password == null || password.isBlank()) {
            throw new FrameworkException(ExceptionType.POSTGRESQL, "Empty postgresql server password spotted");
        }
        String sslMode = pgConfig.getSslMode();
        if(!sslMode.equals(Constants.PG_SSL_PREFER) && !sslMode.equals(Constants.PG_SSL_VERIFY_CA) && !sslMode.equals(Constants.PG_SSL_VERIFY_FULL)) {
            throw new FrameworkException(ExceptionType.POSTGRESQL, "Unsupported postgresql ssl mode");
        }
    }

    @Override
    public Sentry create(Channel channel) {
        return new PgSentry(channel, ctx, pgConfig);
    }

    @Override
    public void close() {
        SslBinding.sslCtxFree(ctx);
    }
}
