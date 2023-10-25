package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.structure.Loc;
import cn.zorcc.common.util.ThreadUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 *   Network master
 */
public final class Master {
    private static final Logger log = new Logger(Master.class);
    private static final OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;
    private final Thread thread;
    private final AtomicInteger acceptCounter = new AtomicInteger(0);

    public Master(MasterConfig masterConfig, List<Worker> workers, int sequence) {
        this.thread = createMasterThread(masterConfig, workers, sequence);
    }

    private Thread createMasterThread(MasterConfig masterConfig, List<Worker> workers, int sequence) {
        return ThreadUtil.platform("Master-" + sequence, () -> {
            final Supplier<Encoder> encoderSupplier = masterConfig.getEncoderSupplier();
            final Supplier<Decoder> decoderSupplier = masterConfig.getDecoderSupplier();
            final Supplier<Handler> handlerSupplier = masterConfig.getHandlerSupplier();
            final Provider provider = masterConfig.getProvider();
            final Loc loc = masterConfig.getLoc();
            final int maxEvents = masterConfig.getMaxEvents();
            final int muxTimeout = masterConfig.getMuxTimeout();
            final int backlog = masterConfig.getBacklog();
            final Thread currentThread = Thread.currentThread();
            final Socket socket = osNetworkLibrary.createSocket(loc);
            osNetworkLibrary.configureServerSocket(socket, loc, masterConfig.getSocketOptions());
            final Mux mux = osNetworkLibrary.createMux();
            try(Arena arena = Arena.ofConfined()){
                log.debug(STR."Master start listening on port : \{loc.port()}, sequence : \{sequence}");
                Timeout timeout = Timeout.of(arena, muxTimeout);
                MemorySegment events = arena.allocate(MemoryLayout.sequenceLayout(maxEvents, osNetworkLibrary.eventLayout()));
                osNetworkLibrary.bindAndListen(socket, loc, backlog);
                osNetworkLibrary.ctl(mux, socket, OsNetworkLibrary.REGISTER_NONE, OsNetworkLibrary.REGISTER_READ);
                while (!currentThread.isInterrupted()) {
                    int count = osNetworkLibrary.muxWait(mux, events, maxEvents, timeout);
                    if(count < 0) {
                        int errno = osNetworkLibrary.errno();
                        if(errno == osNetworkLibrary.interruptCode()) {
                            continue;
                        }else {
                            throw new FrameworkException(ExceptionType.NETWORK, STR."Multiplexing wait failed with errno : \{errno}");
                        }
                    }
                    for(int index = 0; index < count; ++index) {
                        osNetworkLibrary.masterWait(socket, events, index);
                        ClientSocket clientSocket = osNetworkLibrary.accept(masterConfig, loc, socket);
                        Worker worker = Net.chooseWorker(workers, acceptCounter);
                        Encoder encoder = encoderSupplier.get();
                        Decoder decoder = decoderSupplier.get();
                        Handler handler = handlerSupplier.get();
                        Connector connector = provider.newConnector();
                        Channel channel = new Channel(clientSocket.socket(), encoder, decoder, handler, worker, clientSocket.loc(), new AtomicInteger(OsNetworkLibrary.REGISTER_NONE));
                        Net.mount(connector, channel);
                    }
                }
            } finally {
                log.debug(STR."Exiting Net master, sequence : \{sequence}");
                provider.close();
                osNetworkLibrary.exitMux(mux);
                osNetworkLibrary.closeSocket(socket);
            }
        });
    }

    public Thread thread() {
        return thread;
    }
}
