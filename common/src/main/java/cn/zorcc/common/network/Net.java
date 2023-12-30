package cn.zorcc.common.network;

import cn.zorcc.common.*;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.network.api.*;
import cn.zorcc.common.network.lib.OsNetworkLibrary;
import cn.zorcc.common.structure.IntMap;
import cn.zorcc.common.structure.IntPair;
import cn.zorcc.common.structure.Mutex;
import cn.zorcc.common.structure.Wheel;
import org.jctools.queues.atomic.MpscLinkedAtomicQueue;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public final class Net extends AbstractLifeCycle {
    private static final Logger log = new Logger(Net.class);
    private static final AtomicBoolean instanceFlag = new AtomicBoolean(false);
    private static final OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;
    private static final AtomicInteger counter = new AtomicInteger(0);
    private static final Provider tcpProvider = new TcpProvider();
    private static final Provider sslProvider = SslProvider.newClientProvider(System.getProperty(Constants.CA_FILE), System.getProperty(Constants.CA_DIR));
    private static final Listener defaultListener = new Listener(null, null, null, null, null, null, null, null);
    private static final NetConfig defaultNetConfig = new NetConfig();
    private static final PollerConfig defaultPollerConfig = new PollerConfig();
    private static final WriterConfig defaultWriterConfig = new WriterConfig();
    private static final SocketConfig defaultSocketConfig = new SocketConfig();
    /**
     *   Default client connect timeout, could be modified according to your scenario
     */
    private static final DurationWithCallback defaultDurationWithCallback = new DurationWithCallback(Duration.ofSeconds(5), null);
    private final NetConfig config;
    private final State state = new State(Constants.INITIAL);
    private final Mux mux = osNetworkLibrary.createMux();
    private final Set<Provider> providers = new HashSet<>(List.of(tcpProvider, sslProvider));
    private final List<Poller> pollers;
    private final List<Writer> writers;
    private final Duration shutdownTimeout;
    private final Thread netThread;
    private final Queue<Listener> netQueue = new MpscLinkedAtomicQueue<>();

    private Thread createNetThread() {
        return Thread.ofPlatform().unstarted(() -> {
            int maxEvents = config.getMaxEvents();
            int muxTimeout = config.getMuxTimeout();
            IntMap<Listener> listenerMap = new IntMap<>(config.getMapSize());
            try(Arena arena = Arena.ofConfined()) {
                Timeout timeout = Timeout.of(arena, muxTimeout);
                MemorySegment events = arena.allocate(MemoryLayout.sequenceLayout(maxEvents, osNetworkLibrary.eventLayout()));
                for( ; ; ) {
                    int r = osNetworkLibrary.muxWait(mux, events, maxEvents, timeout);
                    if(r < 0) {
                        int errno = Math.abs(r);
                        if(errno == osNetworkLibrary.interruptCode()) {
                            return ;
                        }else {
                            throw new FrameworkException(ExceptionType.NETWORK, STR."Mux wait failed with errno : \{errno}");
                        }
                    }
                    for( ; ; ) {
                        Listener listener = netQueue.poll();
                        if(listener == null) {
                            break ;
                        }else if(listener == defaultListener) {
                            return ;
                        }else {
                            addProvider(listener.provider());
                            Socket socket = listener.socket();
                            Loc loc = listener.loc();
                            log.info(STR."Server listener registered for \{loc}");
                            listenerMap.put(socket.intValue(), listener);
                        }
                    }
                    for(int index = 0; index < r; ++index) {
                        IntPair pair = osNetworkLibrary.access(events, index);
                        int eventType = pair.second();
                        if(eventType == Constants.NET_R) {
                            Listener listener = listenerMap.get(pair.first());
                            if(listener == null) {
                                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
                            }
                            Socket serverSocket = listener.socket();
                            Loc serverLoc = listener.loc();
                            SocketAndLoc socketAndLoc;
                            try{
                                socketAndLoc = osNetworkLibrary.accept(serverLoc, serverSocket, listener.socketConfig());
                            }catch (RuntimeException e) {
                                log.error(STR."Failed to accept connection on \{ serverLoc }", e);
                                continue ;
                            }
                            Socket clientSocket = socketAndLoc.socket();
                            Loc clientLoc = socketAndLoc.loc();
                            int seq = listener.counter().getAndIncrement();
                            Poller poller = pollers.get(seq % pollers.size());
                            Writer writer = writers.get(seq % writers.size());
                            Encoder encoder = listener.encoderSupplier().get();
                            Decoder decoder = listener.decoderSupplier().get();
                            Handler handler = listener.handlerSupplier().get();
                            Channel channel = new ChannelImpl(clientSocket, encoder, decoder, handler, poller, writer, clientLoc);
                            Sentry sentry = listener.provider().create(channel);
                            poller.submit(new PollerTask(PollerTaskType.BIND, channel, sentry));
                            osNetworkLibrary.ctlMux(poller.mux(), clientSocket, Constants.NET_NONE, Constants.NET_W);
                        }else {
                            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
                        }
                    }
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
        this(defaultNetConfig, defaultPollerConfig, defaultWriterConfig);
    }

    public Net(PollerConfig pollerConfig) {
        this(defaultNetConfig, pollerConfig, defaultWriterConfig);
    }

    public Net(WriterConfig writerConfig) {
        this(defaultNetConfig, defaultPollerConfig, writerConfig);
    }

    public Net(PollerConfig pollerConfig, WriterConfig writerConfig) {
        this(defaultNetConfig, pollerConfig, writerConfig);
    }

    public Net(NetConfig netConfig, PollerConfig pollerConfig, WriterConfig writerConfig) {
        if(netConfig == null || pollerConfig == null || writerConfig == null) {
            throw new NullPointerException();
        }
        if(!instanceFlag.compareAndSet(false, true)) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
        int pollerCount = pollerConfig.getPollerCount();
        if(pollerCount <= 0) {
            throw new FrameworkException(ExceptionType.NETWORK, "Poller instances cannot be zero");
        }
        int writerCount = writerConfig.getWriterCount();
        if(writerCount <= 0) {
            throw new FrameworkException(ExceptionType.NETWORK, "Writer instances cannot be zero");
        }
        this.config = netConfig;
        this.pollers = IntStream.range(0, pollerCount).mapToObj(_ -> new Poller(pollerConfig)).toList();
        this.writers = IntStream.range(0, writerCount).mapToObj(_ -> new Writer(writerConfig)).toList();
        this.shutdownTimeout = Duration.ofSeconds(netConfig.getGracefulShutdownTimeout());
        this.netThread = createNetThread();
    }

    public void addServerListener(ListenerConfig listenerConfig) {
        try(Mutex _ = state.withMutex()) {
            int current = state.get();
            if(current > Constants.RUNNING) {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
            Supplier<Encoder> encoderSupplier = Objects.requireNonNull(listenerConfig.getEncoderSupplier());
            Supplier<Decoder> decoderSupplier = Objects.requireNonNull(listenerConfig.getDecoderSupplier());
            Supplier<Handler> handlerSupplier = Objects.requireNonNull(listenerConfig.getHandlerSupplier());
            Provider provider = Objects.requireNonNull(listenerConfig.getProvider());
            Loc loc = Objects.requireNonNull(listenerConfig.getLoc());
            SocketConfig socketConfig = Objects.requireNonNull(listenerConfig.getSocketConfig());
            Socket socket = osNetworkLibrary.createSocket(loc);
            osNetworkLibrary.configureServerSocket(socket, loc, socketConfig);
            Listener listener = new Listener(encoderSupplier, decoderSupplier, handlerSupplier, provider, loc, new AtomicInteger(0), socket, socketConfig);
            if (!netQueue.offer(listener)) {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
            osNetworkLibrary.bindAndListen(socket, loc, config.getBacklog());
            osNetworkLibrary.ctlMux(mux, socket, Constants.NET_NONE, Constants.NET_R);
        }
    }

    public void addProvider(Provider provider) {
        try(Mutex _ = state.withMutex()) {
            if(state.get() > Constants.RUNNING) {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
            providers.add(provider);
        }
    }

    public void connect(Loc loc, Encoder encoder, Decoder decoder, Handler handler, Provider provider, SocketConfig socketConfig, DurationWithCallback durationWithCallback) {
        Socket socket = osNetworkLibrary.createSocket(loc);
        osNetworkLibrary.configureClientSocket(socket, socketConfig);
        int seq = counter.getAndIncrement();
        Poller poller = pollers.get(seq % pollers.size());
        Writer writer = writers.get(seq % writers.size());
        Channel channel = new ChannelImpl(socket, encoder, decoder, handler, poller, writer, loc);
        addProvider(provider);
        Sentry sentry = provider.create(channel);
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment sockAddr = osNetworkLibrary.createSockAddr(loc, arena);
            int r = osNetworkLibrary.connect(socket, sockAddr);
            if(r == 0) {
                poller.submit(new PollerTask(PollerTaskType.BIND, channel, sentry));
                osNetworkLibrary.ctlMux(poller.mux(), socket, Constants.NET_NONE, Constants.NET_W);
            }else if(r < 0){
                int errno = Math.abs(r);
                if (errno == osNetworkLibrary.connectBlockCode()) {
                    Duration duration = durationWithCallback.duration();
                    Runnable callback = durationWithCallback.callback();
                    poller.submit(new PollerTask(PollerTaskType.BIND, channel, callback == null ? sentry : new SentryWithCallback(sentry, callback)));
                    Wheel.wheel().addJob(() -> poller.submit(new PollerTask(PollerTaskType.UNBIND, channel, null)), duration);
                    osNetworkLibrary.ctlMux(poller.mux(), socket, Constants.NET_NONE, Constants.NET_W);
                }else {
                    throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to connect, errno : \{errno}");
                }
            }else {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
        }
    }

    public void connect(Loc loc, Encoder encoder, Decoder decoder, Handler handler, Provider provider, SocketConfig socketConfig) {
        connect(loc, encoder, decoder, handler, provider, socketConfig, defaultDurationWithCallback);
    }

    public void connect(Loc loc, Encoder encoder, Decoder decoder, Handler handler, Provider provider, DurationWithCallback durationWithCallback) {
        connect(loc, encoder, decoder, handler, provider, defaultSocketConfig, durationWithCallback);
    }

    public void connect(Loc loc, Encoder encoder, Decoder decoder, Handler handler, Provider provider) {
        connect(loc, encoder, decoder, handler, provider, defaultSocketConfig, defaultDurationWithCallback);
    }

    @Override
    protected void doInit() {
        try(Mutex _ = state.withMutex()) {
            int current = state.get();
            if(current != Constants.INITIAL) {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
            state.set(Constants.RUNNING);
            pollers.forEach(poller -> poller.thread().start());
            writers.forEach(writer -> writer.thread().start());
            netThread.start();
        }
    }

    @Override
    protected void doExit() throws InterruptedException {
        try(Mutex _ = state.withMutex()) {
            long nano = Clock.nano();
            int current = state.get();
            if(current != Constants.RUNNING) {
                return ;
            }
            state.set(Constants.CLOSING);
            if (!netQueue.offer(defaultListener)) {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
            netThread.join();
            pollers.forEach(poller -> poller.submit(new PollerTask(PollerTaskType.EXIT, null, shutdownTimeout)));
            writers.forEach(writer -> writer.submit(new WriterTask(WriterTaskType.EXIT, null, null, null)));
            for (Poller poller : pollers) {
                poller.thread().join();
            }
            for (Writer writer : writers) {
                writer.thread().join();
            }
            providers.forEach(Provider::close);
            osNetworkLibrary.exit();
            state.set(Constants.STOPPED);
            log.debug(STR."Exiting Net gracefully, cost : \{Duration.ofNanos(Clock.elapsed(nano)).toMillis()} ms");
        }
    }
}
