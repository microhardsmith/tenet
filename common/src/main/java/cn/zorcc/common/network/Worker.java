package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Mix;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ThreadUtil;
import cn.zorcc.common.wheel.Wheel;
import lombok.extern.slf4j.Slf4j;
import org.jctools.queues.atomic.MpscUnboundedAtomicArrayQueue;

import java.lang.foreign.MemorySegment;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 *   Network worker
 */
@Slf4j
public final class Worker {
    /**
     *   Max write operations in one writer thread loop
     */
    private static final int MAX_WRITES_PER_LOOP = 16;
    private final Native n = Native.n;
    private static final int INITIAL = 0;
    private static final int RUNNING = 1;
    private static final int CLOSING = 2;
    private static final int STOPPED = 3;
    private final AtomicInteger state = new AtomicInteger(INITIAL);
    private final Mux mux;
    private final MemorySegment events;
    private final Map<Socket, Object> socketMap = new HashMap<>(Net.MAP_SIZE);
    private final Map<Channel, Sender> channelMap = new HashMap<>(Net.MAP_SIZE);
    private final Queue<ReaderTask> readerTaskQueue = new MpscUnboundedAtomicArrayQueue<>(Constants.QUEUE_SIZE);
    private final TransferQueue<WriterTask> writerTaskQueue = new LinkedTransferQueue<>();
    private final Thread reader;
    private final Thread writer;
    /**
     *   Representing the connections count mounted on current worker instance
     *   This field might be optimized to normal long since it's only accessed by reader thread, AtomicLong is more secure however
     */
    private final AtomicLong counter = new AtomicLong(Constants.ZERO);

    public Worker(NetworkConfig config, int sequence) {
        this.mux = n.createMux();
        this.events = n.createEventsArray(config);
        this.reader = createReaderThread(sequence, config);
        this.writer = createWriterThread(sequence);
    }
    
    private final class Sender {
        /**
         *   Whether current sender has not been shutdown, normally it's null
         */
        private Shutdown shutdown;
        /**
         *   TempBuffer exist means current channel is not writable, incoming data should be written into tempBuffer first, normally it's null
         */
        private WriteBuffer tempBuffer;

        public void send(Channel channel, Object msg) {
            switch (msg) {
                case Boolean b -> {
                    if(Boolean.TRUE.equals(b)) {
                        // channel become writable now, try flush the tempBuffer
                        flushTempBuffer(channel);
                    }else {
                        // force close the sender
                        channelMap.remove(channel);
                        if(tempBuffer != null) {
                            tempBuffer.close();
                        }
                    }
                }
                case Shutdown s -> {
                    if(tempBuffer == null) {
                        // current channel could be safely shutdown
                        doShutdown(channel, s);
                    }else {
                        // still have some underlying data to be transferred
                        shutdown = s;
                    }
                }
                case Mix mix -> {
                    if(shutdown == null) {
                        try(WriteBuffer writeBuffer = new WriteBuffer(Net.WRITE_BUFFER_SIZE)) {
                            for (Object o : mix.objects()) {
                                channel.encoder().encode(writeBuffer, o);
                            }
                            if(writeBuffer.notEmpty()) {
                                doSend(channel, writeBuffer);
                            }
                        }
                    }
                }
                default -> {
                    if(shutdown == null) {
                        try(WriteBuffer writeBuffer = new WriteBuffer(Net.WRITE_BUFFER_SIZE)) {
                            channel.encoder().encode(writeBuffer, msg);
                            if(writeBuffer.notEmpty()) {
                                doSend(channel, writeBuffer);
                            }
                        }
                    }
                }
            }
        }

        /**
         *   Try send writeBuffer, will write into tempBuffer if not writable
         */
        private void doSend(Channel channel, WriteBuffer writeBuffer) {
            if(tempBuffer == null) {
                switch (channel.protocol().doWrite(channel, writeBuffer)) {
                    case PENDING -> {
                        // the data has not yet transferred completely
                        tempBuffer = new WriteBuffer(Net.WRITE_BUFFER_SIZE);
                        tempBuffer.write(writeBuffer.segment());
                    }
                    case FAILURE -> {
                        // channel should be closed, suspend afterwards writing
                        channelMap.remove(channel);
                        submitReaderTask(new ReaderTask(ReaderTask.ReaderTaskType.CLOSE_CHANNEL, null, channel, null));
                    }
                }
            }else {
                // dump current writeBuffer to the temp
                tempBuffer.write(writeBuffer.segment());
            }
        }

