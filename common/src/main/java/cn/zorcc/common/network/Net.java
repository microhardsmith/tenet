package cn.zorcc.common.network;

import cn.zorcc.common.AbstractLifeCycle;
import cn.zorcc.common.Clock;
import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.pojo.Loc;
import cn.zorcc.common.util.NativeUtil;
import cn.zorcc.common.wheel.Wheel;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

/**
 *   Net is the core of the whole Network application, Net consists of a Master and several Workers
 *   For a network channel, the most essential three components are : codec, handler and protocol
 *   Codec determines how the ReadBuffer should be decoded as a java object, and how to parse a java object into a writeBuffer for transferring
 *   Handler determines how we deal with the incoming data
 *   Protocol determines the low level operations
 */
public final class Net extends AbstractLifeCycle {
    private static final Logger log = new Logger(Net.class);
    private static final Duration DEFAULT_CONNECTION_DURATION = Duration.ofSeconds(5);
    private static final Duration DEFAULT_GRACEFUL_SHUTDOWN_DURATION = Duration.ofSeconds(15);
    private static final int DEFAULT_WORKER_COUNT = NativeUtil.getCpuCores();
    private static final SocketOptions DEFAULT_SOCKET_OPTIONS = new SocketOptions();
    private static final AtomicBoolean instanceFlag = new AtomicBoolean(false);
    private static final OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;
    private static final Provider tcpProvider = new TcpProvider();
    private static final Provider sslProvider = SslProvider.newClientProvider();
    private final List<Master> masters = new ArrayList<>();
    private final List<Worker> workers = new ArrayList<>();
    private final Lock lock = new ReentrantLock();
    private final Set<Provider> customProviders = new HashSet<>();
    private final AtomicInteger connectCounter = new AtomicInteger(Constants.ZERO);

    public static Provider tcpProvider() {
        return tcpProvider;
    }

    public static Provider sslProvider() {
        return sslProvider;
    }

    public static Connector newTcpConnector() {
        return tcpProvider.newConnector();
    }

    public static Connector newSslConnector() {
        return sslProvider.newConnector();
    }

    public Net() {
        this(List.of(), DEFAULT_WORKER_COUNT);
    }

    public Net(int workerCount) {
        this(List.of(), workerCount);
    }

    public Net(MasterConfig masterConfigs) {
        this(List.of(masterConfigs));
    }

    public Net(MasterConfig masterConfig, List<WorkerConfig> workerConfigs) {
        this(List.of(masterConfig), workerConfigs);
    }

    public Net(MasterConfig masterConfig, int workerCount) {
        this(List.of(masterConfig), workerCount);
    }

    public Net(List<MasterConfig> masterConfigs) {
        this(masterConfigs, DEFAULT_WORKER_COUNT);
    }

    public Net(List<MasterConfig> masterConfigs, int workerCount) {
        this(masterConfigs, Stream.generate(WorkerConfig::new).limit(workerCount).toList());
    }

    public Net(List<MasterConfig> masterConfigs, List<WorkerConfig> workerConfigs) {
        if(!instanceFlag.compareAndSet(false, true)) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.SINGLETON_MSG);
        }
        if(workerConfigs == null || workerConfigs.isEmpty()) {
            throw new FrameworkException(ExceptionType.NETWORK, "Workers cannot be empty list");
        }
        for (WorkerConfig workerConfig : workerConfigs) {
            Worker worker = new Worker(workerConfig, workers.size());
            workers.add(worker);
        }
        if(masterConfigs != null && !masterConfigs.isEmpty()) {
            for (MasterConfig masterConfig : masterConfigs) {
                Master master = new Master(masterConfig, workers, masters.size());
                masters.add(master);
            }
        }
        registerProvider(tcpProvider);
        registerProvider(sslProvider);
    }

    public void registerProvider(Provider provider) {
        lock.lock();
        try{
            customProviders.add(provider);
        }finally {
            lock.unlock();
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
     *   Launch a client connect operation for remote server
     *   This method could be invoked from any thread (Platform thread would be better since native calls are involved)
     */
    public void connect(Loc loc, Encoder encoder, Decoder decoder, Handler handler, Connector connector, Duration duration, SocketOptions socketOptions) {
        Socket socket = osNetworkLibrary.createSocket(loc);
        osNetworkLibrary.configureClientSocket(socket, socketOptions);
        Worker worker = chooseWorker(workers, connectCounter);
        Acceptor acceptor = new Acceptor(socket, encoder, decoder, handler, connector, worker, loc);
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment sockAddr = osNetworkLibrary.createSockAddr(loc, arena);
            if(osNetworkLibrary.connect(socket, sockAddr) == Constants.ZERO) {
                mount(acceptor);
            }else {
                int errno = osNetworkLibrary.errno();
                if (errno == osNetworkLibrary.connectBlockCode()) {
                    mount(acceptor);
                    Wheel.wheel().addJob(() -> worker.submitReaderTask(new ReaderTask(ReaderTask.ReaderTaskType.CLOSE_ACCEPTOR, acceptor, null, null)), duration);
                }else {
                    throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to connect, errno : \{errno}");
                }
            }
        }
    }

    public void connect(Loc loc, Encoder encoder, Decoder decoder, Handler handler, Connector connector, SocketOptions socketOptions) {
        connect(loc, encoder, decoder, handler, connector, DEFAULT_CONNECTION_DURATION, socketOptions);
    }

    public void connect(Loc loc, Encoder encoder, Decoder decoder, Handler handler, Connector connector, Duration duration) {
        connect(loc, encoder, decoder, handler, connector, duration, DEFAULT_SOCKET_OPTIONS);
    }

    public void connect(Loc loc, Encoder encoder, Decoder decoder, Handler handler, Connector connector) {
        connect(loc, encoder, decoder, handler, connector, DEFAULT_CONNECTION_DURATION, DEFAULT_SOCKET_OPTIONS);
    }


    /**
     *   Mount target acceptor to its worker thread for write events to happen
     */
    public static void mount(Acceptor acceptor) {
        acceptor.worker().submitReaderTask(new ReaderTask(ReaderTask.ReaderTaskType.ADD_ACCEPTOR, acceptor, null, null));
        if (acceptor.state().compareAndSet(OsNetworkLibrary.REGISTER_NONE, OsNetworkLibrary.REGISTER_WRITE)) {
            osNetworkLibrary.ctl(acceptor.worker().mux(), acceptor.socket(), OsNetworkLibrary.REGISTER_NONE, OsNetworkLibrary.REGISTER_WRITE);
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public void doInit() {
        for (Worker worker : workers) {
            worker.reader().start();
            worker.writer().start();
        }
        for (Master master : masters) {
            master.thread().start();
        }
    }

    @Override
    public void doExit() throws InterruptedException {
        lock.lock();
        try{
            long nano = Clock.nano();
            for (Master master : masters) {
                master.thread().interrupt();
            }
            for (Master master : masters) {
                master.thread().join();
            }
            for (Worker worker : workers) {
                worker.submitReaderTask(new ReaderTask(ReaderTask.ReaderTaskType.GRACEFUL_SHUTDOWN, null, null, DEFAULT_GRACEFUL_SHUTDOWN_DURATION));
            }
            for(Worker worker : workers) {
                worker.reader().join();
                worker.writer().join();
            }
            customProviders.forEach(Provider::close);
            osNetworkLibrary.exit();
            log.debug(STR."Exiting Net gracefully, cost : \{Duration.ofNanos(Clock.elapsed(nano)).toMillis()} ms");
        }finally {
            lock.unlock();
        }
    }
}
