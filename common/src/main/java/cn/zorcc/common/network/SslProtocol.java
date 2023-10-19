package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.binding.SslBinding;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *   Protocol using SSL encryption
 */
public class SslProtocol implements Protocol {
    private static final Logger log = new Logger(SslProtocol.class);
    private static final OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;
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
    public void canRead(Channel channel, MemorySegment memorySegment) {
        ReadBuffer rb = null;
        lock.lock();
        try {
            if((state & SHUTDOWN_WANT_TO_READ) != Constants.ZERO) {
                state ^= SHUTDOWN_WANT_TO_READ;
                performShutdown(channel);
            }
            if((state & SEND_WANT_TO_READ) != Constants.ZERO) {
                state ^= SEND_WANT_TO_READ;
                channel.worker().submitWriterTask(new WriterTask(WriterTask.WriterTaskType.WRITABLE, channel, null, null));
            }
            if((state & RECV_WANT_TO_WRITE) == Constants.ZERO) {
                int len = (int) memorySegment.byteSize();
                int received = SslBinding.sslRead(ssl, memorySegment, len);
                if(received <= 0) {
                    int err = SslBinding.sslGetErr(ssl, received);
                    switch (err) {
                        case Constants.SSL_ERROR_WANT_WRITE -> state &= RECV_WANT_TO_WRITE;
                        case Constants.SSL_ERROR_ZERO_RETURN -> performShutdown(channel);
                        case Constants.SSL_ERROR_WANT_READ -> {}
                        default -> {
                            log.debug(STR."\{channel.loc()} ssl recv err, ssl_err : \{err}");
                            channel.close();
                        }
                    }
                }else {
                    rb = new ReadBuffer(received == len ? memorySegment : memorySegment.asSlice(Constants.ZERO, received));
                }
            }
        }finally {
            lock.unlock();
        }
        if (rb != null) {
            channel.onReadBuffer(rb);
        }
    }

    @Override
    public void canWrite(Channel channel) {
        lock.lock();
        try {
            if((state & SHUTDOWN_WANT_TO_WRITE) != Constants.ZERO) {
                state ^= SHUTDOWN_WANT_TO_WRITE;
                performShutdown(channel);
            }
            if((state & SEND_WANT_TO_WRITE) != Constants.ZERO) {
                state ^= SEND_WANT_TO_WRITE;
                channel.worker().submitWriterTask(new WriterTask(WriterTask.WriterTaskType.WRITABLE, channel, null, null));
            }
            if((state & RECV_WANT_TO_WRITE) != Constants.ZERO) {
                state ^= RECV_WANT_TO_WRITE;
            }
        }finally {
            lock.unlock();
        }
        if(channel.state().compareAndSet(OsNetworkLibrary.REGISTER_READ_WRITE, OsNetworkLibrary.REGISTER_READ)) {
            osNetworkLibrary.ctl(channel.worker().mux(), channel.socket(), OsNetworkLibrary.REGISTER_READ_WRITE, OsNetworkLibrary.REGISTER_READ);
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public WriteStatus doWrite(Channel channel, WriteBuffer writeBuffer) {
        boolean registerWrite = false;
        MemorySegment segment = writeBuffer.toSegment();
        int len = (int) segment.byteSize();
        lock.lock();
        try{
            int r = SslBinding.sslWrite(ssl, segment, len);
            if(r <= Constants.ZERO) {
                int err = SslBinding.sslGetErr(ssl, r);
                if(err == Constants.SSL_ERROR_WANT_WRITE) {
                    state &= SEND_WANT_TO_WRITE;
                    registerWrite = true;
                }else if(err == Constants.SSL_ERROR_WANT_READ) {
                    state &= SEND_WANT_TO_READ;
                }else {
                    log.error(STR."Failed to write, ssl err : \{err}");
                    return WriteStatus.FAILURE;
                }

            }else if(r < len){
                return doWrite(channel, writeBuffer.truncate(r));
            }else {
                return WriteStatus.SUCCESS;
            }
        }finally {
            lock.unlock();
        }
        if(registerWrite && channel.state().compareAndSet(OsNetworkLibrary.REGISTER_READ, OsNetworkLibrary.REGISTER_READ_WRITE)) {
            osNetworkLibrary.ctl(channel.worker().mux(), channel.socket(), OsNetworkLibrary.REGISTER_READ, OsNetworkLibrary.REGISTER_READ_WRITE);
        }
        return WriteStatus.PENDING;
    }

    @Override
    public void doShutdown(Channel channel) {
        performShutdown(channel);
    }

    /**
     *   Perform the actual shutdown operation, this function must be guarded by lock
     */
    private void performShutdown(Channel channel) {
        int r;
        boolean registerWrite = false, errOccur = false;
        lock.lock();
        try {
            r = SslBinding.sslShutdown(ssl);
            if(r < 0) {
                int err = SslBinding.sslGetErr(ssl, r);
                switch (err) {
                    case Constants.SSL_ERROR_WANT_READ -> state &= SHUTDOWN_WANT_TO_READ;
                    case Constants.SSL_ERROR_WANT_WRITE -> {
                        state &= SHUTDOWN_WANT_TO_WRITE;
                        registerWrite = true;
                    }
                    default -> {
                        log.error(STR."SSL shutdown failed with err : \{err}");
                        errOccur = true;
                    }
                }
            }
        }finally {
            lock.unlock();
        }
        if(registerWrite && channel.state().compareAndSet(OsNetworkLibrary.REGISTER_READ, OsNetworkLibrary.REGISTER_READ_WRITE)) {
            osNetworkLibrary.ctl(channel.worker().mux(), channel.socket(), OsNetworkLibrary.REGISTER_READ, OsNetworkLibrary.REGISTER_READ_WRITE);
        }
        if(errOccur || r == Constants.ONE) {
            // if openssl.sslShutdown() return 1, then the channel is safe to be closed
            Worker worker = channel.worker();
            if(Thread.currentThread() == worker.reader()) {
                channel.close();
            }else {
                // In this case, the channel close operation might be delayed a little bit
                worker.submitReaderTask(ReaderTask.createCloseChannelTask(channel));
            }
        }
    }

    @Override
    public void doClose(Channel channel) {
        SslBinding.sslFree(ssl);
        osNetworkLibrary.closeSocket(channel.socket());
    }
}
