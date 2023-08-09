package cn.zorcc.common.network;

import cn.zorcc.common.Clock;
import cn.zorcc.common.Constants;
import cn.zorcc.common.LifeCycle;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.pojo.Loc;
import cn.zorcc.common.util.NativeUtil;
import cn.zorcc.common.wheel.Wheel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 *   Net is the core of the whole Network application, Net consists of a Master and several Workers
 *   For a network channel, the most essential three components are : codec, handler and protocol
 *   Codec determines how the ReadBuffer should be decoded as a java object, and how to parse a java object into a writeBuffer for transferring
 *   Handler determines how we deal with the incoming data
 *   Protocol determines the low level operations
 */
public final class Net implements LifeCycle {
    private static final Logger log = LoggerFactory.getLogger(Net.class);
    private static final AtomicBoolean instanceFlag = new AtomicBoolean(false);
    private static final Native n = Native.n;
    private final NetworkConfig networkConfig;
    private final List<Master> masters = new ArrayList<>();
    private final List<Worker> workers = new ArrayList<>();
    private final MemorySegment sslClientCtx;
    private final MemorySegment sslServerCtx;
    private final Supplier<Connector> sslClientConnectorSupplier;
    private final Supplier<Connector> sslServerConnectorSupplier;
    private static final int INITIAL = 0;
    private static final int RUNNING = 1;
    private static final int SHUTDOWN = 2;
    private final AtomicInteger state = new AtomicInteger(INITIAL);
    private final AtomicInteger connectCounter = new AtomicInteger(Constants.ZERO);