        /**
         *   Try flush the tempBuffer, if current channel has been shutdown, perform actual shutdown operation
         */
        private void flushTempBuffer(Channel channel) {
            if(tempBuffer != null) {
                switch (channel.protocol().doWrite(channel, tempBuffer)) {
                    case SUCCESS -> {
                        tempBuffer.close();
                        tempBuffer = null;
                        if(shutdown != null) {
                            // pending buffer has been flushed, do shutdown right now
                            doShutdown(channel, shutdown);
                        }
                    }
                    case FAILURE -> {
                        // channel should be closed, suspend afterwards writing
                        channelMap.remove(channel);
                        submitReaderTask(new ReaderTask(ReaderTask.ReaderTaskType.CLOSE_CHANNEL, null, channel, null));
                    }
                }
            }
        }

        /**
         *   Perform the actual shutdown operation, no longer receiving write tasks
         */
        private void doShutdown(Channel channel, Shutdown s) {
            channelMap.remove(channel);
            channel.protocol().doShutdown(channel);
            Wheel.wheel().addJob(() -> submitReaderTask(new ReaderTask(ReaderTask.ReaderTaskType.CLOSE_CHANNEL, null, channel, null)), s.timeout(), s.timeUnit());
        }
    }

    /**
     *   Create worker's reader thread
     */
    private Thread createReaderThread(int sequence, NetworkConfig config) {
        final int maxEvents = config.getMaxEvents();
        return ThreadUtil.platform("Worker-r-" + sequence, () -> {
            log.debug("Initializing Net worker's reader, sequence : {}", sequence);
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
                        ReaderTask readerTask = readerTaskQueue.poll();
                        if(readerTask == null) {
                            break;
                        }else {
                            handleTask(readerTask);
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

    /**
     *   Create worker's writer thread
     */
    private Thread createWriterThread(int sequence) {
        return ThreadUtil.platform("Worker-w-" + sequence, () -> {
            log.debug("Initializing Net worker's writer, sequence : {}", sequence);
            try{
                for( ; ; ) {
                    WriterTask writerTask = writerTaskQueue.take();
                    if(writerTask == WriterTask.INTERRUPT_TASK) {
                        // exit writer thread
                        break ;
                    }
                    Channel channel = writerTask.channel();
                    Object msg = writerTask.msg();
                    if(msg == null) {
                        // perform channel registry
                        channelMap.put(channel, new Sender());
                    }else {
                        // perform actual send operation
                        Sender sender = channelMap.get(channel);
                        if(sender != null) {
                            sender.send(channel, msg);
                        }
                    }
                }
            }catch (InterruptedException i) {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED, i);
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
    private void handleTask(ReaderTask readerTask) {
        switch (readerTask.type()) {
            case ADD_ACCEPTOR -> {
                Acceptor acceptor = readerTask.acceptor();
                socketMap.put(acceptor.socket(), acceptor);
                counter.getAndIncrement();
            }
            case CLOSE_ACCEPTOR -> readerTask.acceptor().close();
            case CLOSE_CHANNEL -> readerTask.channel().close();
            case GRACEFUL_SHUTDOWN -> {
                if(state.compareAndSet(RUNNING, CLOSING)) {
                    Shutdown shutdown = readerTask.shutdown();
                    if(counter.get() == 0L) {
                        // current worker has no channel bound, could be directly closed
                        state.set(STOPPED);
                        reader.interrupt();
                        submitWriterTask(WriterTask.INTERRUPT_TASK);
                    }else {
                        // shutdown every channel
                        socketMap.values().forEach(o -> {
                            if(o instanceof Acceptor acceptor) {
                                acceptor.close();
                            }else if(o instanceof Channel channel) {
                                channel.shutdown(shutdown);
                            }else {
                                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
                            }
                        });
                    }
                }else {
                    throw new FrameworkException(ExceptionType.NETWORK, "Worker is not running");
                }
            }
            case POSSIBLE_SHUTDOWN -> {
                if(state.compareAndSet(CLOSING, STOPPED)) {
                    reader.interrupt();
                    submitWriterTask(WriterTask.INTERRUPT_TASK);
                }
            }
        }
    }

    public void submitReaderTask(ReaderTask readerTask) {
        if (!readerTaskQueue.offer(readerTask)) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    public void submitWriterTask(WriterTask writerTask) {
        if(!writerTaskQueue.offer(writerTask)) {
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

    public Thread reader() {
        return reader;
    }

    public Thread writer() {
        return writer;
    }

    public void start() {
        if(state.compareAndSet(INITIAL, RUNNING)) {
            reader.start();
            writer.start();
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }
}
