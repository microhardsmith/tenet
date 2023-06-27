package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.pojo.Loc;
import cn.zorcc.common.util.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.foreign.MemorySegment;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 *   Network master
 */
@Slf4j
public final class Master {
    private static final Native n = Native.n;
    private final Supplier<Encoder> encoderSupplier;
    private final Supplier<Decoder> decoderSupplier;
    private final Supplier<Handler> handlerSupplier;
    private final Supplier<Connector> connectorSupplier;
    private final Loc loc;
    private final NetworkConfig networkConfig;
    private final MuxConfig muxConfig;
    private final int sequence;
    /**
     *   In current implementation, a master will always be bounded to all the workers
     */
    private final List<Worker> workers;
    private final Socket socket;
    private final Mux mux;
    private final MemorySegment events;
    private final Thread thread;
    private final AtomicBoolean running = new AtomicBoolean(false);
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
        this.events = n.createEventsArray(muxConfig);
        this.thread = createMasterThread();
    }

    private Thread createMasterThread() {
        return ThreadUtil.platform("Master-" + sequence, () -> {
            final int maxEvents = muxConfig.getMaxEvents();
            final Timeout timeout = Timeout.of(muxConfig.getMuxTimeout());
            Thread currentThread = Thread.currentThread();
            try{
                log.debug("Initializing Net master, sequence : {}", sequence);
                n.bindAndListen(loc, muxConfig, socket);
                n.ctl(mux, socket, Native.REGISTER_NONE, Native.REGISTER_READ);
                while (!currentThread.isInterrupted()) {
                    int count = n.multiplexingWait(mux, events, maxEvents, timeout);
                    if(count == -1) {
                        log.error("Mux wait failed with errno : {}", n.errno());
                        continue;
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
            }
        });
    }

    public Loc loc() {
        return loc;
    }

    public void start() {
        if(running.compareAndSet(false, true)) {
            thread.start();
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    public void exit() throws InterruptedException {
        if(running.compareAndSet(true, false)) {
            thread.interrupt();
            thread.join();
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }
}
