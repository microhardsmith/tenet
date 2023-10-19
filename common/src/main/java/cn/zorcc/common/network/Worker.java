package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.structure.IntIntMap;
import cn.zorcc.common.structure.IntMap;
import cn.zorcc.common.structure.Wheel;
import cn.zorcc.common.util.ThreadUtil;
import org.jctools.queues.atomic.MpscUnboundedAtomicArrayQueue;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
 *   Network worker
 */
public final class Worker {
    private static final Logger log = new Logger(Worker.class);
    private static final OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;
    private static final int RUNNING = 1;
    private static final int CLOSING = 2;
    private static final int STOPPED = 3;
    private final AtomicLong counter = new AtomicLong(Constants.ZERO);
    private final Mux mux;
    private final IntMap<Actor> actorMap;
    private final IntIntMap<TaggedMsg> taggedMsgMap;
    private final Map<Channel, Sender> channelMap;
    private final Queue<ReaderTask> readerTaskQueue = new MpscUnboundedAtomicArrayQueue<>(Constants.QUEUE_SIZE);
    private final TransferQueue<WriterTask> writerTaskQueue = new LinkedTransferQueue<>();
    private final Thread reader;
    private final Thread writer;

    public Worker(WorkerConfig workerConfig, int sequence) {
        this.mux = osNetworkLibrary.createMux();
        final int mapSize = workerConfig.getMapSize();
        this.actorMap = new IntMap<>(mapSize);
        this.taggedMsgMap = new IntIntMap<>(mapSize);
        this.channelMap = new HashMap<>(mapSize);
        this.reader = createReaderThread(mux, workerConfig, sequence);
        this.writer = createWriterThread(workerConfig, sequence);
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

    private void register(Socket socket, Actor actor) {
        actorMap.put(socket.intValue(), actor);
    }

    public void replace(Socket socket, Actor actor) {
        actorMap.replace(socket.intValue(), actor);
    }

    public boolean unregister(Socket socket, Actor actor) {
        return actorMap.remove(socket.intValue(), actor);
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
     *   The reusableSocket mechanism was only provided to reduce the allocation pressure, so we don't have to allocate a Socket object each round
     */
    private Thread createReaderThread(Mux mux, WorkerConfig workerConfig, int sequence) {
        final long readBufferSize = workerConfig.getReadBufferSize();
        final int maxEvents = workerConfig.getMaxEvents();
        final int muxTimeout = workerConfig.getMuxTimeout();
        return ThreadUtil.platform("Worker-r-" + sequence, () -> {
            log.debug(STR."Initializing Net worker's reader, sequence : \{sequence}");
            try(Arena arena = Arena.ofConfined()) {
                int state = RUNNING;
                Timeout timeout = Timeout.of(arena, muxTimeout);
                MemorySegment events = arena.allocate(MemoryLayout.sequenceLayout(maxEvents, osNetworkLibrary.eventLayout()));
                MemorySegment[] bufArray = new MemorySegment[maxEvents];
                for (int i = Constants.ZERO; i < bufArray.length; i++) {
                    bufArray[i] = arena.allocateArray(ValueLayout.JAVA_BYTE, readBufferSize);
                }
                for( ; ; ) {
                    final int count = osNetworkLibrary.muxWait(mux, events, maxEvents, timeout);
                    if(count < Constants.ZERO) {
                        int errno = osNetworkLibrary.errno();
                        if(errno == osNetworkLibrary.interruptCode()) {
                            continue;
                        }else {
                            throw new FrameworkException(ExceptionType.NETWORK, "Multiplexing wait failed with errno : %d".formatted(errno));
                        }
                    }
                    state = processReaderTasks(state);
                    if(state == STOPPED) {
                        break ;
                    }
                    for(int index = Constants.ZERO; index < count; index++) {
                        MemorySegment buffer = bufArray[index];
                        long r = osNetworkLibrary.workerWait(buffer, events, index);
                        int socket = (int) r;
                        Actor actor = actorMap.get(socket);
                        if(actor != null) {
                            long event = r - socket;
                            if(event == OsNetworkLibrary.W) {
                                actor.canWrite();
                            }else if(event == OsNetworkLibrary.R) {
                                actor.canRead(buffer);
                            }else {
                                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
                            }
                        }
                    }
                }
            }finally {
                log.debug(STR."Exiting Net reader, sequence : \{sequence}");
                osNetworkLibrary.exitMux(mux);
            }
        });
    }

    /**
     *   Process all reader tasks in the taskQueue, return whether current reader thread should quit
     */
    private int processReaderTasks(int currentState) {
        for( ; ; ) {
            ReaderTask readerTask = readerTaskQueue.poll();
            if(readerTask == null) {
                return currentState;
            }
            switch (readerTask.type()) {
                case ADD_ACCEPTOR -> {
                    if(currentState == RUNNING) {
                        Acceptor acceptor = readerTask.acceptor();
                        register(acceptor.socket(), acceptor);
                        counter.getAndIncrement();
                    }
                }
                case CLOSE_ACCEPTOR -> readerTask.acceptor().close();
                case CLOSE_CHANNEL -> readerTask.channel().close();
                case ADD_TAG -> {
                    Channel channel = readerTask.channel();
                    TaggedMsg taggedMsg = readerTask.taggedMsg();
                    taggedMsgMap.put(channel.socket().intValue(), taggedMsg.getTag(), taggedMsg);
                }
                case REMOVE_TAG -> {
                    Channel channel = readerTask.channel();
                    TaggedMsg taggedMsg = readerTask.taggedMsg();
                    if (!taggedMsgMap.remove(channel.socket().intValue(), taggedMsg.getTag(), taggedMsg)) {
                        throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
                    }
                }
                case GRACEFUL_SHUTDOWN -> {
                    if(currentState == RUNNING) {
                        if(counter.get() == Constants.ZERO) {
                            submitWriterTask(new WriterTask(WriterTask.WriterTaskType.EXIT, null, null, null));
                            return STOPPED;
                        }else {
                            actorMap.toList().forEach(actor -> actor.canShutdown(readerTask.duration()));
                            return CLOSING;
                        }
                    }
                }
                case POSSIBLE_SHUTDOWN -> {
                    if(currentState == CLOSING) {
                        submitWriterTask(new WriterTask(WriterTask.WriterTaskType.EXIT, null, null, null));
                        return STOPPED;
                    }
                }
            }
        }
    }

    public void receiveTaggedMsg(Channel channel, int tag, Object received) {
        if(Thread.currentThread() != reader) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
        int fd = channel.socket().intValue();
        TaggedMsg taggedMsg = taggedMsgMap.get(fd, tag);
        if(taggedMsg != null) {
            AtomicReference<Object> target = taggedMsg.getTarget();
            if(target != null && target.compareAndSet(TaggedMsg.HOLDER, received)) {
                LockSupport.unpark(taggedMsg.getThread());
                if(!taggedMsgMap.remove(fd, tag, taggedMsg)) {
                    throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
                }
            }
        }
    }

    /**
     *   Create worker's writer thread
     */
    private Thread createWriterThread(WorkerConfig workerConfig, int sequence) {
        return ThreadUtil.platform("Worker-w-" + sequence, () -> {
            log.debug(STR."Initializing Net worker's writer, sequence : \{sequence}");
            final long writeBufferSize = workerConfig.getWriteBufferSize();
            try(Arena arena = Arena.ofConfined()){
                MemorySegment reservedSegment = arena.allocateArray(ValueLayout.JAVA_BYTE, writeBufferSize);
                processWriterTasks(reservedSegment);
            }catch (InterruptedException i) {
                throw new FrameworkException(ExceptionType.NETWORK, "Writer thread interrupted", i);
            }finally {
                log.debug(STR."Exiting Net writer, sequence : \{sequence}");
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
                case MULTIPLE_MSG -> {
                    Sender sender = channelMap.get(channel);
                    if(sender != null && writerTask.msg() instanceof List<?> list) {
                        sender.sendMultipleMsg(channel, reservedSegment, list, writerTask.callback());
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
                    if(sender != null && writerTask.msg() instanceof Duration duration) {
                        sender.shutdown(channel, duration);
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

    }
    
    private final class Sender {
        /**
         *   Whether the sender has been shutdown, normally it's null, could exists when tempBuffer is not null
         */
        private Duration duration;
        /**
         *   Tasks exist means current channel is not writable, incoming data should be wrapped into tasks list first, normally it's null
         */
        private List<SenderTask> tasks;

        public void sendMultipleMsg(Channel channel, MemorySegment reserved, List<?> msgList, WriterCallback callback) {
            try(final WriteBuffer writeBuffer = WriteBuffer.newReservedWriteBuffer(reserved)) {
                WriteBuffer wb = writeBuffer;
                for (Object msg : msgList) {
                    wb = channel.encoder().encode(wb, msg);
                }
                if(wb.writeIndex() > Constants.ZERO) {
                    doSend(channel, wb, callback);
                }
                if(wb != writeBuffer) {
                    wb.close();
                }
            }catch (FrameworkException e) {
                log.error(STR."Failed to perform writing from channel : \{channel.loc()}", e);
                channel.shutdown();
            }
        }

        public void sendMsg(Channel channel, MemorySegment reserved, Object msg, WriterCallback callback) {
            try(final WriteBuffer writeBuffer = WriteBuffer.newReservedWriteBuffer(reserved)) {
                WriteBuffer wb = channel.encoder().encode(writeBuffer, msg);
                if(wb.writeIndex() > Constants.ZERO) {
                    doSend(channel, wb, callback);
                }
                if(wb != writeBuffer) {
                    wb.close();
                }
            } catch (FrameworkException e) {
                log.error(STR."Failed to perform writing from channel : \{channel.loc()}", e);
                channel.shutdown();
            }
        }

        public void becomeWritable(Channel channel) {
            if(tasks != null) {
                for (int i = Constants.ZERO; i < tasks.size(); i++) {
                    SenderTask st = tasks.get(i);
                    switch (channel.protocol().doWrite(channel, st.writeBuffer())) {
                        case SUCCESS -> {
                            // release local memory, invoking callback
                            st.writeBuffer().close();
                            Optional.ofNullable(st.callback()).ifPresent(WriterCallback::onSuccess);
                        }
                        case PENDING -> {
                            // writeBuffer has been truncated internally
                            return ;
                        }
                        case FAILURE -> {
                            // channel should be closed, suspend afterwards writing
                            channelMap.remove(channel);
                            submitReaderTask(ReaderTask.createCloseChannelTask(channel));
                            for(int j = i; j < tasks.size(); j++) {
                                SenderTask followingSt = tasks.get(j);
                                followingSt.writeBuffer().close();
                                Optional.ofNullable(followingSt.callback()).ifPresent(WriterCallback::onFailure);
                            }
                            return ;
                        }
                    }
                }
                tasks = null;
                if(duration != null) {
                    // pending buffer has been flushed, do shutdown right now
                    doShutdown(channel, duration);
                }
            }
        }

        public void shutdown(Channel channel, Duration d) {
            if(tasks == null) {
                // current channel could be safely shutdown
                doShutdown(channel, d);
            }else if(duration == null){
                // still have some underlying data to be transferred
                this.duration = d;
            }
        }

        public void close(Channel channel) {
            channelMap.remove(channel);
            if(tasks != null) {
                for (SenderTask st : tasks) {
                    st.writeBuffer().close();
                    Optional.ofNullable(st.callback()).ifPresent(WriterCallback::onFailure);
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
                        submitReaderTask(ReaderTask.createCloseChannelTask(channel));
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
            WriteBuffer wb = WriteBuffer.newFixedWriteBuffer(arena, writeBuffer.size());
            wb.writeSegment(writeBuffer.toSegment());
            tasks.add(new SenderTask(wb, callback));
        }

        /**
         *   Perform the actual shutdown operation, no longer receiving write tasks
         */
        private void doShutdown(Channel channel, Duration duration) {
            if (!channelMap.remove(channel, this)) {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
            channel.protocol().doShutdown(channel);
            Wheel.wheel().addJob(() -> submitReaderTask(ReaderTask.createCloseChannelTask(channel)), duration);
        }
    }
}
