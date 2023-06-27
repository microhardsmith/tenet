package cn.zorcc.common.network;

import cn.zorcc.common.Clock;
import cn.zorcc.common.Constants;
import cn.zorcc.common.LifeCycle;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.pojo.Loc;
import cn.zorcc.common.util.NativeUtil;
import cn.zorcc.common.wheel.Wheel;
import lombok.extern.slf4j.Slf4j;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 *   Net is the core of the whole Network application, Net consists of a Master and several Workers
 *   For a network channel, the most essential three components are : codec, handler and protocol
 *   Codec determines how the ReadBuffer should be decoded as a java object, and how to parse a java object into a writeBuffer for transferring
 *   Handler determines how we deal with the incoming data
 *   Protocol determines the low level operations
 */
@Slf4j
public class Net implements LifeCycle {
    /**
     *  read buffer maximum size for a read operation, could be changed according to specific circumstances
     */
    public static final long READ_BUFFER_SIZE = 16 * Constants.KB;
    /**
     *  write buffer initial size, will automatically expand, could be changed according to specific circumstances
     */
    public static final int WRITE_BUFFER_SIZE = 4 * Constants.KB;
    /**
     *  socket map initial size, will automatically expand, could be changed according to specific circumstances
     */
    public static final int MAP_SIZE = 1024;
    /**
     *  default connect timeout for client side
     */
    public static final long DEFAULT_CONNECT_TIMEOUT = 5;
    /**
     *  default connect time-unit for client side
     */
    public static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;

    private static final AtomicBoolean instanceFlag = new AtomicBoolean(false);
    private static final Native n = Native.n;

    private final NetworkConfig networkConfig;
    private final List<Master> masters = new ArrayList<>();
    private final List<Worker> workers = new ArrayList<>();
    private final MemorySegment sslClientCtx;
    private final MemorySegment sslServerCtx;
    private final Supplier<Connector> tcpConnectorSupplier = TcpConnector::new;
    private final Supplier<Connector> sslConnectorSupplier;
    private static final int INITIAL = 0;
    private static final int RUNNING = 1;
    private static final int SHUTDOWN = 2;
    private int state = INITIAL;
    /**
     *   To perform round-robin worker selection when executing connect()
     */
    private final AtomicInteger connectCounter = new AtomicInteger(Constants.ZERO);
    private final Lock lock = new ReentrantLock();

