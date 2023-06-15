package cn.zorcc.common.network;

import cn.zorcc.common.Clock;
import cn.zorcc.common.Constants;
import cn.zorcc.common.LifeCycle;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.pojo.Loc;
import cn.zorcc.common.util.ConfigUtil;
import cn.zorcc.common.util.NativeUtil;
import cn.zorcc.common.util.ThreadUtil;
import cn.zorcc.common.wheel.Wheel;
import lombok.extern.slf4j.Slf4j;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
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

    private final NetworkConfig config;
    private final Worker[] workers;
    private final AtomicLong counter = new AtomicLong(0L);
    private final Supplier<Encoder> encoderSupplier;
    private final Supplier<Decoder> decoderSupplier;
    private final Supplier<Handler> handlerSupplier;
    private final Supplier<Connector> connectorSupplier;
    private final MemorySegment sslClientCtx;
    private final MemorySegment sslServerCtx;
    private final Socket socket;
    private final Mux mux;
    private final MemorySegment events;
    private final Thread thread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public Net(Supplier<Encoder> e, Supplier<Decoder> d, Supplier<Handler> h, NetworkConfig config) {
        if(!instanceFlag.compareAndSet(false, true)) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.SINGLETON_MSG);
        }
        validateNetworkConfig(config);
        this.config = config;
        this.encoderSupplier = e;
        this.decoderSupplier = d;
        this.handlerSupplier = h;
        this.sslClientCtx = Openssl.sslCtxNew(Openssl.tlsClientMethod());
        Openssl.configureCtx(sslClientCtx);
        Openssl.setVerify(sslClientCtx, Constants.SSL_VERIFY_PEER, NativeUtil.NULL_POINTER);
        if(NativeUtil.checkNullPointer(sslClientCtx)) {
            throw new FrameworkException(ExceptionType.NETWORK, "SSL client initialization failed");
        }
        if(config.getEnableSsl()) {
            this.sslServerCtx = Openssl.sslCtxNew(Openssl.tlsServerMethod());
            if(NativeUtil.checkNullPointer(sslServerCtx)) {
                throw new FrameworkException(ExceptionType.NETWORK, "SSL server initialization failed");
            }
            Openssl.configureCtx(sslServerCtx);
            try(Arena arena = Arena.openConfined()) {
                MemorySegment publicKey = NativeUtil.allocateStr(arena, config.getPublicKeyFile());
                if (Openssl.setPublicKey(sslServerCtx, publicKey, Constants.SSL_FILETYPE_PEM) <= 0) {
                    Openssl.sslErrPrint(NativeUtil.stderr());
                    throw new FrameworkException(ExceptionType.NETWORK, "SSL server public key err");
                }
                MemorySegment privateKey = NativeUtil.allocateStr(arena, config.getPrivateKeyFile());
                if (Openssl.setPrivateKey(sslServerCtx, privateKey, Constants.SSL_FILETYPE_PEM) <= 0) {
                    Openssl.sslErrPrint(NativeUtil.stderr());
                    throw new FrameworkException(ExceptionType.NETWORK, "SSL server private key err");
                }
                if (Openssl.checkPrivateKey(sslServerCtx) <= 0) {
                    Openssl.sslErrPrint(NativeUtil.stderr());
                    throw new FrameworkException(ExceptionType.NETWORK, "SSL server private key and public key doesn't match");
                }
            }
            this.connectorSupplier = () -> new SslConnector(false, Openssl.sslNew(sslServerCtx));
        }else {
            this.sslServerCtx = null;
            this.connectorSupplier = TcpConnector::new;
        }
        this.workers = new Worker[config.getWorkerCount()];
        for(int sequence = 0; sequence < workers.length; sequence++) {
            workers[sequence] = new Worker(config, sequence);
        }
        this.socket = n.createSocket();
        n.configureSocket(config, socket);
        this.mux = n.createMux();
        this.events = n.createEventsArray(config);
        this.thread = createMasterThread();
    }

    private Thread createMasterThread() {
        return ThreadUtil.platform("Master", () -> {
            int maxEvents = config.getMaxEvents();
            Timeout timeout = Timeout.of(config.getMuxTimeout());
            Thread currentThread = Thread.currentThread();
            try{
                log.debug("Initializing Net master");
                n.bindAndListen(config, socket);
                n.ctl(mux, socket, Native.REGISTER_NONE, Native.REGISTER_READ);
                while (!currentThread.isInterrupted()) {
                    int count = n.multiplexingWait(mux, events, maxEvents, timeout);
                    if(count == -1) {
                        log.error("Mux wait failed with errno : {}", n.errno());
                        continue;
                    }
                    for(int index = 0; index < count; ++index) {
                        ClientSocket clientSocket = n.waitForAccept(config, socket, events, index);
                        Socket socket = clientSocket.socket();
                        Loc loc = clientSocket.loc();
                        Worker worker = nextWorker();
                        Encoder encoder = encoderSupplier.get();
                        Decoder decoder = decoderSupplier.get();
                        Handler handler = handlerSupplier.get();
                        Connector connector = connectorSupplier.get();
                        Acceptor acceptor = new Acceptor(socket, encoder, decoder, handler, connector, worker, loc);
                        mount(acceptor);
                    }
                }
            } finally {
                log.debug("Exiting Net master");
                n.exitMux(mux);
            }
        });
    }

    /**
     *   Create Net instance using default NetworkConfig
     */
    public Net(Supplier<Encoder> e, Supplier<Decoder> d, Supplier<Handler> h) {
        this(e, d, h, new NetworkConfig());
    }

    public Supplier<Encoder> encoderSupplier() {
        return encoderSupplier;
    }

    public Supplier<Decoder> decoderSupplier() {
        return decoderSupplier;
    }

    public Supplier<Handler> handlerSupplier() {
        return handlerSupplier;
    }

    public Supplier<Connector> connectorSupplier() {
        return connectorSupplier;
    }

    /**
     *   create a new client-side ssl object
     */
    public MemorySegment newSsl() {
        return Openssl.sslNew(sslClientCtx);
    }


    /**
     *   validate global network config, throw exception if illegal
     */
    private void validateNetworkConfig(NetworkConfig config) {
        String ip = config.getIp();
        if(!ConfigUtil.checkIp(ip)) {
            throw new FrameworkException(ExceptionType.NETWORK, "IpAddress is not valid : %s".formatted(ip));
        }
        Integer workerCount = config.getWorkerCount();
        if(workerCount < 1) {
            throw new FrameworkException(ExceptionType.NETWORK, "Worker count must be at least 1");
        }
    }

    /**
     *   return current network config
     */
    public NetworkConfig config() {
        return config;
    }

    /**
     *   perform round-robin worker selection for worker
     */
    public Worker nextWorker() {
        int index = (int) (counter.getAndIncrement() % workers.length);
        return workers[index];
    }

    /**
     *   Launch a client connect operation for remote server
     */
    public void connect(Loc loc, Encoder encoder, Decoder decoder, Handler handler, Supplier<Connector> connectorSupplier, long timeout, TimeUnit timeUnit) {
        Socket socket = n.createSocket();
        n.configureSocket(config, socket);
        Worker worker = nextWorker();
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
    public void connect(Loc loc, Encoder encoder, Decoder decoder, Handler handler, Supplier<Connector> connectorSupplier) {
        connect(loc, encoder, decoder, handler, connectorSupplier, DEFAULT_CONNECT_TIMEOUT, DEFAULT_TIME_UNIT);
    }

    /**
     *   Mount target acceptor to its worker thread for write events to happen
     */
    private void mount(Acceptor acceptor) {
        acceptor.worker().submitReaderTask(new ReaderTask(ReaderTask.ReaderTaskType.ADD_ACCEPTOR, acceptor, null, null));
        if (acceptor.state().compareAndSet(Native.REGISTER_NONE, Native.REGISTER_WRITE)) {
            n.ctl(acceptor.worker().mux(), acceptor.socket(), Native.REGISTER_NONE, Native.REGISTER_WRITE);
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public void init() {
        if(running.compareAndSet(false, true)) {
            for (Worker worker : workers) {
                worker.start();
            }
            thread.start();
        }
    }

    @Override
    public void shutdown() {
        try{
            long nano = Clock.nano();
            thread.interrupt();
            thread.join();
            for (Worker worker : workers) {
                worker.submitReaderTask(new ReaderTask(ReaderTask.ReaderTaskType.GRACEFUL_SHUTDOWN, null, null, new Shutdown(config.getShutdownTimeout(), TimeUnit.SECONDS)));
            }
            for(Worker worker : workers) {
                worker.reader().join();
                worker.writer().join();
            }
            n.exit();
            log.debug("Exiting Net gracefully, cost : {} ms", TimeUnit.NANOSECONDS.toMillis(Clock.elapsed(nano)));
        }catch (InterruptedException e) {
            throw new FrameworkException(ExceptionType.NETWORK, "Shutting down Net failed because of thread interruption");
        }
    }
}
