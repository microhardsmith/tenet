package cn.zorcc.common.network;

import cn.zorcc.common.AbstractLifeCycle;
import cn.zorcc.common.Clock;
import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.network.api.*;
import cn.zorcc.common.network.lib.OsNetworkLibrary;
import cn.zorcc.common.structure.Mutex;
import cn.zorcc.common.structure.Wheel;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public final class Net extends AbstractLifeCycle {
    private static final Logger log = new Logger(Net.class);
    private static final AtomicBoolean instanceFlag = new AtomicBoolean(false);
    private static final OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;
    private static final AtomicInteger counter = new AtomicInteger(0);
    private static final Provider tcpProvider = new TcpProvider();
    private static final Provider sslProvider = SslProvider.newClientProvider();
    private static final PollerConfig defaultPollerConfig = new PollerConfig();
    private static final WriterConfig defaultWriterConfig = new WriterConfig();
    private static final SocketConfig defaultSocketConfig = new SocketConfig();
    /**
     *   Default client connect timeout, could be modified according to your scenario
     */
    private static final Duration defaultConnectTimeout = Duration.ofSeconds(5);
    /**
     *   Default graceful shutdown timeout, could be modified according to your scenario
     */
    private static final Duration defaultGracefulShutdownTimeout = Duration.ofSeconds(30);
    private final State state = new State();
    private final List<Listener> listeners = new ArrayList<>();
    private final Set<Provider> providers = new HashSet<>();
    private final List<Poller> pollers;
    private final List<Writer> writers;

    public static Provider tcpProvider() {
        return tcpProvider;
    }

    public static Provider sslProvider() {
        return sslProvider;
    }

    public Net() {
        this(defaultPollerConfig, defaultWriterConfig);
    }

    public Net(PollerConfig pollerConfig) {
        this(pollerConfig, defaultWriterConfig);
    }

    public Net(WriterConfig writerConfig) {
        this(defaultPollerConfig, writerConfig);
    }

    public Net(PollerConfig pollerConfig, WriterConfig writerConfig) {
        if(!instanceFlag.compareAndSet(false, true)) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.SINGLETON_MSG);
        }
        int pollerCount = pollerConfig.getPollerCount();
        if(pollerCount <= 0) {
            throw new FrameworkException(ExceptionType.NETWORK, "Poller instances cannot be zero");
        }
        int writerCount = writerConfig.getWriterCount();
        if(writerCount <= 0) {
            throw new FrameworkException(ExceptionType.NETWORK, "Writer instances cannot be zero");
        }
        this.pollers = IntStream.range(0, pollerCount).mapToObj(_ -> new Poller(pollerConfig)).toList();
        this.writers = IntStream.range(0, writerCount).mapToObj(_ -> new Writer(writerConfig)).toList();
        addProvider(tcpProvider());
        addProvider(sslProvider());
    }

    public void addListener(ListenerConfig listenerConfig) {
        try(Mutex _ = state.withMutex()) {
            int current = state.get();
            if(current > Constants.RUNNING) {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
            Listener listener = new Listener(listenerConfig, pollers, writers);
            listeners.add(listener);
            if(current == Constants.RUNNING) {
                listener.thread().start();
            }
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

    public void connect(Loc loc, Encoder encoder, Decoder decoder, Handler handler, Provider provider, Duration duration, SocketConfig socketConfig) {
        Socket socket = osNetworkLibrary.createSocket(loc);
        osNetworkLibrary.configureClientSocket(socket, socketConfig);
        int seq = counter.getAndIncrement();
        Poller poller = pollers.get(seq % pollers.size());
        Writer writer = writers.get(seq % writers.size());
        Channel channel = new Channel(socket, encoder, decoder, handler, poller, writer, loc);
        Sentry sentry = provider.create(channel);
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment sockAddr = osNetworkLibrary.createSockAddr(loc, arena);
            if(osNetworkLibrary.connect(socket, sockAddr) == 0) {
                poller.submit(new PollerTask(PollerTaskType.BIND, channel, sentry));
            }else {
                int errno = osNetworkLibrary.errno();
                if (errno == osNetworkLibrary.connectBlockCode()) {
                    poller.submit(new PollerTask(PollerTaskType.BIND, channel, sentry));
                    Wheel.wheel().addJob(() -> poller.submit(new PollerTask(PollerTaskType.UNBIND, channel, duration)), duration);
                }else {
                    throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to connect, errno : \{errno}");
                }
            }
        }
    }

    public void connect(Loc loc, Encoder encoder, Decoder decoder, Handler handler, Provider provider, SocketConfig socketConfig) {
        connect(loc, encoder, decoder, handler, provider, defaultConnectTimeout, socketConfig);
    }

    public void connect(Loc loc, Encoder encoder, Decoder decoder, Handler handler, Provider provider, Duration duration) {
        connect(loc, encoder, decoder, handler, provider, duration, defaultSocketConfig);
    }

    public void connect(Loc loc, Encoder encoder, Decoder decoder, Handler handler, Provider provider) {
        connect(loc, encoder, decoder, handler, provider, defaultConnectTimeout, defaultSocketConfig);
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
            listeners.forEach(listener -> listener.thread().start());
        }
    }

    @Override
    protected void doExit() throws InterruptedException {
        try(Mutex _ = state.withMutex()) {
            long nano = Clock.nano();
            int current = state.get();
            if(current != Constants.RUNNING) {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
            state.set(Constants.CLOSING);
            listeners.forEach(listener -> listener.thread().interrupt());
            for (Listener listener : listeners) {
                listener.thread().join();
            }
            pollers.forEach(poller -> poller.submit(new PollerTask(PollerTaskType.EXIT, null, defaultGracefulShutdownTimeout)));
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
