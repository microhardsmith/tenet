package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.State;
import cn.zorcc.common.bindings.SslBinding;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.api.Protocol;
import cn.zorcc.common.network.lib.OsNetworkLibrary;
import cn.zorcc.common.structure.Mutex;

import java.lang.foreign.MemorySegment;

public record SslProtocol(
        Channel channel,
        MemorySegment ssl,
        State sslState
) implements Protocol {
    private static final OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;
    /**
     *   We don't need RECV_WANT_READ or RECV_WANT_WRITE here
     *   RECV_WANT_READ can be ignored because it's triggered by onReadableEvent(), and it will sure be triggered Again
     *   RECV_WANT_WRITE can be ignored because when it's triggered, we will unregister readable event and register writable event,
     *   when onWritableEvent() is triggered, readable event will always be registered, so it will be able to read then
     */
    private static final int SEND_WANT_READ = 1 << 4;
    private static final int SEND_WANT_WRITE = 1 << 8;
    private static final int LOCAL_INITIATED_SHUTDOWN = 1 << 12;
    private static final int REMOTE_INITIATED_SHUTDOWN = 1 << 16;

    @Override
    public int onReadableEvent(MemorySegment reserved, int len) {
        try(Mutex _ = sslState.withMutex()) {
            if(sslState.unregister(SEND_WANT_READ)) {
                channel.writer().submit(new WriterTask(WriterTaskType.WRITABLE, channel, null, null));
            }
            int received = SslBinding.sslRead(ssl, reserved, len);
            if(received <= 0) {
                int err = SslBinding.sslGetErr(ssl, received);
                if(err == Constants.SSL_ERROR_WANT_READ) {
                    return Constants.NET_IGNORED;
                }else if(err == Constants.SSL_ERROR_WANT_WRITE) {
                    return Constants.NET_W;
                }else if(err == Constants.SSL_ERROR_ZERO_RETURN) {
                    if(!sslState.unregister(LOCAL_INITIATED_SHUTDOWN)) {
                        sslState.register(REMOTE_INITIATED_SHUTDOWN);
                    }
                    return 0;
                }else {
                    return SslBinding.throwException(err, "SSL_read()");
                }
            }else {
                return received;
            }
        }
    }

    @Override
    public int onWritableEvent() {
        try(Mutex _ = sslState.withMutex()) {
            if(sslState.unregister(SEND_WANT_WRITE)) {
                channel.writer().submit(new WriterTask(WriterTaskType.WRITABLE, channel, null, null));
            }
            return Constants.NET_R;
        }
    }

    @Override
    public int doWrite(MemorySegment data, int len) {
        try(Mutex _ = sslState.withMutex()) {
            int written = SslBinding.sslWrite(ssl, data, len);
            if(written <= 0) {
                int err = SslBinding.sslGetErr(ssl, written);
                if(err == Constants.SSL_ERROR_WANT_READ) {
                    sslState.register(SEND_WANT_READ);
                    return Constants.NET_PR;
                }else if(err == Constants.SSL_ERROR_WANT_WRITE) {
                    sslState.register(SEND_WANT_WRITE);
                    return Constants.NET_PW;
                }else {
                    return SslBinding.throwException(err, "SSL_write()");
                }
            }else {
                return written;
            }
        }
    }

    @Override
    public void doShutdown() {
        try(Mutex _ = sslState.withMutex()) {
            sslState.register(LOCAL_INITIATED_SHUTDOWN);
            int r = SslBinding.sslShutdown(ssl);
            if(r < 0) {
                throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to perform SSL_shutdown(), err code : \{SslBinding.sslGetErr(ssl, r)}");
            }
        }
    }

    @Override
    public void doClose() {
        try(Mutex _ = sslState.withMutex()) {
            if(sslState.unregister(REMOTE_INITIATED_SHUTDOWN)) {
                SslBinding.sslShutdown(ssl);
            }
            SslBinding.sslFree(ssl);
            if(osNetworkLibrary.closeSocket(channel.socket()) != 0) {
                throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to close socket, errno : \{osNetworkLibrary.errno()}");
            }
        }
    }
}
