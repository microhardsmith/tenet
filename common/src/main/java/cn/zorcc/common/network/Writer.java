package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.structure.IntMap;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicInteger;

public final class Writer {
    private static final Logger log = new Logger(Writer.class);
    private static final AtomicInteger counter = new AtomicInteger(0);
    private final BlockingQueue<WriterTask> queue = new LinkedTransferQueue<>();
    private final Thread writerThread;
    public Writer(WriterConfig writerConfig) {
        this.writerThread = createWriterThread(writerConfig);
    }

    public void submit(WriterTask writerTask) {
        if(writerTask == null || !queue.offer(writerTask)) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    public Thread thread() {
        return writerThread;
    }

    private Thread createWriterThread(WriterConfig writerConfig) {
        int sequence = counter.getAndIncrement();
        return Thread.ofPlatform().name(STR."writer-\{sequence}").unstarted(() -> {
            log.info(STR."Initializing writer thread, sequence : \{sequence}");
            try(Arena arena = Arena.ofConfined()){
                IntMap<WriterNode> nodeMap = new IntMap<>(writerConfig.getMapSize());
                MemorySegment reservedSegment = arena.allocateArray(ValueLayout.JAVA_BYTE, writerConfig.getWriteBufferSize());
                processWriterTasks(nodeMap, reservedSegment);
            }catch (InterruptedException i) {
                throw new FrameworkException(ExceptionType.NETWORK, "Writer thread interrupted", i);
            }finally {
                log.info(STR."Exiting writer thread, sequence : \{sequence}");
            }
        });
    }

    private void processWriterTasks(IntMap<WriterNode> nodeMap, MemorySegment reserved) throws InterruptedException {
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
        if(msg instanceof ProtoAndState protoAndState) {
            Channel channel = writerTask.channel();
            WriterNode writerNode = new ProtocolWriterNode(nodeMap, channel, protoAndState.protocol(), protoAndState.state());
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

    private void handleShutdown(IntMap<WriterNode> nodeMap, WriterTask writerTask) {
        Channel channel = writerTask.channel();
        WriterNode writerNode = nodeMap.get(channel.socket().intValue());
        if(writerNode != null) {
            writerNode.onShutdown(writerTask);
        }
    }

    private void handleClose(IntMap<WriterNode> nodeMap, WriterTask writerTask) {
        Channel channel = writerTask.channel();
        WriterNode writerNode = nodeMap.get(channel.socket().intValue());
        if(writerNode != null) {
            writerNode.onClose(writerTask);
        }
    }

}
