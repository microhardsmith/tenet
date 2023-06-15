package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import lombok.extern.slf4j.Slf4j;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *   Tcp channel with TLS
 */
@Slf4j
public class SslProtocol implements Protocol {
    private static final Native n = Native.n;
    private final MemorySegment ssl;
    private final Lock lock = new ReentrantLock();
    private int state = 0;
    private static final int RECV_WANT_TO_WRITE = 1;
    private static final int SEND_WANT_TO_READ = 1 << 1;
    private static final int SEND_WANT_TO_WRITE = 1 << 2;
    private static final int SHUTDOWN_WANT_TO_READ = 1 << 3;
    private static final int SHUTDOWN_WANT_TO_WRITE = 1 << 4;

    public SslProtocol(MemorySegment ssl) {
        this.ssl = ssl;
    }

    /**
     *   Deal with channel canRead operation
     *   while reading from ssl channel, SSL_ERROR_WANT_READ will be ignored, since read operation will always be triggered
     *   if SSL_ERROR_WANT_WRITE happened, which means current read operation is not successful because channel is not able to perform write operation
     *
     */
    @Override
    public void canRead(Channel channel, ReadBuffer readBuffer) {
        lock.lock();
        try {
            if((state & SHUTDOWN_WANT_TO_READ) != 0) {
                state ^= SHUTDOWN_WANT_TO_READ;
                performShutdown(channel);
            }
            if((state & SEND_WANT_TO_READ) != 0) {
                state ^= SEND_WANT_TO_READ;
                channel.worker().submitWriterTask(new WriterTask(channel, Boolean.TRUE));
            }
            if((state & RECV_WANT_TO_WRITE) == 0) {
                int r = Openssl.sslRead(ssl, readBuffer.segment(), (int) readBuffer.len());
                if(r <= 0) {
                    int err = Openssl.sslGetErr(ssl, r);
                    if(err == Constants.SSL_ERROR_WANT_WRITE) {
                        state &= RECV_WANT_TO_WRITE;
                    }else if(err == Constants.SSL_ERROR_ZERO_RETURN) {
                        performShutdown(channel);
                    }else if(err != Constants.SSL_ERROR_WANT_READ) {
                        // usually connection reset by peer
                        log.debug("{} ssl recv err, ssl_err : {}", channel.loc(), err);
                        channel.close();
                    }
                }else {
                    readBuffer.setWriteIndex(r);
                    channel.onReadBuffer(readBuffer);
                }
            }
        }finally {
            lock.unlock();
        }
    }

    @Override
    public void canWrite(Channel channel) {
        lock.lock();
        try {
            if((state & SHUTDOWN_WANT_TO_WRITE) != 0) {
                state ^= SHUTDOWN_WANT_TO_WRITE;
                performShutdown(channel);
            }
            if((state & SEND_WANT_TO_WRITE) != 0) {
                state ^= SEND_WANT_TO_WRITE;
                channel.worker().submitWriterTask(new WriterTask(channel, Boolean.TRUE));
            }
            if((state & RECV_WANT_TO_WRITE) != 0) {
                state ^= RECV_WANT_TO_WRITE;
            }
            if(channel.state().compareAndSet(Native.REGISTER_READ_WRITE, Native.REGISTER_READ)) {
                n.ctl(channel.worker().mux(), channel.socket(), Native.REGISTER_READ_WRITE, Native.REGISTER_READ);
            }else {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
        }finally {
            lock.unlock();
        }
    }

    @Override
    public WriteStatus doWrite(Channel channel, WriteBuffer writeBuffer) {
        MemorySegment segment = writeBuffer.segment();
        int len = (int) segment.byteSize();
        lock.lock();
        try{
            int r = Openssl.sslWrite(ssl, segment, len);
            if(r <= 0) {
                int err = Openssl.sslGetErr(ssl, r);
                if(err == Constants.SSL_ERROR_WANT_WRITE) {
                    state &= SEND_WANT_TO_WRITE;
                    if(channel.state().compareAndSet(Native.REGISTER_READ, Native.REGISTER_READ_WRITE)) {
                        n.ctl(channel.worker().mux(), channel.socket(), Native.REGISTER_READ, Native.REGISTER_READ_WRITE);
                    }
                }else if(err == Constants.SSL_ERROR_WANT_READ) {
                    state &= SEND_WANT_TO_READ;
                }else {
                    log.error("Failed to write, ssl err : {}", err);
                    return WriteStatus.FAILURE;
                }
                return WriteStatus.PENDING;
            }else if(r < len){
                // only write a part of the segment, wait for next loop
                writeBuffer.truncate(r);
                return doWrite(channel, writeBuffer);
            }
            return WriteStatus.SUCCESS;
        }finally {
            lock.unlock();
        }
    }

    @Override
    public void doShutdown(Channel channel) {
        lock.lock();
        try{
            performShutdown(channel);
        }finally {
            lock.unlock();
        }
    }

    private void performShutdown(Channel channel) {
        int r = Openssl.sslShutdown(ssl);
        if(r < 0) {
            int err = Openssl.sslGetErr(ssl, r);
            if(err == Constants.SSL_ERROR_WANT_READ) {
                state &= SHUTDOWN_WANT_TO_READ;
            }else if(err == Constants.SSL_ERROR_WANT_WRITE) {
                state &= SHUTDOWN_WANT_TO_WRITE;
                if(channel.state().compareAndSet(Native.REGISTER_READ, Native.REGISTER_READ_WRITE)) {
                    n.ctl(channel.worker().mux(), channel.socket(), Native.REGISTER_READ, Native.REGISTER_READ_WRITE);
                }
            }else {
                log.error("SSL shutdown failed with err : {}", err);
            }
        }else if(r == 1) {
            Worker worker = channel.worker();
            if(Thread.currentThread() == worker.reader()) {
                channel.close();
            }else {
                // In this case, the channel close operation might be delayed a little bit
                worker.submitReaderTask(new ReaderTask(ReaderTask.ReaderTaskType.CLOSE_CHANNEL, null, channel, null));
            }
        }
    }

    @Override
    public void doClose(Channel channel) {
        Openssl.sslFree(ssl);
        n.closeSocket(channel.socket());
    }
}