    public Net(NetworkConfig networkConfig) {
        if(!instanceFlag.compareAndSet(false, true)) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.SINGLETON_MSG);
        }
        this.networkConfig = networkConfig;
        this.sslClientCtx = Openssl.sslCtxNew(Openssl.tlsClientMethod());
        Openssl.configureCtx(sslClientCtx);
        Openssl.setVerify(sslClientCtx, Constants.SSL_VERIFY_PEER, NativeUtil.NULL_POINTER);
        if(NativeUtil.checkNullPointer(sslClientCtx)) {
            throw new FrameworkException(ExceptionType.NETWORK, "SSL client initialization failed");
        }
        if(networkConfig.getEnableSsl()) {
            this.sslServerCtx = Openssl.sslCtxNew(Openssl.tlsServerMethod());
            if(NativeUtil.checkNullPointer(sslServerCtx)) {
                throw new FrameworkException(ExceptionType.NETWORK, "SSL server initialization failed");
            }
            Openssl.configureCtx(sslServerCtx);
            try(Arena arena = Arena.openConfined()) {
                MemorySegment publicKey = NativeUtil.allocateStr(arena, networkConfig.getPublicKeyFile());
                if (Openssl.setPublicKey(sslServerCtx, publicKey, Constants.SSL_FILETYPE_PEM) <= 0) {
                    throw new FrameworkException(ExceptionType.NETWORK, "SSL server public key err");
                }
                MemorySegment privateKey = NativeUtil.allocateStr(arena, networkConfig.getPrivateKeyFile());
                if (Openssl.setPrivateKey(sslServerCtx, privateKey, Constants.SSL_FILETYPE_PEM) <= 0) {
                    throw new FrameworkException(ExceptionType.NETWORK, "SSL server private key err");
                }
                if (Openssl.checkPrivateKey(sslServerCtx) <= 0) {
                    throw new FrameworkException(ExceptionType.NETWORK, "SSL server private key and public key doesn't match");
                }
            }
            this.sslConnectorSupplier = () -> new SslConnector(false, Openssl.sslNew(sslServerCtx));
        }else {
            this.sslServerCtx = null;
            this.sslConnectorSupplier = () -> {
                throw new FrameworkException(ExceptionType.NETWORK, "SSL server not configured");
            };
        }
    }

    /**
     *   Create Net instance using default NetworkConfig
     */
    public Net() {
        this(new NetworkConfig());
    }

    /**
     *   Perform round-robin worker selection for worker
     *   The atomicInteger will overflow when reaching Integer.MAX_VALUE, which is totally safe here
     */
    public static Worker chooseWorker(List<Worker> workers, AtomicInteger counter) {
        int index = counter.getAndIncrement() % workers.size();
        return workers.get(index);
    }

    /**
     *   Add a new Master instance to current Net
     */
    public void addMaster(Supplier<Encoder> e, Supplier<Decoder> d, Supplier<Handler> h, Supplier<Connector> c, Loc loc, MuxConfig muxConfig) {
        lock.lock();
        try{
            if(state != INITIAL) {
                throw new FrameworkException(ExceptionType.NETWORK, "Can't add master when net is running or shutdown");
            }
            loc.validate();
            muxConfig.validate();
            if (masters.stream().anyMatch(master -> master.loc().equals(loc))) {
                throw new FrameworkException(ExceptionType.NETWORK, "Master already exist for target loc");
            }
            int sequence = masters.size();
            masters.add(new Master(e, d, h, c, workers, loc, networkConfig, muxConfig, sequence));
        }finally {
            lock.unlock();
        }
    }

    /**
     *   Add a new Worker instance to current Net
     */
    public void addWorker(MuxConfig muxConfig) {
        lock.lock();
        try{
            if(state != INITIAL) {
                throw new FrameworkException(ExceptionType.NETWORK, "Can't add worker when net is running or shutdown");
            }
            muxConfig.validate();
            int sequence = workers.size();
            workers.add(new Worker(muxConfig, sequence));
        }finally {
            lock.unlock();
        }
    }

    /**
     *   Create a new client-side ssl object
     */
    public MemorySegment newClientSsl() {
        return Openssl.sslNew(sslClientCtx);
    }

    /**
     *   Return supplier for a new tcp connector
     */
    public Supplier<Connector> tcpConnectorSupplier() {
        return tcpConnectorSupplier;
    }

    /**
     *   Return supplier for a new ssl connector
     */
    public Supplier<Connector> sslConnectorSupplier() {
        return sslConnectorSupplier;
    }

    /**
     *   Launch a client connect operation for remote server
     */
    public void connect(Loc loc, Encoder encoder, Decoder decoder, Handler handler, Supplier<Connector> connectorSupplier, long timeout, TimeUnit timeUnit) {
        loc.validate();
        Socket socket = n.createSocket();
        n.configureSocket(networkConfig, socket);
        Worker worker = chooseWorker(workers, connectCounter);
        Connector connector = connectorSupplier.get();
        Acceptor acceptor = new Acceptor(socket, encoder, decoder, handler, connector, worker, loc);
        try(Arena arena = Arena.openConfined()) {
            MemorySegment sockAddr = n.createSockAddr(loc, arena);
            if(n.connect(socket, sockAddr) == 0) {
                // connection successfully established
                mount(acceptor);
            }else {
                int errno = n.errno();
                if (errno == n.connectBlockCode()) {
                    // connection is still in-process
                    mount(acceptor);
                    Wheel.wheel().addJob(() -> worker.submitReaderTask(new ReaderTask(ReaderTask.ReaderTaskType.CLOSE_ACCEPTOR, acceptor)), timeout, timeUnit);
                }else {
                    throw new FrameworkException(ExceptionType.NETWORK, "Failed to connect, errno : %d".formatted(errno));
                }
            }
        }
    }


    /**
     *   Mount target acceptor to its worker thread for write events to happen
     */
    public static void mount(Acceptor acceptor) {
        acceptor.worker().submitReaderTask(new ReaderTask(ReaderTask.ReaderTaskType.ADD_ACCEPTOR, acceptor));
        if (acceptor.state().compareAndSet(Native.REGISTER_NONE, Native.REGISTER_WRITE)) {
            n.ctl(acceptor.worker().mux(), acceptor.socket(), Native.REGISTER_NONE, Native.REGISTER_WRITE);
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    /**
     *   Launch a client connect operation for remote server, using default timeout and time-unit
     */
    @SuppressWarnings("unused")
    public void connect(Loc loc, Encoder encoder, Decoder decoder, Handler handler, Supplier<Connector> connectorSupplier) {
        connect(loc, encoder, decoder, handler, connectorSupplier, DEFAULT_CONNECT_TIMEOUT, DEFAULT_TIME_UNIT);
    }

    @Override
    public void init() {
        lock.lock();
        try{
            if(state == INITIAL) {
                state = RUNNING;
                // worker must be started before master
                for (Worker worker : workers) {
                    worker.start();
                }
                for (Master master : masters) {
                    master.start();
                }
            }else {
                throw new FrameworkException(ExceptionType.NETWORK, "Net already running");
            }
        }finally {
            lock.unlock();
        }
    }

    @Override
    public void shutdown() {
        lock.lock();
        try{
            if(state == RUNNING) {
                state = SHUTDOWN;
                long nano = Clock.nano();
                for (Master master : masters) {
                    master.exit();
                }
                for (Worker worker : workers) {
                    worker.submitReaderTask(new ReaderTask(ReaderTask.ReaderTaskType.GRACEFUL_SHUTDOWN, new Shutdown(networkConfig.getShutdownTimeout(), TimeUnit.SECONDS)));
                }
                for(Worker worker : workers) {
                    worker.reader().join();
                    worker.writer().join();
                }
                n.exit();
                Openssl.sslCtxFree(sslClientCtx);
                if(sslServerCtx != null) {
                    Openssl.sslCtxFree(sslServerCtx);
                }
                log.debug("Exiting Net gracefully, cost : {} ms", TimeUnit.NANOSECONDS.toMillis(Clock.elapsed(nano)));
            }else {
                throw new FrameworkException(ExceptionType.NETWORK, "Net already shutdown");
            }
        }catch (InterruptedException e) {
            throw new FrameworkException(ExceptionType.NETWORK, "Shutting down Net failed because of thread interruption");
        }finally {
            lock.unlock();
        }
    }
}
