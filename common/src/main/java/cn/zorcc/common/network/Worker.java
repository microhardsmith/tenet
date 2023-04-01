package cn.zorcc.common.network;

import cn.zorcc.common.LifeCycle;
import cn.zorcc.common.ReadBuffer;
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
        this.state = new NetworkState();
        this.readBuffers = new ReadBuffer[config.getMaxEvents()];
        this.thread = ThreadUtil.platform("Worker-" + sequence, () -> {
            try{
                Thread currentThread = Thread.currentThread();
                for(int i = 0; i < readBuffers.length; i++) {
                    readBuffers[i] = new ReadBuffer(config.getReadBufferSize());
                }
                while (!currentThread.isInterrupted()) {
                    n.waitForData(readBuffers, state);
                }
            } finally {
                // close current read buffers
                for (ReadBuffer readBuffer : readBuffers) {
                    readBuffer.close();
                }
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
            log.info("Initializing network worker, sequence : {}", sequence);
            n.createMux(config, state);
            int mapSize = config.getMapSize();
            if(NativeUtil.isWindows()) {
                state.setLongMap(new ConcurrentHashMap<>(mapSize));
            }else {
                state.setIntMap(new ConcurrentHashMap<>(mapSize));
            }
            thread.start();
        }
    }

    @Override
    public void shutdown() {
        if(running.compareAndSet(true, false)) {
            log.debug("Shutting down network worker, sequence : {}", sequence);
            thread.interrupt();
            // close all existing channels
            if(NativeUtil.isWindows()) {
                Map<Long, Channel> longMap = state.getLongMap();
                longMap.values().forEach(Channel::shutdown);
            }else {
                Map<Integer, Channel> intMap = state.getIntMap();
                intMap.values().forEach(Channel::shutdown);
            }
            // close mux fd
            n.exitMux(state.getMux());
        }

    }
}
