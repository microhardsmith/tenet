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
        this.state = new NetworkState();
        this.thread = ThreadUtil.platform("Master", () -> {
            final Thread currentThread = Thread.currentThread();
            while (!currentThread.isInterrupted()) {
                n.waitForAccept(net, state);
            }
        });
    }

    public NetworkState state() {
        return state;
    }

    @Override
    public void init() {
        if(running.compareAndSet(false, true)) {
            log.debug("Initializing network dispatcher");
            n.createMux(config, state);
            Socket serverSocket = n.createSocket(config, true);
            state.setSocket(serverSocket);
            n.bindAndListen(config, state);
            n.registerRead(state.getMux(), state.getSocket());
            thread.start();
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public void shutdown() {
        if(running.compareAndSet(true, false)) {
            log.debug("Shutting down network dispatcher");
            thread.interrupt();
            // close mux fd
            n.exitMux(state.getMux());
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }
}