    public Net(NetworkConfig networkConfig) {
        if(!instanceFlag.compareAndSet(false, true)) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.SINGLETON_MSG);
        }
        this.networkConfig = networkConfig;
        this.sslClientCtx = Openssl.sslCtxNew(Openssl.tlsClientMethod());
        Openssl.configureCtx(sslClientCtx);
        Openssl.setVerify(sslClientCtx, Constants.SSL_VERIFY_PEER, NativeUtil.NULL_POINTER);
        this.sslClientConnectorSupplier =  () -> new SslConnector(true, Openssl.sslNew(sslClientCtx));
        if(NativeUtil.checkNullPointer(sslClientCtx)) {
            throw new FrameworkException(ExceptionType.NETWORK, "SSL client initialization failed");
        }
        if(Boolean.TRUE.equals(networkConfig.getEnableSsl())) {
            this.sslServerCtx = Openssl.sslCtxNew(Openssl.tlsServerMethod());
            if(NativeUtil.checkNullPointer(sslServerCtx)) {
                throw new FrameworkException(ExceptionType.NETWORK, "SSL server initialization failed");
            }
            Openssl.configureCtx(sslServerCtx);
            try(Arena arena = Arena.ofConfined()) {
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
            this.sslServerConnectorSupplier = () -> new SslConnector(false, Openssl.sslNew(sslServerCtx));
        }else {
            this.sslServerCtx = null;
            this.sslServerConnectorSupplier = () -> {
                throw new FrameworkException(ExceptionType.NETWORK, "SSL server not configured");
            };
        }
    }

    /**
     *   Perform round-robin worker selection for worker
     *   The atomicInteger will overflow when reaching Integer.MAX_VALUE, which is totally safe here
     */
    public static Worker chooseWorker(List<Worker> workers, AtomicInteger counter) {
        final int index = Math.abs(counter.getAndIncrement() % workers.size());
        return workers.get(index);
    }

    /**
     *   Add a new Master instance to current Net, not thread safe
     */
    public void addMaster(Supplier<Encoder> e, Supplier<Decoder> d, Supplier<Handler> h, Supplier<Connector> c, Loc loc, MuxConfig muxConfig) {
        if(state.getAndSet(INITIAL) != INITIAL) {
            throw new FrameworkException(ExceptionType.NETWORK, "Can't add master when net is running or shutdown");
        }
        loc.validate();
        muxConfig.validate();
        if (masters.stream().anyMatch(master -> master.loc().equals(loc))) {
            throw new FrameworkException(ExceptionType.NETWORK, "Master already exist for target loc");
        }
        int sequence = masters.size();
        masters.add(new Master(e, d, h, c, workers, loc, networkConfig, muxConfig, sequence));
    }

    /**
     *   Add a new Worker instance to current Net, not thread safe
     */
    public void addWorker(NetworkConfig networkConfig, MuxConfig muxConfig) {
        if(state.getAndSet(INITIAL) != INITIAL) {
            throw new FrameworkException(ExceptionType.NETWORK, "Can't add worker when net is running or shutdown");
        }
        muxConfig.validate();
        int sequence = workers.size();
        workers.add(new Worker(networkConfig, muxConfig, sequence));
    }

    /**
     *   Create a new client-side ssl object
     */
    public MemorySegment newClientSsl() {
        return Openssl.sslNew(sslClientCtx);
    }

    /**
     *   Return supplier for a new client-side ssl connector
     */
    public Supplier<Connector> sslClientConnectorSupplier() {
        return sslClientConnectorSupplier;
    }

    /**
     *   Return supplier for a new server-side ssl connector
     */
    public Supplier<Connector> sslServerConnectorSupplier() {
        return sslServerConnectorSupplier;
    }

    /**
     *   Launch a client connect operation for remote server
     */
    public void connect(Loc loc, Encoder encoder, Decoder decoder, Handler handler, Connector connector, long timeout, TimeUnit timeUnit) {
        loc.validate();
        Socket socket = n.createSocket();
        n.configureSocket(networkConfig, socket);
        Worker worker = chooseWorker(workers, connectCounter);
        Acceptor acceptor = new Acceptor(socket, encoder, decoder, handler, connector, worker, loc);
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment sockAddr = n.createSockAddr(loc, arena);
            if(n.connect(socket, sockAddr) == 0) {
                mount(acceptor);
            }else {
                int errno = n.errno();
                if (errno == n.connectBlockCode()) {
                    mount(acceptor);
                    Wheel.wheel().addJob(() -> worker.submitReaderTask(new ReaderTask(ReaderTask.ReaderTaskType.CLOSE_ACCEPTOR, acceptor, null, null)), timeout, timeUnit);
                }else {
                    throw new FrameworkException(ExceptionType.NETWORK, "Failed to connect, errno : %d".formatted(errno));
                }
            }
        }
    }

    /**
     *   Launch a client connect operation for remote server, using default timeout and time-unit
     */
    @SuppressWarnings("unused")
    public void connect(Loc loc, Encoder encoder, Decoder decoder, Handler handler, Connector connector) {
        connect(loc, encoder, decoder, handler, connector, networkConfig.getDefaultConnectionTimeout(), TimeUnit.MILLISECONDS);
    }


    /**
     *   Mount target acceptor to its worker thread for write events to happen
     */
    public static void mount(Acceptor acceptor) {
        acceptor.worker().submitReaderTask(new ReaderTask(ReaderTask.ReaderTaskType.ADD_ACCEPTOR, acceptor, null, null));
        if (acceptor.state().compareAndSet(Native.REGISTER_NONE, Native.REGISTER_WRITE)) {
            n.ctl(acceptor.worker().mux(), acceptor.socket(), Native.REGISTER_NONE, Native.REGISTER_WRITE);
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public void init() {
        if(state.compareAndSet(INITIAL, RUNNING)) {
            for (Worker worker : workers) {
                if(worker.state().compareAndSet(INITIAL, RUNNING)) {
                    worker.reader().start();
                    worker.writer().start();
                }else {
                    throw new FrameworkException(ExceptionType.NETWORK, "Worker already running");
                }
            }
            for (Master master : masters) {
                master.thread().start();
            }
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, "Net already running");
        }
    }

    @Override
    public void shutdown() throws InterruptedException {
        if(state.compareAndSet(RUNNING, SHUTDOWN)) {
            long nano = Clock.nano();
            for (Master master : masters) {
                master.thread().interrupt();
            }
            for (Master master : masters) {
                master.thread().join();
            }
            for (Worker worker : workers) {
                worker.submitReaderTask(new ReaderTask(ReaderTask.ReaderTaskType.GRACEFUL_SHUTDOWN, null, null, new Shutdown(networkConfig.getGracefulShutdownTimeout(), TimeUnit.MILLISECONDS)));
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
    }
}
