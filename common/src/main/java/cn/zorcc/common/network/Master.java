package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.LifeCycle;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *   Network Dispatcher
 */
@Slf4j
public class Master implements LifeCycle {
    private static final Native n = Native.n;
    private final Socket socket;
    private final NetworkState state;
    private final Thread thread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public Master(Net net) {
        NetworkConfig config = net.config();
        this.socket = n.createSocket(config);
        Mux mux = n.createMux();
        MemorySegment eventsArray = n.createEventsArray(config);
        this.state = new NetworkState(mux, eventsArray, new ConcurrentHashMap<>(Net.MAP_SIZE));
        this.thread = ThreadUtil.platform("Master", () -> {
            int maxEvents = config.getMaxEvents();
            Thread currentThread = Thread.currentThread();
            try{
                n.bindAndListen(config, socket);
                n.ctl(state.mux(), socket, Native.REGISTER_NONE, Native.REGISTER_READ);
                while (!currentThread.isInterrupted()) {
                    int count = n.multiplexingWait(state, maxEvents);
                    if(count == -1) {
                        log.error("Mux wait failed with errno : {}", n.errno());
                        continue;
                    }
                    for(int index = 0; index < count; ++index) {
                        n.waitForAccept(state, index, net);
                    }
                }
            } finally {
                log.debug("Exiting network master");
                n.exitMux(state.mux());
            }
        });
    }

    public Socket socket() {
        return socket;
    }

    public NetworkState state() {
        return state;
    }

    @Override
    public void init() {
        if(running.compareAndSet(false, true)) {
            thread.start();
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public void shutdown() {
        if(running.compareAndSet(true, false)) {
            thread.interrupt();
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }
}
