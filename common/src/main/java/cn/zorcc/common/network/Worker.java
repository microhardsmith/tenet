package cn.zorcc.common.network;

import cn.zorcc.common.*;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ThreadUtil;
import cn.zorcc.common.wheel.Wheel;
import org.jctools.queues.atomic.MpscUnboundedAtomicArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.*;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 *   Network worker
 */
public final class Worker {
    private static final Logger log = LoggerFactory.getLogger(Worker.class);
    private final Native n = Native.n;
    /**
     *   INITIAL -> RUNNING -> CLOSING -> STOPPED
     */
    private static final int INITIAL = 0;
    private static final int RUNNING = 1;
    private static final int CLOSING = 2;
    private static final int STOPPED = 3;
    private final AtomicInteger state = new AtomicInteger(INITIAL);
    /**
     *   Representing the number of connections mounted on current worker instance
     *   This field might be optimized to normal long since it's only accessed by reader thread, AtomicLong is more secure however
     */
    private final AtomicLong counter = new AtomicLong(Constants.ZERO);
    private final NetworkConfig networkConfig;
    private final MuxConfig muxConfig;
    private final int sequence;
    private final Mux mux;
    private final Map<Socket, Object> socketMap;
    private final Map<Channel, Sender> channelMap;
    private final Queue<ReaderTask> readerTaskQueue = new MpscUnboundedAtomicArrayQueue<>(Constants.QUEUE_SIZE);
    private final TransferQueue<WriterTask> writerTaskQueue = new LinkedTransferQueue<>();
    private final Thread reader;
    private final Thread writer;

