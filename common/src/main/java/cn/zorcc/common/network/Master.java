package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.pojo.Loc;
import cn.zorcc.common.util.ThreadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 *   Network master
 */
public final class Master {
    private static final Logger log = LoggerFactory.getLogger(Master.class);
    private static final Native n = Native.n;
    private final Supplier<Encoder> encoderSupplier;
    private final Supplier<Decoder> decoderSupplier;
    private final Supplier<Handler> handlerSupplier;
    private final Supplier<Connector> connectorSupplier;
    /**
     *   In current implementation, a master will always be bounded to all the workers
     */
    private final List<Worker> workers;
    private final Loc loc;
    private final NetworkConfig networkConfig;
    private final MuxConfig muxConfig;
    private final int sequence;
    private final Socket socket;
    private final Mux mux;
    private final Thread thread;
    /**
     *   To perform round-robin worker selection for each master instance
     */
    private final AtomicInteger acceptCounter = new AtomicInteger(Constants.ZERO);

    public Master(Supplier<Encoder> e, Supplier<Decoder> d, Supplier<Handler> h, Supplier<Connector> c,
                  List<Worker> workers, Loc loc, NetworkConfig networkConfig, MuxConfig muxConfig, int sequence) {
        this.encoderSupplier = e;
        this.decoderSupplier = d;
        this.handlerSupplier = h;
        this.connectorSupplier = c;
        this.workers = Collections.unmodifiableList(workers);
        this.loc = loc;
        this.networkConfig = networkConfig;
        this.muxConfig = muxConfig;
        this.sequence = sequence;
        this.socket = n.createSocket();
        n.configureSocket(networkConfig, socket);
        this.mux = n.createMux();
        this.thread = createMasterThread();
    }

    private Thread createMasterThread() {
        return ThreadUtil.platform("Master-" + sequence, () -> {
            int maxEvents = muxConfig.getMaxEvents();
            Thread currentThread = Thread.currentThread();
            try(Arena arena = Arena.ofConfined()){
                log.debug("Master start listening on port : {}, sequence : {}, ", loc.port(), sequence);
                Timeout timeout = Timeout.of(arena, muxConfig.getMuxTimeout());
                MemorySegment events = n.createEventsArray(muxConfig, arena);
                n.bindAndListen(loc, muxConfig, socket);
                n.ctl(mux, socket, Native.REGISTER_NONE, Native.REGISTER_READ);
                while (!currentThread.isInterrupted()) {
                    int count = n.multiplexingWait(mux, events, maxEvents, timeout);
                    if(count < Constants.ZERO) {
                        int errno = n.errno();
                        if(errno == n.interruptCode()) {
                            continue;
                        }else {
                            throw new FrameworkException(ExceptionType.NETWORK, "Multiplexing wait failed with errno : %d".formatted(errno));
                        }
                    }
                    for(int index = 0; index < count; ++index) {
                        ClientSocket clientSocket = n.waitForAccept(networkConfig, socket, events, index);
                        Socket socket = clientSocket.socket();
                        Loc loc = clientSocket.loc();
                        Worker worker = Net.chooseWorker(workers, acceptCounter);
                        Encoder encoder = encoderSupplier.get();
                        Decoder decoder = decoderSupplier.get();
                        Handler handler = handlerSupplier.get();
                        Connector connector = connectorSupplier.get();
                        Acceptor acceptor = new Acceptor(socket, encoder, decoder, handler, connector, worker, loc);
                        Net.mount(acceptor);
                    }
                }
            } finally {
                log.debug("Exiting Net master, sequence : {}", sequence);
                n.exitMux(mux);
                n.closeSocket(socket);
            }
        });
    }

    public Loc loc() {
        return loc;
    }

    public Thread thread() {
        return thread;
    }
}
