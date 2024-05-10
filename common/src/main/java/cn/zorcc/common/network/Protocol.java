package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.bindings.SslBinding;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.structure.IntHolder;
import cn.zorcc.common.util.SslUtil;

import java.lang.foreign.MemorySegment;

/**
 *   Protocol determines how a channel should interact with the poller thread and writer thread, protocol instance will be shared among them
 *   so if an action will be performed by both thread, there must be an external lock to guarantee the safety of it
 */
public interface Protocol {
    OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;

    /**
     *   Indicates that protocol could read from the socket now, this function will always be invoked on poller thread
     *   the parameter len will always be the exact length of reserved segment
     *   return a positive number to indicate actual bytes read, or a flag to indicate a state change
     */
    long onReadableEvent(MemorySegment reserved, long len);

    /**
     *   Indicates that protocol could write to the socket now, this function will always be invoked on poller thread
     *   return a flag to indicate a state change
     */
    long onWritableEvent();

    /**
     *   Perform the actual write operation, this function will always be invoked on writer thread
     *   the parameter len will always be the exact length of data segment
     *   return a positive number to indicate actual bytes written, or a flag to indicate a state change
     */
    long doWrite(MemorySegment data, long len);

    /**
     *   Perform the actual shutdown operation, this function will always be invoked on writer thread
     *   It can be guaranteed that this function will be only invoked once, no external synchronization needed
     *   If a RuntimeException was thrown in this function, channel will be closed
     */
    void doShutdown();

    /**
     *   Perform the actual close operation, this function could be invoked in poller thread or writer thread depending on the situation (the later closed one gets to call doClose())
     *   It can be guaranteed that this function will be only invoked once, no external synchronization needed
     *   RuntimeException thrown in this function would not be handled, it would only be recorded in log
     */
    void doClose();

    static Protocol newTcpProtocol(Channel channel) {
        return new TcpProtocol(channel);
    }

    record TcpProtocol(
            Channel channel
    ) implements Protocol {

        @Override
        public long onReadableEvent(MemorySegment reserved, long len) {
            long r = osNetworkLibrary.recv(channel.socket(), reserved, len);
            if(r < 0L) {
                throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to perform recv(), errno : \{Math.abs(r)}");
            }else {
                return r;
            }
        }

        @Override
        public long onWritableEvent() {
            channel.writer().submit(new WriterTask(WriterTaskType.WRITABLE, channel, null, null));
            return -Constants.NET_R;
        }

        @Override
        public long doWrite(MemorySegment data, long len) {
            Socket socket = channel.socket();
            long r = osNetworkLibrary.send(socket, data, len);
            if(r < 0L) {
                int errno = Math.toIntExact(-r);
                if(errno == osNetworkLibrary.sendBlockCode()) {
                    return -Constants.NET_PW;
                }else {
                    throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to perform send(), errno : \{errno}");
                }
            }else {
                return r;
            }
        }

        @Override
        public void doShutdown() {
            int r = osNetworkLibrary.shutdownWrite(channel.socket());
            if(r < 0) {
                throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to perform shutdown(), errno : \{Math.abs(r)}");
            }
        }


        @Override
        public void doClose() {
            int r = osNetworkLibrary.closeSocket(channel.socket());
            if(r < 0) {
                throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to close socket, errno : \{Math.abs(r)}");
            }
        }

    }

    static Protocol newSslProtocol(Channel channel, MemorySegment ssl, IntHolder sslState) {
        return new SslProtocol(channel, ssl, sslState);
    }

    record SslProtocol(
            Channel channel,
            MemorySegment ssl,
            IntHolder sslState
    ) implements Protocol {

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
        public long onReadableEvent(MemorySegment reserved, long len) {
            int state = sslState.lock(Thread::yield);
            try{
                if((state & SEND_WANT_READ) != 0) {
                    state &= ~SEND_WANT_READ;
                    channel.writer().submit(new WriterTask(WriterTaskType.WRITABLE, channel, null, null));
                }
                int received = SslBinding.sslRead(ssl, reserved, Math.toIntExact(len));
                if(received <= 0) {
                    int err = SslBinding.sslGetErr(ssl, received);
                    if(err == Constants.SSL_ERROR_WANT_READ) {
                        return -Constants.NET_IGNORED;
                    }else if(err == Constants.SSL_ERROR_WANT_WRITE) {
                        return -Constants.NET_W;
                    }else if(err == Constants.SSL_ERROR_ZERO_RETURN) {
                        if((state & LOCAL_INITIATED_SHUTDOWN) == 0) {
                            state |= REMOTE_INITIATED_SHUTDOWN;
                        }else {
                            state &= (~LOCAL_INITIATED_SHUTDOWN);
                        }
                        return 0;
                    }else {
                        return SslUtil.throwException(err, "SSL_read()", Poller.localMemApi());
                    }
                }else {
                    return received;
                }
            }finally {
                sslState.unlock(state);
            }
        }

        @Override
        public long onWritableEvent() {
            int state = sslState.lock(Thread::yield);
            try{
                if((state & SEND_WANT_WRITE) != 0) {
                    state &= (~SEND_WANT_WRITE);
                    channel.writer().submit(new WriterTask(WriterTaskType.WRITABLE, channel, null, null));
                }
                return -Constants.NET_R;
            }finally {
                sslState.unlock(state);
            }
        }

        @Override
        public long doWrite(MemorySegment data, long len) {
            int state = sslState.lock(Thread::yield);
            try{
                int written = SslBinding.sslWrite(ssl, data, Math.toIntExact(len));
                if(written <= 0) {
                    int err = SslBinding.sslGetErr(ssl, written);
                    if(err == Constants.SSL_ERROR_WANT_READ) {
                        state |= SEND_WANT_READ;
                        return -Constants.NET_PR;
                    }else if(err == Constants.SSL_ERROR_WANT_WRITE) {
                        state |= SEND_WANT_WRITE;
                        return -Constants.NET_PW;
                    }else {
                        return SslUtil.throwException(err, "SSL_write()", Writer.localMemApi());
                    }
                }else {
                    return written;
                }
            }finally {
                sslState.unlock(state);
            }
        }

        @Override
        public void doShutdown() {
            int state = sslState.lock(Thread::yield);
            try{
                state |= LOCAL_INITIATED_SHUTDOWN;
                int r = SslBinding.sslShutdown(ssl);
                if(r < 0) {
                    throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to perform SSL_shutdown(), err code : \{SslBinding.sslGetErr(ssl, r)}");
                }
            }finally {
                sslState.unlock(state);
            }
        }

        @Override
        public void doClose() {
            int state = sslState.lock(Thread::yield);
            try{
                if((state & REMOTE_INITIATED_SHUTDOWN) != 0) {
                    state &= (~REMOTE_INITIATED_SHUTDOWN);
                    SslBinding.sslShutdown(ssl);
                }
                SslBinding.sslFree(ssl);
                int r = osNetworkLibrary.closeSocket(channel.socket());
                if(r < 0) {
                    throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to close socket, errno : \{Math.abs(r)}");
                }
            }finally {
                sslState.unlock(state);
            }
        }
    }
}