    public Worker(NetworkConfig networkConfig, MuxConfig muxConfig, int sequence) {
        this.networkConfig = networkConfig;
        this.muxConfig = muxConfig;
        this.sequence = sequence;
        this.mux = n.createMux();
        final int mapSize = networkConfig.getMapSize();
        this.socketMap = new HashMap<>(mapSize);
        this.channelMap = new HashMap<>(mapSize);
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

    public AtomicInteger state() {
        return state;
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

    /**
     *   Create worker's reader thread
     */
    private Thread createReaderThread() {
        final long readBufferSize = networkConfig.getReadBufferSize();
        final int maxEvents = muxConfig.getMaxEvents();
        return ThreadUtil.platform("Worker-r-" + sequence, () -> {
            log.debug("Initializing Net worker's reader, sequence : {}", sequence);
            try(Arena arena = Arena.ofConfined()) {
                Timeout timeout = Timeout.of(arena, muxConfig.getMuxTimeout());
                MemorySegment events = n.createEventsArray(muxConfig, arena);
                MemorySegment[] bufArray = new MemorySegment[maxEvents];
                for (int i = 0; i < bufArray.length; i++) {
                    bufArray[i] = arena.allocateArray(ValueLayout.JAVA_BYTE, readBufferSize);
                }
                for( ; ; ) {
                    final int count = n.multiplexingWait(mux, events, maxEvents, timeout);
                    if(count < Constants.ZERO) {
                        int errno = n.errno();
                        if(errno == n.interruptCode()) {
                            continue;
                        }else {
                            throw new FrameworkException(ExceptionType.NETWORK, "Multiplexing wait failed with errno : %d".formatted(errno));
                        }
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
                log.debug("Exiting Net reader, sequence : {}", sequence);
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
                    Acceptor acceptor = readerTask.acceptor();
                    socketMap.put(acceptor.socket(), acceptor);
                    counter.getAndIncrement();
                }
                case CLOSE_ACCEPTOR -> readerTask.acceptor().close();
                case CLOSE_CHANNEL -> readerTask.channel().close();
                case GRACEFUL_SHUTDOWN -> {
                    if(state.compareAndSet(RUNNING, CLOSING)) {
                        if(counter.get() == Constants.ZERO) {
                            state.set(STOPPED);
                            submitWriterTask(new WriterTask(WriterTask.WriterTaskType.EXIT, null, null, null));
                            return true;
                        }else {
                            log.debug("Net worker awaiting for termination,  sequence : {}, socket count : {}", sequence, socketMap.size());
                            for (Object obj : socketMap.values()) {
                                switch (obj) {
                                    case Acceptor acceptor -> acceptor.close();
                                    case Channel channel -> channel.shutdown(readerTask.shutdown());
                                    default -> throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
                                }
                            }
                        }
                    }else {
                        throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
                    }
                }
                case POSSIBLE_SHUTDOWN -> {
                    if(state.compareAndSet(CLOSING, STOPPED)) {
                        submitWriterTask(new WriterTask(WriterTask.WriterTaskType.EXIT, null, null, null));
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
            try(Arena arena = Arena.ofConfined()){
                MemorySegment reservedSegment = arena.allocateArray(ValueLayout.JAVA_BYTE, writeBufferSize);
                processWriterTasks(reservedSegment);
            }catch (InterruptedException i) {
                throw new FrameworkException(ExceptionType.NETWORK, "Writer thread interrupted", i);
            }finally {
                log.debug("Exiting Net writer, sequence : {}", sequence);
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
                        sender.sendMix(channel, reservedSegment, mix, writerTask.callback());
                    }
                }
                case MSG -> {
                    Sender sender = channelMap.get(channel);
                    if(sender != null) {
                        sender.sendMsg(channel, reservedSegment, writerTask.msg(), writerTask.callback());
                    }
                }
                case WRITABLE -> {
                    Sender sender = channelMap.get(channel);
                    if(sender != null) {
                        sender.becomeWritable(channel);
                    }
                }
                case REMOVE -> {
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

    private record SenderTask(
            WriteBuffer writeBuffer,
            WriterCallback callback
    ){
        void release() {
            writeBuffer.close();
            if(callback != null) {
                callback.onSuccess();
            }
        }

        void fail() {
            writeBuffer.close();
            if(callback != null) {
                callback.onFailure();
            }
        }
    }
    
    private final class Sender {
        /**
         *   Whether the sender has been shutdown, normally it's null, could exists when tempBuffer is not null
         */
        private Shutdown shutdown;
        /**
         *   Tasks exist means current channel is not writable, incoming data should be wrapped into tasks list first, normally it's null
         */
        private List<SenderTask> tasks;

        public void sendMix(Channel channel, MemorySegment reserved, Mix mix, WriterCallback callback) {
            try(final WriteBuffer writeBuffer = new WriteBuffer(reserved, new ReservedWriteBufferPolicy())) {
                WriteBuffer wb = writeBuffer;
                for (Object msg : mix.objects()) {
                    wb = channel.encoder().encode(wb, msg);
                }
                if(wb.writeIndex() > 0) {
                    doSend(channel, wb, callback);
                }
                if(wb != writeBuffer) {
                    wb.close();
                }
            }catch (FrameworkException e) {
                log.error("Failed to perform writing from channel : {}", channel.loc(), e);
                channel.shutdown();
            }
        }

        public void sendMsg(Channel channel, MemorySegment reserved, Object msg, WriterCallback callback) {
            try(final WriteBuffer writeBuffer = new WriteBuffer(reserved, new ReservedWriteBufferPolicy())) {
                WriteBuffer wb = channel.encoder().encode(writeBuffer, msg);
                if(wb.writeIndex() > 0) {
                    doSend(channel, wb, callback);
                }
                if(wb != writeBuffer) {
                    wb.close();
                }
            } catch (FrameworkException e) {
                log.error("Failed to perform writing from channel : {}", channel.loc(), e);
                channel.shutdown();
            }
        }

        public void becomeWritable(Channel channel) {
            if(tasks != null) {
                for (int i = 0; i < tasks.size(); i++) {
                    SenderTask st = tasks.get(i);
                    switch (channel.protocol().doWrite(channel, st.writeBuffer())) {
                        case SUCCESS -> {
                            // release local memory, invoking callback
                            st.release();
                        }
                        case PENDING -> {
                            // writeBuffer has been truncated internally
                            return ;
                        }
                        case FAILURE -> {
                            // channel should be closed, suspend afterwards writing
                            channelMap.remove(channel);
                            submitReaderTask(new ReaderTask(ReaderTask.ReaderTaskType.CLOSE_CHANNEL, null, channel, null));
                            for(int j = i; j < tasks.size(); j++) {
                                tasks.get(j).fail();
                            }
                        }
                    }
                }
                tasks = null;
                if(shutdown != null) {
                    // pending buffer has been flushed, do shutdown right now
                    doShutdown(channel, shutdown);
                }
            }
        }

        public void shutdown(Channel channel, Shutdown s) {
            if(tasks == null) {
                // current channel could be safely shutdown
                doShutdown(channel, s);
            }else if(shutdown == null){
                // still have some underlying data to be transferred
                this.shutdown = s;
            }
        }

        public void close(Channel channel) {
            channelMap.remove(channel);
            if(tasks != null) {
                for (SenderTask task : tasks) {
                    task.fail();
                }
            }
        }

        /**
         *   Try send writeBuffer, will write into tempBuffer if current channel is not writable
         *   Note that since the whole operation is single-threaded, shutdown will only exists when tempBuffer is not null, so here we only need to examine the tempBuffer
         */
        private void doSend(Channel channel, WriteBuffer writeBuffer, WriterCallback callback) {
            if(tasks == null) {
                switch (channel.protocol().doWrite(channel, writeBuffer)) {
                    case SUCCESS -> {
                        // invoking successful callback operation
                        if(callback != null) {
                            callback.onSuccess();
                        }
                    }
                    case PENDING -> {
                        // data has not been transferred completely
                        tasks = new ArrayList<>();
                        copyLocally(writeBuffer, callback);
                    }
                    case FAILURE -> {
                        // channel should be closed, suspend afterwards writing, fail the callback
                        channelMap.remove(channel);
                        submitReaderTask(new ReaderTask(ReaderTask.ReaderTaskType.CLOSE_CHANNEL, null, channel, null));
                        if(callback != null) {
                            callback.onFailure();
                        }
                    }
                }
            }else {
                copyLocally(writeBuffer, callback);
            }
        }

        /**
         *   Copy target writeBuffer with its callback locally
         */
        private void copyLocally(WriteBuffer writeBuffer, WriterCallback callback) {
            Arena arena = Arena.ofConfined();
            WriteBuffer wb = new WriteBuffer(arena.allocateArray(ValueLayout.JAVA_BYTE, writeBuffer.size()), new IgnoreWriteBufferPolicy(arena));
            wb.write(writeBuffer.content());
            tasks.add(new SenderTask(wb, callback));
        }

        /**
         *   Perform the actual shutdown operation, no longer receiving write tasks
         */
        private void doShutdown(Channel channel, Shutdown s) {
            if (!channelMap.remove(channel, this)) {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
            channel.protocol().doShutdown(channel);
            Wheel.wheel().addJob(() -> submitReaderTask(new ReaderTask(ReaderTask.ReaderTaskType.CLOSE_CHANNEL, null, channel, null)), s.timeout(), s.timeUnit());
        }
    }
}
