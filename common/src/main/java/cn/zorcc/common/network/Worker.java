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
     *   Representing the number of connections mounted on current worker instance
     *   This field might be optimized to normal long since it's only accessed by reader thread, AtomicLong is more secure however
     */
    private final AtomicLong counter = new AtomicLong(Constants.ZERO);

    public Worker(MuxConfig muxConfig, int sequence) {
        this.mux = n.createMux();
        this.events = n.createEventsArray(muxConfig);
        this.reader = createReaderThread(sequence, muxConfig);
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

        public void sendMix(Channel channel, Mix mix) {
            if(shutdown == null) {
                try(WriteBuffer writeBuffer = new WriteBuffer(Net.WRITE_BUFFER_SIZE)) {
                    for (Object msg : mix.objects()) {
                        channel.encoder().encode(writeBuffer, msg);
                    }
                    if(writeBuffer.notEmpty()) {
                        doSend(channel, writeBuffer);
                    }
                }
            }
        }

        public void sendMsg(Channel channel, Object msg) {
            if(shutdown == null) {
                try(WriteBuffer writeBuffer = new WriteBuffer(Net.WRITE_BUFFER_SIZE)) {
                    channel.encoder().encode(writeBuffer, msg);
                    if(writeBuffer.notEmpty()) {
                        doSend(channel, writeBuffer);
                    }
                }
            }
        }

        public void becomeWritable(Channel channel) {
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
                        submitReaderTask(new ReaderTask(ReaderTask.ReaderTaskType.CLOSE_CHANNEL, channel));
                    }
                }
            }
        }

        public void shutdown(Channel channel, Shutdown shutdown) {
            if(tempBuffer == null) {
                // current channel could be safely shutdown
                doShutdown(channel, shutdown);
            }else {
                // still have some underlying data to be transferred
                this.shutdown = shutdown;
            }
        }

        public void close(Channel channel) {
            channelMap.remove(channel);
            if(tempBuffer != null) {
                tempBuffer.close();
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
                        submitReaderTask(new ReaderTask(ReaderTask.ReaderTaskType.CLOSE_CHANNEL, channel));
                    }
                }
            }else {
                // dump current writeBuffer to the temp
                tempBuffer.write(writeBuffer.segment());
            }
        }

        /**
         *   Perform the actual shutdown operation, no longer receiving write tasks
         */
        private void doShutdown(Channel channel, Shutdown s) {
            channelMap.remove(channel);
            channel.protocol().doShutdown(channel);
            Wheel.wheel().addJob(() -> submitReaderTask(new ReaderTask(ReaderTask.ReaderTaskType.CLOSE_CHANNEL, channel)), s.timeout(), s.timeUnit());
        }
    }

    /**
     *   Create worker's reader thread
     */
    private Thread createReaderThread(int sequence, MuxConfig muxConfig) {
        final int maxEvents = muxConfig.getMaxEvents();
        return ThreadUtil.platform("Worker-r-" + sequence, () -> {
            log.debug("Initializing Net worker's reader, sequence : {}", sequence);
            Timeout timeout = Timeout.of(muxConfig.getMuxTimeout());
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
                loop();
            }catch (InterruptedException i) {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED, i);
            }
        });
    }

    /**
     *   Infinite loop handling writeTasks from the queue
     */
    private void loop() throws InterruptedException {
        for( ; ; ) {
            WriterTask writerTask = writerTaskQueue.take();
            Channel channel = writerTask.channel();
            switch (writerTask.type()) {
                case EXIT -> {
                    return ;
                }
                case INITIATE -> channelMap.put(channel, new Sender());
                case MIX_OF_MSG -> {
                    Sender sender = channelMap.get(channel);
                    if(sender != null && writerTask.msg() instanceof Mix mix) {
                        sender.sendMix(channel, mix);
                    }
                }
                case MSG -> {
                    Sender sender = channelMap.get(channel);
                    if(sender != null) {
                        sender.sendMsg(channel, writerTask.msg());
                    }
                }
                case WRITABLE -> {
                    Sender sender = channelMap.get(channel);
                    if(sender != null) {
                        sender.becomeWritable(channel);
                    }
                }
                case CLOSE -> {
                    Sender sender = channelMap.get(channel);
                    if(sender != null) {
                        sender.close(channel);
                    }
                }
                case SHUTDOWN -> {
                    Sender sender = channelMap.get(channel);
                    if(sender != null && writerTask.msg() instanceof Shutdown shutdown) {
                        sender.shutdown(channel, shutdown);
                    }
                }
                default -> throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
        }
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
                if(readerTask.target() instanceof Acceptor acceptor) {
                    socketMap.put(acceptor.socket(), acceptor);
                    counter.getAndIncrement();
                }else {
                    throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
                }
            }
            case CLOSE_ACCEPTOR -> {
                if(readerTask.target() instanceof Acceptor acceptor) {
                    acceptor.close();
                }else {
                    throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
                }
            }
            case CLOSE_CHANNEL -> {
                if(readerTask.target() instanceof Channel channel) {
                    channel.close();
                }else {
                    throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
                }
            }
            case GRACEFUL_SHUTDOWN -> {
                if(state.compareAndSet(RUNNING, CLOSING) && readerTask.target() instanceof Shutdown shutdown) {
                    if(counter.get() == 0L) {
                        // current worker has no channel bound, could be directly closed
                        state.set(STOPPED);
                        reader.interrupt();
                        submitWriterTask(new WriterTask(WriterTask.WriterTaskType.EXIT, null, null));
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
                    throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
                }
            }
            case POSSIBLE_SHUTDOWN -> {
                if(state.compareAndSet(CLOSING, STOPPED)) {
                    reader.interrupt();
                    submitWriterTask(new WriterTask(WriterTask.WriterTaskType.EXIT, null, null));
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
