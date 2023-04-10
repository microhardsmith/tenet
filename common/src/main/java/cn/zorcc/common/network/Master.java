package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.LifeCycle;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 *   Network Dispatcher
 */
@Slf4j
public class Master implements LifeCycle {
    private static final Native n = Native.n;
    private final NetworkConfig config;
    private final NetworkState state;
    private final Thread thread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public Master(Net net) {
        this.config = net.config();
        this.state = NetworkState.forMaster(config);
        this.thread = ThreadUtil.platform("Master", () -> {
            log.debug("Initializing network master, mux : {}, socket : {}", state.mux(), state.socket());
            Thread currentThread = Thread.currentThread();
            try{
                n.bindAndListen(config, state.socket());
                n.registerRead(state.mux(), state.socket());
                while (!currentThread.isInterrupted()) {
                    n.waitForAccept(net, state);
                }
            } finally {
                log.debug("Exiting network master");
                n.exitMux(state.mux());
            }
        });
    }

    public static boolean inMasterThread() {
        return Thread.currentThread().getName().equals("Master");
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
