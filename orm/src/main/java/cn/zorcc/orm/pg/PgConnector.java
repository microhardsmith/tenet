package cn.zorcc.orm.pg;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.*;
import cn.zorcc.common.util.NativeUtil;
import cn.zorcc.orm.DatabaseConfig;
import lombok.extern.slf4j.Slf4j;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class PgConnector implements Connector {
    /**
     *   Temp buffer size for containing the read content
     */
    private static final long BUFFER_SIZE = 8192;
    private static final Native n = Native.n;
    private final DatabaseConfig databaseConfig;
    private final AtomicReference<MemorySegment> sslReference = new AtomicReference<>(NativeUtil.NULL_POINTER);
    private static final int TCP_SEND_WANT_WRITE = 1 << 2;
    private static final int SSL_RECV_WANT_WRITE = 1 << 3;
    private static final int SSL_SEND_WANT_READ = 1 << 4;
    private static final int SSL_SEND_WANT_WRITE = 1 << 5;
    private final AtomicInteger rwStatus = new AtomicInteger(0);

    private static final int INITIAL = 1;
    private static final int AWAIT_SSL = 2;
    private final AtomicInteger dataStatus = new AtomicInteger(INITIAL);
    private final AtomicReference<WriteBuffer> unreadBuffer = new AtomicReference<>(null);
    private final AtomicReference<WriteBuffer> unwrittenBuffer = new AtomicReference<>(null);
    public PgConnector(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    @Override
    public void shouldClose(Socket socket) {
        MemorySegment ssl = sslReference.getAndSet(null);
        if(!NativeUtil.checkNullPointer(ssl)) {
            Openssl.sslFree(ssl);
        }
        n.closeSocket(socket);
    }

    @Override
    public void shouldRead(Acceptor acceptor) {
        int i = rwStatus.get();
        if((i & SSL_SEND_WANT_READ) != 0) {
            rwStatus.set(i ^ SSL_SEND_WANT_READ);
            doWriteSsl(acceptor, sslReference.get(), unwrittenBuffer.getAndSet(null));
        }else if((i & SSL_RECV_WANT_WRITE) == 0) {
            doRead(acceptor);
        }
    }

    @Override
    public void shouldWrite(Acceptor acceptor) {
        if(acceptor.state().compareAndSet(Native.REGISTER_READ_WRITE, Native.REGISTER_READ)) {
            NetworkState workerState = acceptor.worker().state();
            n.ctl(workerState.mux(), acceptor.socket(), Native.REGISTER_READ_WRITE, Native.REGISTER_READ);
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
        if(dataStatus.get() == INITIAL) {
            processInitial(acceptor);
        }
        int i = rwStatus.get();
        if((i & TCP_SEND_WANT_WRITE) != 0) {
            rwStatus.set(i ^ TCP_SEND_WANT_WRITE);
            doWriteTcp(acceptor, unwrittenBuffer.getAndSet(null));
        }else if((i & SSL_RECV_WANT_WRITE) != 0) {
            rwStatus.set(i ^ SSL_RECV_WANT_WRITE);
        }else if((i & SSL_SEND_WANT_WRITE) != 0) {
            rwStatus.set(i ^ SSL_SEND_WANT_WRITE);
            doWriteSsl(acceptor, sslReference.get(), unwrittenBuffer.getAndSet(null));
        }
    }

    private void processInitial(Acceptor acceptor) {
        int errOpt = n.getErrOpt(acceptor.socket());
        if(errOpt == 0) {
            dataStatus.set(AWAIT_SSL);
            WriteBuffer writeBuffer = new WriteBuffer(8);
            writeBuffer.writeInt(8);
            writeBuffer.writeInt(80877103);
            doWrite(acceptor, writeBuffer);
        }else {
            log.error("Failed to establish postgresql connection, errno : {}", n.errno());
            acceptor.close();
        }
    }

    private void doRead(Acceptor acceptor) {
        MemorySegment ssl = sslReference.get();
        if(NativeUtil.checkNullPointer(ssl)) {
            doReadTcp(acceptor);
        }else {
            doReadSsl(acceptor, ssl);
        }
    }

    private void doReadTcp(Acceptor acceptor) {
        try(ReadBuffer readBuffer = new ReadBuffer(BUFFER_SIZE)) {
            long bytes = n.recv(acceptor.socket(), readBuffer.segment(), BUFFER_SIZE);
            if(bytes > 0) {
                readBuffer.setWriteIndex(bytes);
                handle(acceptor, readBuffer);
            }else if(bytes == 0) {
                acceptor.close();
            }else {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
        }
    }

    private void doReadSsl(Acceptor acceptor, MemorySegment ssl) {
        try(ReadBuffer readBuffer = new ReadBuffer(BUFFER_SIZE)) {
            int r = Openssl.sslRead(ssl, readBuffer.segment(), (int) BUFFER_SIZE);
            if(r > 0) {
                readBuffer.setWriteIndex(r);
                handle(acceptor, readBuffer);
            }else {
                int err = Openssl.sslGetErr(ssl, r);
                if(err == Constants.SSL_ERROR_WANT_WRITE) {
                    rwStatus.updateAndGet(i -> i & SSL_RECV_WANT_WRITE);
                    if(acceptor.state().compareAndSet(Native.REGISTER_READ, Native.REGISTER_READ_WRITE)) {
                        NetworkState workerState = acceptor.worker().state();
                        n.ctl(workerState.mux(), acceptor.socket(), Native.REGISTER_READ, Native.REGISTER_READ_WRITE);
                    }
                }else if(err == Constants.SSL_ERROR_SYSCALL || err == Constants.SSL_ERROR_ZERO_RETURN) {
                    acceptor.close();
                }else if(err != Constants.SSL_ERROR_WANT_READ) {
                    throw new FrameworkException(ExceptionType.NETWORK, "SSL read failed with err : %d".formatted(err));
                }
            }
        }
    }

    private void doWrite(Acceptor acceptor, WriteBuffer writeBuffer) {
        MemorySegment ssl = sslReference.get();
        if(NativeUtil.checkNullPointer(ssl)) {
            doWriteTcp(acceptor, writeBuffer);
        }else {
            doWriteSsl(acceptor, ssl, writeBuffer);
        }
    }

    private void doWriteTcp(Acceptor acceptor, WriteBuffer writeBuffer) {
        MemorySegment segment = writeBuffer.segment();
        long len = segment.byteSize();
        long bytes = n.send(acceptor.socket(), segment, len);
        if(bytes == -1L) {
            int errno = n.errno();
            if(errno == n.sendBlockCode()) {
                rwStatus.updateAndGet(i -> i & TCP_SEND_WANT_WRITE);
                unwrittenBuffer.set(writeBuffer);
                if(acceptor.state().compareAndSet(Native.REGISTER_READ, Native.REGISTER_READ_WRITE)) {
                    NetworkState workerState = acceptor.worker().state();
                    n.ctl(workerState.mux(), acceptor.socket(), Native.REGISTER_READ, Native.REGISTER_READ_WRITE);
                }
            }else {
                acceptor.close();
            }
        }else if(bytes < len) {
            writeBuffer.truncate(bytes);
            doWriteTcp(acceptor, writeBuffer);
        }else {
            writeBuffer.close();
        }
    }

    private void doWriteSsl(Acceptor acceptor, MemorySegment ssl, WriteBuffer writeBuffer) {
        MemorySegment segment = writeBuffer.segment();
        int len = (int) segment.byteSize();
        int r = Openssl.sslWrite(ssl, segment, len);
        if(r <= 0) {
            int err = Openssl.sslGetErr(ssl, r);
            if(err == Constants.SSL_ERROR_WANT_WRITE) {
                rwStatus.updateAndGet(i -> i & SSL_SEND_WANT_WRITE);
                unwrittenBuffer.set(writeBuffer);
                if(acceptor.state().compareAndSet(Native.REGISTER_READ, Native.REGISTER_READ_WRITE)) {
                    NetworkState workerState = acceptor.worker().state();
                    n.ctl(workerState.mux(), acceptor.socket(), Native.REGISTER_READ, Native.REGISTER_READ_WRITE);
                }
            }else if(err == Constants.SSL_ERROR_WANT_READ) {
                rwStatus.updateAndGet(i -> i & SSL_SEND_WANT_READ);
                unwrittenBuffer.set(writeBuffer);
            }else {
                acceptor.close();
            }
        }else if(r < len) {
            writeBuffer.truncate(r);
            doWriteSsl(acceptor, ssl, writeBuffer);
        }else {
            writeBuffer.close();
        }
    }

    private void handle(Acceptor acceptor, ReadBuffer readBuffer) {
        WriteBuffer unread = unreadBuffer.getAndSet(null);
        if(unread != null) {
            unread.write(readBuffer.remaining());
            try(ReadBuffer buffer = unread.toReadBuffer()) {
                onReadBuffer(acceptor, buffer);
            }
        }else {
            onReadBuffer(acceptor, readBuffer);
        }
    }

    private void onReadBuffer(Acceptor acceptor, ReadBuffer readBuffer) {
        switch (dataStatus.get()) {
            case AWAIT_SSL -> processSsl(acceptor, readBuffer);
        }
        if(readBuffer.remains()) {
            WriteBuffer writeBuffer = new WriteBuffer(BUFFER_SIZE);
            writeBuffer.write(readBuffer.remaining());
            unreadBuffer.set(writeBuffer);
        }
    }

    private void processSsl(Acceptor acceptor, ReadBuffer readBuffer) {

    }
}
