package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.jctools.queues.atomic.MpscUnboundedAtomicArrayQueue;

import java.lang.foreign.MemorySegment;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 *   Network worker
 */
@Slf4j
public final class Worker {
    private final Native n = Native.n;
    private static final int INITIAL = 0;
    private static final int RUNNING = 1;
    private static final int CLOSING = 2;
    private static final int STOPPED = 3;
    private final AtomicInteger state = new AtomicInteger(INITIAL);
    private final Mux mux;
    private final MemorySegment events;
    private final Map<Socket, Object> socketMap = new HashMap<>(Net.MAP_SIZE);
    private final Queue<Task> taskQueue = new MpscUnboundedAtomicArrayQueue<>(Constants.QUEUE_SIZE);
    private final Thread thread;
    /**
     *   Representing the connections count mounted on current worker instance
     */
    private final AtomicLong counter = new AtomicLong(Constants.ZERO);

    public Worker(NetworkConfig config, int sequence) {
        this.mux = n.createMux();
        this.events = n.createEventsArray(config);
        final int maxEvents = config.getMaxEvents();
        this.thread = ThreadUtil.platform("Worker-" + sequence, () -> {
            log.debug("Initializing Net worker, sequence : {}", sequence);
            Timeout timeout = Timeout.of(config.getMuxTimeout());
            Thread currentThread = Thread.currentThread();
            try(ReadBufferArray readBufferArray = new ReadBufferArray(maxEvents)) {
                while (!currentThread.isInterrupted()) {
                    final int count = n.multiplexingWait(mux, events, maxEvents, timeout);
                    if(count == -1) {
                        log.error("Mux wait failed with errno : {}", n.errno());
                        continue;
                    }
                    for( ; ; ) {
                        Task task = taskQueue.poll();
                        if(task == null) {
                            break;
                        }else {
                            handleTask(task);
                        }
                    }
                    for(int index = 0; index < count; ++index) {
                        n.waitForData(socketMap, readBufferArray.element(index), events, index);
                    }
                }
            }finally {
                log.debug("Exiting Net worker, sequence : {}", sequence);
                n.exitMux(mux);
            }
        });
    }

    public Mux mux() {
        return mux;
    }

    public Map<Socket, Object> socketMap() {
        return socketMap;
    }

    public AtomicLong counter() {
        return counter;
    }

    /**
     *   Processing worker tasks from taskQueue
     */
    private void handleTask(Task task) {
        switch (task.type()) {
            case ADD_ACCEPTOR -> {
                Acceptor acceptor = task.acceptor();
                socketMap.put(acceptor.socket(), acceptor);
                counter.getAndIncrement();
            }
            case CLOSE_ACCEPTOR -> task.acceptor().close();
            case CLOSE_CHANNEL -> task.channel().close();
            case GRACEFUL_SHUTDOWN -> {
                if(state.compareAndSet(RUNNING, CLOSING)) {
                    Shutdown shutdown = task.shutdown();
                    if(counter.get() == 0L) {
                        state.set(STOPPED);
                        thread.interrupt();
                    }
                    socketMap.values().forEach(o -> {
                        if(o instanceof Acceptor acceptor) {
                            acceptor.close();
                        }else if(o instanceof Channel channel) {
                            channel.shutdown(shutdown);
                        }else {
                            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
                        }
                    });
                }else {
                    throw new FrameworkException(ExceptionType.NETWORK, "Worker is not running");
                }
            }
            case POSSIBLE_SHUTDOWN -> {
                if(state.compareAndSet(CLOSING, STOPPED)) {
                    thread.interrupt();
                }
            }
        }
    }

    public void submitTask(Task task) {
        if (!taskQueue.offer(task)) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
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

    public Thread thread() {
        return thread;
    }

    public void start() {
        if(state.compareAndSet(INITIAL, RUNNING)) {
            thread.start();
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }
}
