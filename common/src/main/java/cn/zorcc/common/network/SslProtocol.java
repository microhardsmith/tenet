package cn.zorcc.common.network;

import lombok.extern.slf4j.Slf4j;

import java.lang.foreign.MemorySegment;

/**
 *   Tcp channel with TLS
 */
@Slf4j
public class SslProtocol implements Protocol {
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

    public SslProtocol(Socket socket, boolean fromClient) {
        this.ssl = Openssl.sslNew(fromClient ? sslClientCtx : sslServerCtx);
    }


}
