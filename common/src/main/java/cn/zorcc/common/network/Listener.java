package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.network.api.*;
import cn.zorcc.common.network.lib.OsNetworkLibrary;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class Listener {
    private static final Logger log = new Logger(Listener.class);
    private static final AtomicInteger counter = new AtomicInteger(0);
    private static final OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;
    private final Thread listenerThread;
    public Listener(ListenerConfig listenerConfig, List<Poller> pollers, List<Writer> writers) {
        this.listenerThread = createListenerThread(listenerConfig, pollers, writers);
    }

    private Thread createListenerThread(ListenerConfig listenerConfig, List<Poller> pollers, List<Writer> writers) {
        int sequence = counter.getAndIncrement();
        return Thread.ofPlatform().name("Listener-" + sequence).unstarted(() -> {
            Supplier<Encoder> encoderSupplier = listenerConfig.getEncoderSupplier();
            Supplier<Decoder> decoderSupplier = listenerConfig.getDecoderSupplier();
            Supplier<Handler> handlerSupplier = listenerConfig.getHandlerSupplier();
            Provider provider = listenerConfig.getProvider();
            Loc loc = listenerConfig.getLoc();
            int maxEvents = listenerConfig.getMaxEvents();
            int muxTimeout = listenerConfig.getMuxTimeout();
            int backlog = listenerConfig.getBacklog();
            Thread currentThread = Thread.currentThread();
            Socket socket = osNetworkLibrary.createSocket(loc);
            osNetworkLibrary.configureServerSocket(socket, loc, listenerConfig.getSocketOptions());
            Mux mux = osNetworkLibrary.createMux();
            AtomicInteger counter = new AtomicInteger(0);
            try(Arena arena = Arena.ofConfined()) {
                log.info(STR."Start listening on port : \{loc.port()}, sequence : \{sequence}");
                Timeout timeout = Timeout.of(arena, muxTimeout);
                MemorySegment events = arena.allocate(MemoryLayout.sequenceLayout(maxEvents, osNetworkLibrary.eventLayout()));
                osNetworkLibrary.bindAndListen(socket, loc, backlog);
                osNetworkLibrary.ctl(mux, socket, 0, Constants.NET_R);
                while (!currentThread.isInterrupted()) {
                    int count = osNetworkLibrary.muxWait(mux, events, maxEvents, timeout);
                    if(count < 0) {
                        int errno = osNetworkLibrary.errno();
                        if(errno == osNetworkLibrary.interruptCode()) {
                            return ;
                        }else {
                            throw new FrameworkException(ExceptionType.NETWORK, STR."Mux wait failed with errno : \{errno}");
                        }
                    }
                    for(int index = 0; index < count; ++index) {
                        osNetworkLibrary.listenerAccess(socket, events, index);
                        SocketAndLoc socketAndLoc = osNetworkLibrary.accept(listenerConfig, loc, socket);
                        int seq = counter.getAndIncrement();
                        Poller poller = pollers.get(seq % pollers.size());
                        Writer writer = writers.get(seq % writers.size());
                        Encoder encoder = encoderSupplier.get();
                        Decoder decoder = decoderSupplier.get();
                        Handler handler = handlerSupplier.get();
                        Channel channel = new Channel(socketAndLoc.socket(), encoder, decoder, handler, poller, writer, socketAndLoc.loc());
                        Sentry sentry = provider.create(channel);
                        poller.submit(new PollerTask(PollerTaskType.BIND, channel, sentry));
                    }
                }
            } finally {
                log.info(STR."Exiting Net master, sequence : \{sequence}");
                provider.close();
                osNetworkLibrary.exitMux(mux);
                osNetworkLibrary.closeSocket(socket);
            }
        });
    }

    public Thread thread() {
        return listenerThread;
    }
}
