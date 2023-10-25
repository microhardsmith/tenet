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
public final class SslProtocol implements Protocol {
    private static final Logger log = new Logger(SslProtocol.class);
    private static final OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;
    private final Receiver receiver;
    private final Channel channel;
    private final MemorySegment ssl;
    private final Lock lock = new ReentrantLock();
    private int state = 0;
    private static final int RECV_WANT_TO_WRITE = 1;
    private static final int SEND_WANT_TO_READ = 1 << 1;
    private static final int SEND_WANT_TO_WRITE = 1 << 2;
    private static final int SHUTDOWN_WANT_TO_READ = 1 << 3;
    private static final int SHUTDOWN_WANT_TO_WRITE = 1 << 4;

    public SslProtocol(Receiver receiver, MemorySegment ssl) {
        this.receiver = receiver;
        this.channel = receiver.getChannel();
        this.ssl = ssl;
    }

    /**
     *   Deal with channel canRead operation
     *   while reading from ssl channel, SSL_ERROR_WANT_READ will be ignored, since read operation will always be triggered
     *   if SSL_ERROR_WANT_WRITE happened, which means current read operation is not successful because channel is not able to perform write operation
     *
     */
    @Override
    public void canRead(MemorySegment reserved) {
        ReadBuffer rb = null;
        lock.lock();
        try {
            if((state & SHUTDOWN_WANT_TO_READ) != 0) {
                state ^= SHUTDOWN_WANT_TO_READ;
                doShutdownInReaderThread(receiver);
            }
            if((state & SEND_WANT_TO_READ) != 0) {
                state ^= SEND_WANT_TO_READ;
                channel.worker().submitWriterTask(new WriterTask(WriterTask.WriterTaskType.WRITABLE, channel, null, null));
            }
            if((state & RECV_WANT_TO_WRITE) == 0) {
                int len = (int) reserved.byteSize();
                int received = SslBinding.sslRead(ssl, reserved, len);
                if(received <= 0) {
                    int err = SslBinding.sslGetErr(ssl, received);
                    switch (err) {
                        case Constants.SSL_ERROR_WANT_WRITE -> state &= RECV_WANT_TO_WRITE;
                        case Constants.SSL_ERROR_ZERO_RETURN -> doShutdownInReaderThread(receiver);
                        case Constants.SSL_ERROR_WANT_READ -> {}
                        default -> {
                            log.debug(STR."\{channel.loc()} ssl recv err, ssl_err : \{err}");
                            receiver.close();
                        }
                    }
                }else {
                    rb = new ReadBuffer(received == len ? reserved : reserved.asSlice(0, received));
                }
            }
        }finally {
            lock.unlock();
        }
        if (rb != null) {
            receiver.onChannelBuffer(rb);
        }
    }

    @Override
    public void canWrite() {
        lock.lock();
        try {
            if((state & SHUTDOWN_WANT_TO_WRITE) != 0) {
                state ^= SHUTDOWN_WANT_TO_WRITE;
                doShutdownInReaderThread(receiver);
            }
            if((state & SEND_WANT_TO_WRITE) != 0) {
                state ^= SEND_WANT_TO_WRITE;
                channel.worker().submitWriterTask(new WriterTask(WriterTask.WriterTaskType.WRITABLE, channel, null, null));
            }
            if((state & RECV_WANT_TO_WRITE) != 0) {
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
    public WriteStatus doWrite(WriteBuffer writeBuffer) {
        boolean registerWrite = false;
        MemorySegment segment = writeBuffer.toSegment();
        int len = (int) segment.byteSize();
        lock.lock();
        try{
            int r = SslBinding.sslWrite(ssl, segment, len);
            if(r <= 0) {
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
                return doWrite(writeBuffer.truncate(r));
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
    public void doShutdown() {
        doShutdownInWriterThread(channel);
    }

    /**
     *   If shutdown was immediately finished in writer thread, we need to tell the read thread to close the channel manually
     */
    private void doShutdownInWriterThread(Channel channel) {
        if(shutdown()) {
            channel.worker().submitReaderTask(new ReaderTask(ReaderTask.ReaderTaskType.CLOSE_ACTOR, null, channel, null, null));
        }
    }

    /**
     *   If shutdown was immediately finished in reader thread, we can close the channel without concern
     */
    private void doShutdownInReaderThread(Receiver receiver) {
        if(shutdown()) {
            receiver.close();
        }
    }

    /**
     *   Perform the actual shutdown operation, return if current channel needs to be immediately shutdown
     *   We have two situations that needs to be immediately shutdown:
     *   1. SSL shutdown failed, and we are not able to deal with the err code
     *   2. SSL shutdown return 1, means the remote has already closed the channel
     */
    private boolean shutdown() {
        int r, err = 0;
        lock.lock();
        try {
            r = SslBinding.sslShutdown(ssl);
            if(r < 0) {
                err = SslBinding.sslGetErr(ssl, r);
                if(err == Constants.SSL_ERROR_WANT_READ) {
                    state &= SHUTDOWN_WANT_TO_READ;
                }else if(err == Constants.SSL_ERROR_WANT_WRITE) {
                    state &= SHUTDOWN_WANT_TO_WRITE;
                }else {
                    log.error(STR."SSL shutdown failed with err : \{err}");
                }
            }
        }finally {
            lock.unlock();
        }
        if(r == Constants.SSL_ERROR_WANT_WRITE && channel.state().compareAndSet(OsNetworkLibrary.REGISTER_READ, OsNetworkLibrary.REGISTER_READ_WRITE)) {
            osNetworkLibrary.ctl(channel.worker().mux(), channel.socket(), OsNetworkLibrary.REGISTER_READ, OsNetworkLibrary.REGISTER_READ_WRITE);
        }
        return err < 0 || r == 1;
    }

    @Override
    public void doClose() {
        SslBinding.sslFree(ssl);
        osNetworkLibrary.closeSocket(channel.socket());
    }
}
