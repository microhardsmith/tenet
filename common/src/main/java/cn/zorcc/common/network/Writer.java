package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.bindings.TenetBinding;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.structure.Allocator;
import cn.zorcc.common.structure.IntMap;
import cn.zorcc.common.structure.MemApi;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicInteger;

public record Writer(
        BlockingQueue<WriterTask> writerQueue,
        MemApi memApi,
        Thread writerThread
) {
    private static final Logger log = new Logger(Writer.class);
    private static final AtomicInteger counter = new AtomicInteger(0);
    private static final ScopedValue<MemApi> MEM_SCOPE = ScopedValue.newInstance();

    public static Writer newWriter(NetConfig config, MemApi memApi) {
        BlockingQueue<WriterTask> queue = new LinkedTransferQueue<>();
        Thread writerThread = createWriterThread(config, memApi, queue);
        return new Writer(queue, memApi, writerThread);
    }

    public void submit(WriterTask writerTask) {
        if(writerTask == null || !writerQueue.offer(writerTask)) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    /**
     *   Writer could provide its local allocator for Encoder usage
     */
    public static Allocator localAllocator() {
        return Allocator.newDirectAllocator(MEM_SCOPE.orElseThrow(() -> new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED)));
    }

    private static Thread createWriterThread(NetConfig config, MemApi memApi, BlockingQueue<WriterTask> queue) {
        int sequence = counter.getAndIncrement();
        return Thread.ofPlatform().name(STR."writer-\{sequence}").unstarted(() -> {
            log.info(STR."Initializing writer thread, sequence : \{sequence}");
            if(config.isEnableRpMalloc()) {
                TenetBinding.rpThreadInitialize();
            }
            ScopedValue.runWhere(MEM_SCOPE, memApi, () -> {
                try(Allocator allocator = Allocator.newDirectAllocator(memApi)) {
                    IntMap<WriterNode> nodeMap = IntMap.newTreeMap(config.getWriterMapSize());
                    MemorySegment reservedSegment = allocator.allocate(ValueLayout.JAVA_BYTE, config.getWriterBufferSize());
                    processWriterTasks(nodeMap, queue, reservedSegment);
                }catch (InterruptedException i) {
                    throw new FrameworkException(ExceptionType.NETWORK, "Writer thread interrupted", i);
                }finally {
                    log.info(STR."Exiting writer thread, sequence : \{sequence}");
                    if(config.isEnableRpMalloc()) {
                        TenetBinding.rpThreadFinalize();
                    }
                }
            });
        });
    }

    private static void processWriterTasks(IntMap<WriterNode> nodeMap, BlockingQueue<WriterTask> queue, MemorySegment reserved) throws InterruptedException {
        int state = Constants.RUNNING;
        for( ; ; ) {
            WriterTask writerTask = queue.take();
            switch (writerTask.type()) {
                case INITIATE -> handleInitiateMsg(nodeMap, writerTask);
                case SINGLE_MSG -> handleSingleMsg(nodeMap, writerTask, reserved);
                case MULTIPLE_MSG -> handleMultipleMsg(nodeMap, writerTask, reserved);
                case WRITABLE -> handleWritable(nodeMap, writerTask);
                case SHUTDOWN -> handleShutdown(nodeMap, writerTask);
                case CLOSE -> handleClose(nodeMap, writerTask);
                case EXIT -> {
                    if(state == Constants.RUNNING) {
                        if(nodeMap.isEmpty()) {
                            return ;
                        }else {
                            state = Constants.CLOSING;
                        }
                    }
                }
                case POTENTIAL_EXIT -> {
                    if(state == Constants.CLOSING && nodeMap.isEmpty()) {
                        return ;
                    }
                }
                default -> throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
        }
    }

    private static void handleInitiateMsg(IntMap<WriterNode> nodeMap, WriterTask writerTask) {
        Object msg = writerTask.msg();
        if(msg instanceof Protocol protocol) {
            Channel channel = writerTask.channel();
            WriterNode writerNode = new WriterNode.ProtocolWriterNode(nodeMap, channel, protocol);
            nodeMap.put(channel.socket().intValue(), writerNode);
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    private static void handleSingleMsg(IntMap<WriterNode> nodeMap, WriterTask writerTask, MemorySegment reserved) {
        Channel channel = writerTask.channel();
        WriterNode writerNode = nodeMap.get(channel.socket().intValue());
        if(writerNode != null) {
            writerNode.onMsg(reserved, writerTask);
        }
    }

    private static void handleMultipleMsg(IntMap<WriterNode> nodeMap, WriterTask writerTask, MemorySegment reserved) {
        Channel channel = writerTask.channel();
        WriterNode writerNode = nodeMap.get(channel.socket().intValue());
        if(writerNode != null) {
            writerNode.onMultipleMsg(reserved, writerTask);
        }
    }

    private static void handleWritable(IntMap<WriterNode> nodeMap, WriterTask writerTask) {
        Channel channel = writerTask.channel();
        WriterNode writerNode = nodeMap.get(channel.socket().intValue());
        if(writerNode != null) {
            writerNode.onWritable(writerTask);
        }
    }

    private static void handleShutdown(IntMap<WriterNode> nodeMap, WriterTask writerTask) {
        Channel channel = writerTask.channel();
        WriterNode writerNode = nodeMap.get(channel.socket().intValue());
        if(writerNode != null) {
            writerNode.onShutdown(writerTask);
        }
    }

    private static void handleClose(IntMap<WriterNode> nodeMap, WriterTask writerTask) {
        Channel channel = writerTask.channel();
        WriterNode writerNode = nodeMap.get(channel.socket().intValue());
        if(writerNode != null) {
            writerNode.onClose(writerTask);
        }
    }

}
