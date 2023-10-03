package cn.zorcc.orm.core;

import cn.zorcc.common.Constants;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.binding.SslBinding;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.network.*;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 *   TODO refactor
 */
public class PgConnector implements Connector {
    private static final Logger log = new Logger(PgConnector.class);
    private static final OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;
    private final Net net;
    private static final int INITIAL = 0;
    private static final int SSL_UNWRITTEN = 1;
    private static final int SSL_WAIT = 2;
    private static final int SSL_WANT_READ = 3;
    private static final int SSL_WANT_WRITE = 4;
    private final AtomicInteger status = new AtomicInteger(INITIAL);
    private final AtomicReference<MemorySegment> sslReference = new AtomicReference<>(NativeUtil.NULL_POINTER);
    private final AtomicReference<WriteBuffer> unwritten = new AtomicReference<>(null);
    public PgConnector(Net net) {
        this.net = net;
    }

    @Override
    public void doClose(Acceptor acceptor) {
        MemorySegment ssl = sslReference.getAndSet(null);
        if(!NativeUtil.checkNullPointer(ssl)) {
            SslBinding.sslFree(ssl);
        }
        osNetworkLibrary.closeSocket(acceptor.socket());
    }

    @Override
    public void canRead(Acceptor acceptor, MemorySegment buffer) {
        final int i = status.get();
        if(i == SSL_WAIT) {
            try(Arena arena = Arena.ofConfined()) {
                MemorySegment segment = arena.allocate(ValueLayout.JAVA_BYTE);
                if (osNetworkLibrary.recv(acceptor.socket(), segment, (int) segment.byteSize()) < Constants.ZERO) {
                    log.error(STR."Unable to read, errno : \{ osNetworkLibrary.errno()}");
                    acceptor.close();
                }
                byte b = NativeUtil.getByte(segment, 0L);
                if(b == PgConstants.SSL_OK) {
                    MemorySegment ssl = NativeUtil.NULL_POINTER; // TODO refactor this class
                    int r = SslBinding.sslSetFd(ssl, acceptor.socket().intValue());
                    if(r == 0) {
                        log.error(STR."Failed to set fd for ssl, err : \{SslBinding.sslGetErr(ssl, r)}");
                        acceptor.close();
                    }else {
                        sslReference.set(ssl);
                        doSslConnect(acceptor, ssl);
                    }
                }else if(b == PgConstants.SSL_DISABLE) {
                    acceptor.toChannel(new TcpProtocol());
                }else {
                    log.error(STR."Unable to process SSL initialize msg, byte : \{b}");
                    acceptor.close();
                }
            }
        }else if(i == SSL_WANT_READ) {
            doSslConnect(acceptor, sslReference.get());
        }
    }

    @Override
    public void canWrite(Acceptor acceptor) {
        final int i = status.get();
        if(i == INITIAL) {
            Socket socket = acceptor.socket();
            int errOpt = osNetworkLibrary.getErrOpt(socket);
            if(errOpt == 0) {
                long len = 8L;
                WriteBuffer writeBuffer = WriteBuffer.newDefaultWriteBuffer(Arena.ofConfined(), len);
                writeBuffer.writeInt(8);
                writeBuffer.writeInt(PgConstants.SSL_CODE);
                write(acceptor, writeBuffer);
            }else {
                log.error(STR."Unable to write, errno : \{ osNetworkLibrary.errno()}");
                acceptor.close();
            }
        }else if(i == SSL_UNWRITTEN) {
            write(acceptor, unwritten.getAndSet(null));
        }else if(i == SSL_WANT_WRITE) {
            doSslConnect(acceptor, sslReference.get());
        }
    }

    private void doSslConnect(Acceptor acceptor, MemorySegment ssl) {
        int c = SslBinding.sslConnect(ssl);
        if(c == 1) {
            acceptor.toChannel(new SslProtocol(ssl));
        }else {
            int err = SslBinding.sslGetErr(ssl, c);
            if(err == Constants.SSL_ERROR_WANT_READ) {
                status.set(SSL_WANT_READ);
            }else if(err == Constants.SSL_ERROR_WANT_WRITE) {
                status.set(SSL_WANT_WRITE);
                int current = acceptor.state().getAndUpdate(i -> (i & OsNetworkLibrary.REGISTER_WRITE) == 0 ? i + OsNetworkLibrary.REGISTER_WRITE : i);
                if((current & OsNetworkLibrary.REGISTER_WRITE) == 0) {
                    osNetworkLibrary.ctl(acceptor.worker().mux(), acceptor.socket(), current, current + OsNetworkLibrary.REGISTER_READ);
                }
            }else {
                log.error(STR."Failed to perform ssl handshake, ssl err : \{err}");
                acceptor.close();
            }
        }
    }

    private void write(Acceptor acceptor, WriteBuffer writeBuffer) {
        MemorySegment segment = writeBuffer.content();
        int len = (int) segment.byteSize();
        int bytes = osNetworkLibrary.send(acceptor.socket(), segment, len);
        if(bytes == -1) {
            int errno = osNetworkLibrary.errno();
            if(errno == osNetworkLibrary.sendBlockCode()) {
                status.set(SSL_UNWRITTEN);
                unwritten.set(writeBuffer);
            }else {
                log.error(STR."Failed to establish postgresql connection, errno : \{errno}");
                acceptor.close();
            }
        }else if(bytes < len) {
            write(acceptor, writeBuffer.truncate(bytes));
        }else {
            writeBuffer.close();
            status.set(SSL_WAIT);
            if (acceptor.state().compareAndSet(OsNetworkLibrary.REGISTER_WRITE, OsNetworkLibrary.REGISTER_READ)) {
                osNetworkLibrary.ctl(acceptor.worker().mux(), acceptor.socket(), OsNetworkLibrary.REGISTER_WRITE, OsNetworkLibrary.REGISTER_READ);
            }else {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
        }
    }


}
