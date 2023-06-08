package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import lombok.extern.slf4j.Slf4j;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
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
        boolean shouldUnpark = false;
        lock.lock();
        try {
            if((state & SHUTDOWN_WANT_TO_READ) != 0) {
                state ^= SHUTDOWN_WANT_TO_READ;
                performShutdown(channel);
            }
            if((state & SEND_WANT_TO_READ) != 0) {
                state ^= SEND_WANT_TO_READ;
                shouldUnpark = true;
            }
            if((state & RECV_WANT_TO_WRITE) == 0) {
                int r = Openssl.sslRead(ssl, readBuffer.segment(), (int) readBuffer.len());
                if(r <= 0) {
                    int err = Openssl.sslGetErr(ssl, r);
                    if(err == Constants.SSL_ERROR_WANT_WRITE) {
                        state &= RECV_WANT_TO_WRITE;
                    }else if(err == Constants.SSL_ERROR_ZERO_RETURN) {
                        performShutdown(channel);
                    }else if(err == Constants.SSL_ERROR_SYSCALL) {
                        channel.close();
                    }else if(err != Constants.SSL_ERROR_WANT_READ) {
                        throw new FrameworkException(ExceptionType.NETWORK, "SSL read failed with err : %d".formatted(err));
                    }
                }else {
                    readBuffer.setWriteIndex(r);
                    channel.onReadBuffer(readBuffer);
                }
            }
        }finally {
            lock.unlock();
        }
        if(shouldUnpark) {
            LockSupport.unpark(channel.writerThread());
        }
    }

    @Override
    public void canWrite(Channel channel) {
        boolean shouldUnpark = false;
        lock.lock();
        try {
            if((state & SHUTDOWN_WANT_TO_WRITE) != 0) {
                state ^= SHUTDOWN_WANT_TO_WRITE;
                performShutdown(channel);
            }
            if((state & SEND_WANT_TO_WRITE) != 0) {
                state ^= SEND_WANT_TO_WRITE;
                shouldUnpark = true;
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
        if(shouldUnpark) {
            LockSupport.unpark(channel.writerThread());
        }
    }

    @Override
    public void doWrite(Channel channel, WriteBuffer writeBuffer) {
        MemorySegment segment = writeBuffer.segment();
        int len = (int) segment.byteSize();
        boolean shouldPark = false;
        lock.lock();
        try{
            int r = Openssl.sslWrite(ssl, segment, len);
            if(r <= 0) {
                shouldPark = true;
                int err = Openssl.sslGetErr(ssl, r);
                if(err == Constants.SSL_ERROR_WANT_WRITE) {
                    state &= SEND_WANT_TO_WRITE;
                    if(channel.state().compareAndSet(Native.REGISTER_READ, Native.REGISTER_READ_WRITE)) {
                        n.ctl(channel.worker().mux(), channel.socket(), Native.REGISTER_READ, Native.REGISTER_READ_WRITE);
                    }
                }else if(err == Constants.SSL_ERROR_WANT_READ) {
                    state &= SEND_WANT_TO_READ;
                }else {
                    throw new FrameworkException(ExceptionType.NETWORK, "Failed to write, ssl err : %d".formatted(err));
                }
            }else if(r < len){
                // only write a part of the segment, wait for next loop
                writeBuffer.truncate(r);
                doWrite(channel, writeBuffer);
            }
        }finally {
            lock.unlock();
        }
        if(shouldPark) {
            LockSupport.park();
            doWrite(channel, writeBuffer);
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
            if(Thread.currentThread() == worker.thread()) {
                channel.close();
            }else {
                // In this case, the channel close operation might be delayed a little bit
                worker.submitTask(new Task(Task.TaskType.CLOSE_CHANNEL, null, channel, null));
            }
        }
    }

    @Override
    public void doClose(Channel channel) {
        Openssl.sslFree(ssl);
        n.closeSocket(channel.socket());
    }
}
