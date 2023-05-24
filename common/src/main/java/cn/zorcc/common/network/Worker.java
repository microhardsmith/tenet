package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.LifeCycle;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *   Network worker
 */
@Slf4j
public final class Worker implements LifeCycle {
    private final Native n = Native.n;
    private final NetworkState state;
    private final Thread thread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public Worker(Net net, int sequence) {
        NetworkConfig config = net.config();
        Mux mux = n.createMux();
        MemorySegment eventsArray = n.createEventsArray(config);
        this.state = new NetworkState(mux, eventsArray, new ConcurrentHashMap<>(Net.MAP_SIZE));
        int maxEvents = config.getMaxEvents();
        this.thread = ThreadUtil.platform("Worker-" + sequence, () -> {
            Thread currentThread = Thread.currentThread();
            try(ReadBufferArray readBufferArray = new ReadBufferArray(maxEvents)) {
                while (!currentThread.isInterrupted()) {
                    int count = n.multiplexingWait(state, maxEvents);
                    if(count == -1) {
                        log.error("Mux wait failed with errno : {}", n.errno());
                        continue;
                    }
                    for(int index = 0; index < count; ++index) {
                        n.waitForData(state, index, readBufferArray.element(index));
                    }
                }
            }finally {
                log.debug("Exiting network worker, sequence : {}", sequence);
                n.exitMux(state.mux());
            }
        });
    }

    @SuppressWarnings("resource")
    private static class ReadBufferArray implements AutoCloseable {
        private final ReadBuffer[] readBuffers;
        public ReadBufferArray(int size) {
            this.readBuffers = new ReadBuffer[size];
            for(int i = 0; i < readBuffers.length; i++) {
                readBuffers[i] = new ReadBuffer(Net.READ_BUFFER_SIZE);
            }
        }

        public ReadBuffer element(int index) {
            return readBuffers[index];
        }

        @Override
        public void close() {
            for (ReadBuffer readBuffer : readBuffers) {
                if(readBuffer != null) {
                    readBuffer.close();
                }
            }
        }
    }

    /**
     *   Examine if current operation runs in the worker thread
     *   TODO Could be removed after validation
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
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }
}
