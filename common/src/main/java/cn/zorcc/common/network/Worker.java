package cn.zorcc.common.network;

import cn.zorcc.common.Carrier;
import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.structure.IntMap;
import cn.zorcc.common.structure.IntMapNode;
import cn.zorcc.common.util.ThreadUtil;
import org.jctools.queues.atomic.MpscUnboundedAtomicArrayQueue;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.time.Duration;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicLong;
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
    private final AtomicLong counter = new AtomicLong(0);
    private final Mux mux;
    private final Queue<ReaderTask> readerTaskQueue = new MpscUnboundedAtomicArrayQueue<>(Constants.QUEUE_SIZE);
    private final TransferQueue<WriterTask> writerTaskQueue = new LinkedTransferQueue<>();
    private final Thread reader;
    private final Thread writer;

    public Worker(WorkerConfig workerConfig, int sequence) {
        this.mux = osNetworkLibrary.createMux();
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
        long readBufferSize = workerConfig.getReadBufferSize();
        int maxEvents = workerConfig.getMaxEvents();
        int muxTimeout = workerConfig.getMuxTimeout();
        return ThreadUtil.platform("Worker-r-" + sequence, () -> {
            log.debug(STR."Initializing Net worker's reader, sequence : \{sequence}");
            IntMap<Receiver> receiverMap = new IntMap<>(workerConfig.getMapSize());
            try(Arena arena = Arena.ofConfined()) {
                int state = RUNNING;
                Timeout timeout = Timeout.of(arena, muxTimeout);
                MemorySegment events = arena.allocate(MemoryLayout.sequenceLayout(maxEvents, osNetworkLibrary.eventLayout()));
                MemorySegment[] reservedArray = new MemorySegment[maxEvents];
                for (int i = 0; i < reservedArray.length; i++) {
                    reservedArray[i] = arena.allocateArray(ValueLayout.JAVA_BYTE, readBufferSize);
                }
                for( ; ; ) {
                    final int count = osNetworkLibrary.muxWait(mux, events, maxEvents, timeout);
                    if(count < 0) {
                        int errno = osNetworkLibrary.errno();
                        if(errno == osNetworkLibrary.interruptCode()) {
                            continue;
                        }else {
                            throw new FrameworkException(ExceptionType.NETWORK, "Multiplexing wait failed with errno : %d".formatted(errno));
                        }
                    }
                    state = processReaderTasks(receiverMap, state);
                    if(state == STOPPED) {
                        break ;
                    }
                    for(int index = 0; index < count; index++) {
                        MemorySegment reserved = reservedArray[index];
                        long r = osNetworkLibrary.workerWait(reserved, events, index);
                        Receiver receiver = receiverMap.get((int) r);
                        if(receiver != null) {
                            if((r & OsNetworkLibrary.W) != 0) {
                                receiver.doWrite();
                            }else if((r & OsNetworkLibrary.R) != 0) {
                                receiver.doRead(reserved);
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
    private int processReaderTasks(IntMap<Receiver> receiverMap, int currentState) {
        for( ; ; ) {
            ReaderTask readerTask = readerTaskQueue.poll();
            if(readerTask == null) {
                return currentState;
            }
            switch (readerTask.type()) {
                case ADD_CHANNEL -> {
                    if(currentState == RUNNING) {
                        Acceptor acceptor = readerTask.acceptor();
                        Receiver receiver = new Receiver(receiverMap);
                        receiver.setAcceptor(acceptor);
                        receiverMap.put(acceptor.socket().intValue(), receiver);
                        counter.getAndIncrement();
                    }
                }
                case CLOSE_ACTOR -> {
                    readerTask.actor().close();
                }
                case REGISTER_MSG -> {
                    TaggedMsg taggedMsg = readerTask.taggedMsg();
                    int tag = taggedMsg.tag();
                    channel.tagMap().put(tag, taggedMsg);
                    if(tag < 0) {
                        channel.setIdentifier(tag);
                    }
                }
                case UNREGISTER_MSG -> {
                    if(readerTask.actor() instanceof Channel channel) {
                        TaggedMsg taggedMsg = readerTask.taggedMsg();
                        int tag = taggedMsg.tag();
                        IntMapNode<TaggedMsg> node = channel.tagMap().getNode(tag);
                        if(node != null) {
                            Carrier carrier = node.getValue().carrier();
                            if(carrier.target().compareAndSet(Carrier.HOLDER, null)) {
                                channel.tagMap().removeNode(tag, node);
                                LockSupport.unpark(carrier.thread());
                            }
                        }
                    }else {
                        throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
                    }

                }
                case GRACEFUL_SHUTDOWN -> {
                    if(currentState == RUNNING) {
                        if(counter.get() == 0) {
                            submitWriterTask(new WriterTask(WriterTask.WriterTaskType.EXIT, null, null, null));
                            return STOPPED;
                        }else {
                            receiverMap.asList().forEach(receiver -> receiver.gracefulShutdown(readerTask.duration()));
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


    /**
     *   Create worker's writer thread
     */
    private Thread createWriterThread(WorkerConfig workerConfig, int sequence) {
        return ThreadUtil.platform("Worker-w-" + sequence, () -> {
            log.debug(STR."Initializing Net worker's writer, sequence : \{sequence}");
            int mapSize = workerConfig.getMapSize();
            long writeBufferSize = workerConfig.getWriteBufferSize();
            try(Arena arena = Arena.ofConfined()){
                IntMap<Sender> senderMap = new IntMap<>(mapSize);
                MemorySegment reservedSegment = arena.allocateArray(ValueLayout.JAVA_BYTE, writeBufferSize);
                processWriterTasks(senderMap, reservedSegment);
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
    private void processWriterTasks(IntMap<Sender> senderMap, MemorySegment reservedSegment) throws InterruptedException {
        for( ; ; ) {
            WriterTask writerTask = writerTaskQueue.take();
            Channel channel = writerTask.channel();
            switch (writerTask.type()) {
                case EXIT -> {
                    return ;
                }
                case INITIATE -> senderMap.put(channel.socket().intValue(), new Sender(this, senderMap));
                case MULTIPLE_MSG -> {
                    Sender sender = senderMap.get(channel.socket().intValue());
                    if(sender != null && writerTask.msg() instanceof List<?> list) {
                        sender.sendMultipleMsg(channel, reservedSegment, list, writerTask.callback());
                    }
                }
                case MSG -> {
                    Sender sender = senderMap.get(channel.socket().intValue());
                    if(sender != null) {
                        sender.sendMsg(channel, reservedSegment, writerTask.msg(), writerTask.callback());
                    }
                }
                case WRITABLE -> {
                    Sender sender = senderMap.get(channel.socket().intValue());
                    if(sender != null) {
                        sender.becomeWritable(channel);
                    }
                }
                case REMOVE -> {
                    Sender sender = senderMap.get(channel.socket().intValue());
                    if(sender != null) {
                        sender.close(channel);
                    }
                }
                case SHUTDOWN -> {
                    Sender sender = senderMap.get(channel.socket().intValue());
                    if(sender != null && writerTask.msg() instanceof Duration duration) {
                        sender.shutdown(channel, duration);
                    }
                }
                default -> throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
        }
    }
}
