package cn.zorcc.common.network;

import cn.zorcc.common.*;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.structure.*;
import cn.zorcc.common.util.ConfigUtil;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public final class Net implements LifeCycle {
    private static final Logger log = new Logger(Net.class);
    private static final AtomicBoolean instanceFlag = new AtomicBoolean(false);
    private static final OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;
    private static final AtomicInteger counter = new AtomicInteger(0);
    private static final Provider tcpProvider = Provider.newTcpProvider();
    private static final Provider sslProvider = Provider.newSslClientProvider(System.getProperty(Constants.CA_FILE), System.getProperty(Constants.CA_DIR));
    private static final ListenerTask EXIT_TASK = new ListenerTask(null, null, null, null, null, null, null);
    private static final NetConfig defaultNetConfig = new NetConfig();
    private static final SocketConfig defaultSocketConfig = new SocketConfig();
    private static final Duration defaultDuration = Duration.ofSeconds(5);
    private final NetConfig config;
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();
    private final Mux mux = osNetworkLibrary.createMux();
    private final Set<Provider> clientProviders = ConcurrentHashMap.newKeySet();
    private final List<ListenerTask> pendingTasks = new CopyOnWriteArrayList<>();
    private final List<Poller> pollers;
    private final List<Writer> writers;
    private final Duration shutdownTimeout;
    private final Thread netThread;
    private final TaskQueue<ListenerTask> netQueue;
    private int state;

    private Thread createNetThread() {
        return Thread.ofPlatform().unstarted(() -> {
            MemApi memApi = config.isEnableRpMalloc() ? RpMalloc.tInitialize() : MemApi.DEFAULT;
            int maxEvents = config.getMaxEvents();
            int muxTimeout = config.getMuxTimeout();
            IntMap<ListenerTask> listenerMap = IntMap.newTreeMap(config.getMapSize());
            Set<Provider> serverProviders = new HashSet<>();
            try(Allocator allocator = Allocator.newDirectAllocator(memApi)) {
                Timeout timeout = Timeout.of(allocator, muxTimeout);
                MemorySegment events = allocator.allocate(MemoryLayout.sequenceLayout(maxEvents, osNetworkLibrary.eventLayout()));
                int counter = 0;
                for( ; ; ) {
                    int r = osNetworkLibrary.waitMux(mux, events, maxEvents, timeout);
                    for (ListenerTask listenerTask : netQueue.elements()) {
                        if(listenerTask == EXIT_TASK) {
                            return ;
                        }else {
                            serverProviders.add(listenerTask.provider());
                            Socket socket = listenerTask.socket();
                            Loc loc = listenerTask.loc();
                            log.info(STR."Server listenerTask registered for \{loc}");
                            listenerMap.put(socket.intValue(), listenerTask);
                        }
                    }
                    for(int index = 0; index < r; ++index) {
                        MuxEvent muxEvent = osNetworkLibrary.access(events, index);
                        long eventType = muxEvent.event();
                        if(eventType == Constants.NET_R) {
                            ListenerTask listenerTask = listenerMap.get(muxEvent.socket());
                            if(listenerTask == null) {
                                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
                            }
                            Socket serverSocket = listenerTask.socket();
                            Loc serverLoc = listenerTask.loc();
                            SocketAndLoc socketAndLoc;
                            try{
                                socketAndLoc = osNetworkLibrary.accept(serverLoc, serverSocket, listenerTask.socketConfig(), memApi);
                            }catch (RuntimeException e) {
                                log.error(STR."Failed to accept connection on \{ serverLoc }", e);
                                continue ;
                            }
                            Socket clientSocket = socketAndLoc.socket();
                            Loc clientLoc = socketAndLoc.loc();
                            int seq = counter++;
                            Poller poller = pollers.get(seq % pollers.size());
                            Writer writer = writers.get(seq % writers.size());
                            Encoder encoder = listenerTask.encoderSupplier().get();
                            Decoder decoder = listenerTask.decoderSupplier().get();
                            Handler handler = listenerTask.handlerSupplier().get();
                            Channel channel = Channel.newChannel(clientSocket, encoder, decoder, handler, poller, writer, clientLoc);
                            Sentry sentry = listenerTask.provider().create(channel);
                            poller.submit(new PollerTask(PollerTaskType.BIND, channel, sentry));
                            osNetworkLibrary.ctlMux(poller.mux(), clientSocket, Constants.NET_NONE, Constants.NET_W, memApi);
                        }else {
                            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
                        }
                    }
                }
            } finally {
                serverProviders.forEach(Provider::close);
                if(config.isEnableRpMalloc()) {
                    RpMalloc.tRelease();
                }
            }
        });
    }

    public static Provider tcpProvider() {
        return tcpProvider;
    }

    public static Provider sslProvider() {
        return sslProvider;
    }

    public Net() {
        this(defaultNetConfig);
    }

    public Net(NetConfig config) {
        if(!instanceFlag.compareAndSet(false, true)) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
        validateConfig(config);
        this.config = config;
        this.netQueue = new TaskQueue<>(config.getQueueSize());
        this.pollers = IntStream.range(0, config.getPollerCount()).mapToObj(_ -> Poller.newPoller(config)).toList();
        this.writers = IntStream.range(0, config.getWriterCount()).mapToObj(_ -> Writer.newWriter(config)).toList();
        this.shutdownTimeout = Duration.ofSeconds(config.getGracefulShutdownTimeout());
        this.netThread = createNetThread();
    }

    private void validateConfig(NetConfig config) {
        if(config == null) {
            throw new NullPointerException();
        }
        ConfigUtil.checkParam(config.getMaxEvents(), 0, Integer.MAX_VALUE);
        ConfigUtil.checkParam(config.getMuxTimeout(), 5, 100);
        ConfigUtil.checkParam(config.getBacklog(), 16, Constants.KB);
        ConfigUtil.checkParam(config.getMapSize(), 4, Constants.KB);
        ConfigUtil.checkParam(config.getQueueSize(), 0, Constants.KB);
        ConfigUtil.checkParam(config.getGracefulShutdownTimeout(), 0, 300);
        ConfigUtil.checkParam(config.getPollerCount(), 0, NativeUtil.getCpuCores());
        ConfigUtil.checkParam(config.getPollerQueueSize(), 0, 16 * Constants.KB);
        ConfigUtil.checkParam(config.getPollerMaxEvents(), 0, Constants.KB);
        ConfigUtil.checkParam(config.getPollerMuxTimeout(), 5, Integer.MAX_VALUE);
        ConfigUtil.checkParam(config.getPollerBufferSize(), Constants.KB, 16 * Constants.MB);
        ConfigUtil.checkParam(config.getPollerMapSize(), 16, 16 * Constants.KB);
        ConfigUtil.checkParam(config.getWriterCount(), 0, NativeUtil.getCpuCores());
        ConfigUtil.checkParam(config.getWriterBufferSize(), Constants.KB, 16 * Constants.MB);
        ConfigUtil.checkParam(config.getPollerMapSize(), 16, 16 * Constants.KB);
    }

    /**
     *   Register a listener to current Net instance, the server would be listening when the Net instance got initialized
     */
    public void serve(ListenerConfig listenerConfig) {
        readLock.lock();
        try{
            if(state > Constants.RUNNING) {
                return ;
            }
            Supplier<Encoder> encoderSupplier = Objects.requireNonNull(listenerConfig.getEncoderSupplier());
            Supplier<Decoder> decoderSupplier = Objects.requireNonNull(listenerConfig.getDecoderSupplier());
            Supplier<Handler> handlerSupplier = Objects.requireNonNull(listenerConfig.getHandlerSupplier());
            Provider provider = Objects.requireNonNull(listenerConfig.getProvider());
            Loc loc = Objects.requireNonNull(listenerConfig.getLoc());
            SocketConfig socketConfig = Objects.requireNonNull(listenerConfig.getSocketConfig());
            Socket socket = osNetworkLibrary.createSocket(loc);
            osNetworkLibrary.configureServerSocket(socket, loc, socketConfig);
            pendingTasks.add(new ListenerTask(encoderSupplier, decoderSupplier, handlerSupplier, provider, loc, socket, socketConfig));
        } finally {
            readLock.unlock();
        }
    }

    public void connect(Loc loc, Encoder encoder, Decoder decoder, Handler handler, Provider provider, SocketConfig socketConfig, Duration duration) {
        readLock.lock();
        try{
            if(state > Constants.RUNNING) {
                return ;
            }
            Socket socket = osNetworkLibrary.createSocket(loc);
            osNetworkLibrary.configureClientSocket(socket, socketConfig);
            int seq = counter.getAndIncrement();
            Poller poller = pollers.get(seq % pollers.size());
            Writer writer = writers.get(seq % writers.size());
            Channel channel = Channel.newChannel(socket, encoder, decoder, handler, poller, writer, loc);
            if(provider != tcpProvider && provider != sslProvider) {
                clientProviders.add(provider);
            }
            Sentry sentry = provider.create(channel);
            osNetworkLibrary.useSockAddr(loc, MemApi.DEFAULT, sockAddr -> {
                int r = osNetworkLibrary.connect(socket, sockAddr);
                if(r == 0) {
                    poller.submit(new PollerTask(PollerTaskType.BIND, channel, sentry));
                    osNetworkLibrary.ctlMux(poller.mux(), socket, Constants.NET_NONE, Constants.NET_W, MemApi.DEFAULT);
                }else if(r < 0){
                    int errno = Math.abs(r);
                    if (errno == osNetworkLibrary.connectBlockCode()) {
                        poller.submit(new PollerTask(PollerTaskType.BIND, channel, sentry));
                        Wheel.wheel().addJob(() -> poller.submit(new PollerTask(PollerTaskType.UNBIND, channel, null)), duration);
                        osNetworkLibrary.ctlMux(poller.mux(), socket, Constants.NET_NONE, Constants.NET_W, MemApi.DEFAULT);
                    }else {
                        throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to connect, errno : \{errno}");
                    }
                }else {
                    throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
                }
            });
        } finally {
            readLock.unlock();
        }
    }

    public void connect(Loc loc, Encoder encoder, Decoder decoder, Handler handler, Provider provider, SocketConfig socketConfig) {
        connect(loc, encoder, decoder, handler, provider, socketConfig, defaultDuration);
    }

    public void connect(Loc loc, Encoder encoder, Decoder decoder, Handler handler, Provider provider, Duration duration) {
        connect(loc, encoder, decoder, handler, provider, defaultSocketConfig, duration);
    }

    public void connect(Loc loc, Encoder encoder, Decoder decoder, Handler handler, Provider provider) {
        connect(loc, encoder, decoder, handler, provider, defaultSocketConfig, defaultDuration);
    }

    @Override
    public void init() {
        writeLock.lock();
        try{
            if(state != Constants.INITIAL) {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
            if(config.isEnableRpMalloc()) {
                RpMalloc.initialize();
            }
            pollers.forEach(poller -> poller.pollerThread().start());
            writers.forEach(writer -> writer.writerThread().start());
            pendingTasks.forEach(listenerTask -> {
                netQueue.offer(listenerTask);
                osNetworkLibrary.bindAndListen(listenerTask.socket(), listenerTask.loc(), MemApi.DEFAULT, config.getBacklog());
                osNetworkLibrary.ctlMux(mux, listenerTask.socket(), Constants.NET_NONE, Constants.NET_R, MemApi.DEFAULT);
            });
            netThread.start();
            state = Constants.RUNNING;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void exit() {
        writeLock.lock();
        try{
            long nano = Clock.nano();
            if(state != Constants.RUNNING) {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
            state = Constants.CLOSING;
            netQueue.offer(EXIT_TASK);
            netThread.join();
            pollers.forEach(poller -> poller.submit(new PollerTask(PollerTaskType.EXIT, null, shutdownTimeout)));
            writers.forEach(writer -> writer.submit(new WriterTask(WriterTaskType.EXIT, null, null, null)));
            for (Poller poller : pollers) {
                poller.pollerThread().join();
            }
            for (Writer writer : writers) {
                writer.writerThread().join();
            }
            clientProviders.forEach(Provider::close);
            osNetworkLibrary.exit();
            if(config.isEnableRpMalloc()) {
                RpMalloc.release();
            }
            log.debug(STR."Exiting Net gracefully, cost : \{Duration.ofNanos(Clock.elapsed(nano)).toMillis()} ms");
            state = Constants.STOPPED;
        } catch (InterruptedException e) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
        } finally {
            writeLock.unlock();
        }
    }
}
