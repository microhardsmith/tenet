package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Mix;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ThreadUtil;
import cn.zorcc.common.wheel.Wheel;
import lombok.extern.slf4j.Slf4j;
import org.jctools.queues.atomic.MpscUnboundedAtomicArrayQueue;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
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
    private final NetworkConfig networkConfig;
    private final MuxConfig muxConfig;
    private final int sequence;
    private final Mux mux;
    private final MemorySegment events;
    private final Map<Socket, Object> socketMap;
    private final Map<Channel, Sender> channelMap;
    private final Queue<ReaderTask> readerTaskQueue = new MpscUnboundedAtomicArrayQueue<>(Constants.QUEUE_SIZE);
    private final TransferQueue<WriterTask> writerTaskQueue = new LinkedTransferQueue<>();
    private final Thread reader;
    private final Thread writer;
    /**
     *   Representing the number of connections mounted on current worker instance
     *   This field might be optimized to normal long since it's only accessed by reader thread, AtomicLong is more secure however
     */
    private final AtomicLong counter = new AtomicLong(Constants.ZERO);

    public Worker(NetworkConfig networkConfig, MuxConfig muxConfig, int sequence) {
        this.networkConfig = networkConfig;
        this.muxConfig = muxConfig;
        this.sequence = sequence;
        this.mux = n.createMux();
        this.events = n.createEventsArray(muxConfig);
        this.socketMap = new HashMap<>(networkConfig.getMapSize());
        this.channelMap = new HashMap<>(networkConfig.getMapSize());
        this.reader = createReaderThread();
        this.writer = createWriterThread();
    }

    public Thread reader() {
        return reader;
    }

    public Thread writer() {
        return writer;
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

    public void start() {
        if(state.compareAndSet(INITIAL, RUNNING)) {
            reader.start();
            writer.start();
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    /**
     *   Create worker's reader thread
     */
    private Thread createReaderThread() {
        final long readBufferSize = networkConfig.getReadBufferSize();
        final int maxEvents = muxConfig.getMaxEvents();
        return ThreadUtil.platform("Worker-r-" + sequence, () -> {
            log.debug("Initializing Net worker's reader, sequence : {}", sequence);
            Timeout timeout = Timeout.of(muxConfig.getMuxTimeout());
            try(Arena arena = Arena.openConfined()) {
                MemorySegment[] bufArray = new MemorySegment[maxEvents];
                for (int i = 0; i < bufArray.length; i++) {
                    bufArray[i] = arena.allocateArray(ValueLayout.JAVA_BYTE, readBufferSize);
                }
                for( ; ; ) {
                    final int count = n.multiplexingWait(mux, events, maxEvents, timeout);
                    if(count == -1) {
                        log.error("Mux wait failed with errno : {}", n.errno());
                        continue;
                    }
                    if(processReaderTasks()) {
                        break ;
                    }
                    for(int index = 0; index < count; index++) {
                        MemorySegment buffer = bufArray[index];
                        n.waitForData(socketMap, buffer, events, index);
                    }
                }
            }finally {
                log.debug("Exiting Net worker, sequence : {}", sequence);
                n.exitMux(mux);
            }
        });
    }

    /**
     *   Process all reader tasks in the taskQueue, return whether current reader thread should quit
     */
    private boolean processReaderTasks() {
        for( ; ; ) {
            ReaderTask readerTask = readerTaskQueue.poll();
            if(readerTask == null) {
                return false;
            }
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
                            submitWriterTask(new WriterTask(WriterTask.WriterTaskType.EXIT, null, null));
                            return true;
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
                        submitWriterTask(new WriterTask(WriterTask.WriterTaskType.EXIT, null, null));
                        return true;
                    }
                }
            }
        }
    }

    /**
     *   Create worker's writer thread
     */
    private Thread createWriterThread() {
        return ThreadUtil.platform("Worker-w-" + sequence, () -> {
            log.debug("Initializing Net worker's writer, sequence : {}", sequence);
            final long writeBufferSize = networkConfig.getWriteBufferSize();
            try(Arena arena = Arena.openConfined()){
                MemorySegment reservedSegment = arena.allocateArray(ValueLayout.JAVA_BYTE, writeBufferSize);
                processWriterTasks(reservedSegment);
            }catch (InterruptedException i) {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED, i);
            }
        });
    }

    /**
     *   Infinite loop handling writeTasks from the queue
     */
    private void processWriterTasks(MemorySegment reservedSegment) throws InterruptedException {
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
                        sender.sendMix(channel, reservedSegment, mix);
                    }
                }
                case MSG -> {
                    Sender sender = channelMap.get(channel);
                    if(sender != null) {
                        sender.sendMsg(channel, reservedSegment, writerTask.msg());
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
    
    private final class Sender {
        /**
         *   Whether current sender has not been shutdown, normally it's null
         */
        private Shutdown shutdown;
        /**
         *   TempBuffer exist means current channel is not writable, incoming data should be written into tempBuffer first, normally it's null
         */
        private WriteBuffer tempBuffer;

        public void sendMix(Channel channel, MemorySegment reserved, Mix mix) {
            if(shutdown == null) {
                try(WriteBuffer writeBuffer = new WriteBuffer(reserved)) {
                    for (Object msg : mix.objects()) {
                        channel.encoder().encode(writeBuffer, msg);
                    }
                    if(writeBuffer.writeIndex() > 0) {
                        doSend(channel, writeBuffer);
                    }
                }
            }
        }

        public void sendMsg(Channel channel, MemorySegment reserved, Object msg) {
            if(shutdown == null) {
                try(WriteBuffer writeBuffer = new WriteBuffer(reserved)) {
                    channel.encoder().encode(writeBuffer, msg);
                    if(writeBuffer.writeIndex() > 0) {
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
                        // data has not been transferred completely, copy them to a tempBuffer
                        tempBuffer = new WriteBuffer(Arena.openConfined(), writeBuffer.size());
                        tempBuffer.write(writeBuffer.content());
                    }
                    case FAILURE -> {
                        // channel should be closed, suspend afterwards writing
                        channelMap.remove(channel);
                        submitReaderTask(new ReaderTask(ReaderTask.ReaderTaskType.CLOSE_CHANNEL, channel));
                    }
                }
            }else {
                // dump current writeBuffer to the temp
                tempBuffer.write(writeBuffer.content());
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
}
