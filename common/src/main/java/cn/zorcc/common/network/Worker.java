package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.LifeCycle;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;
import cn.zorcc.common.util.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *   Network worker
 */
@Slf4j
public final class Worker implements LifeCycle {
    private final Native n = Native.n;
    private final NetworkConfig config;
    private final int sequence;
    private final NetworkState state;
    private final Thread thread;
    private final ReadBuffer[] readBuffers;
    private final AtomicBoolean running = new AtomicBoolean(false);
    public Worker(Net net, int sequence) {
        this.config = net.config();
        this.sequence = sequence;
        this.state = NetworkState.forWorker(config);
        this.readBuffers = new ReadBuffer[config.getMaxEvents()];
        this.thread = ThreadUtil.platform("Worker-" + sequence, () -> {
            log.info("Initializing network worker, mux : {}, sequence : {}", state.mux(), sequence);
            Thread currentThread = Thread.currentThread();
            try{
                for(int i = 0; i < readBuffers.length; i++) {
                    readBuffers[i] = new ReadBuffer(config.getReadBufferSize());
                }
                while (!currentThread.isInterrupted()) {
                    n.waitForData(readBuffers, state);
                }
            } finally {
                log.debug("Exiting network worker, sequence : {}", sequence);
                for (ReadBuffer readBuffer : readBuffers) {
                    readBuffer.close();
                }
                n.exitMux(state.mux());
            }
        });
    }

    /**
     *   判断当前线程是否为worker线程
     */
    public static boolean inWorkerThread() {
        return Thread.currentThread().getName().startsWith("Worker-");
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
        }
    }
}
