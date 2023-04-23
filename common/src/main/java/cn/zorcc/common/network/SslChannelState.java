package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.foreign.MemorySegment;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;

/**
 *   Tcp channel with TLS
 */
@Slf4j
public non-sealed class SslChannelState implements ChannelState {
    private static final MemorySegment sslServerMethod;
    private static final MemorySegment sslClientMethod;
    private static final MemorySegment sslServerCtx;
    private static final MemorySegment sslClientCtx;

    static {
        log.debug("Loading openssl library, version : {}", Openssl.version());
        sslServerMethod = Openssl.tlsServer();
        sslClientMethod = Openssl.tlsClient();
        sslServerCtx = Openssl.sslCtxNew(sslServerMethod);
        sslClientCtx = Openssl.sslCtxNew(sslClientMethod);
    }

    /**
     *   current ssl pointer
     */
    private final MemorySegment ssl;

    public SslChannelState(Socket socket, boolean fromClient) {
        this.ssl = Openssl.sslNew(fromClient ? sslClientCtx : sslServerCtx);
    }

    @Override
    public void init() {

    }

    @Override
    public void shutdown() {

    }
}
