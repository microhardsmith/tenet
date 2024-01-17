package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.State;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.network.api.Protocol;
import cn.zorcc.common.network.lib.OsNetworkLibrary;
import cn.zorcc.common.structure.*;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

/**
 *   Currently, writerNode has only one implementation, so it's designed as a utility class rather than an interface
 */
public final class ProtocolWriterNode implements WriterNode {
    private static final Logger log = new Logger(ProtocolWriterNode.class);
    private static final OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;
    private record Task(
            MemorySegment memorySegment,
            WriterCallback writerCallback
    ) {

    }

    private final IntMap<WriterNode> nodeMap;
    private final Channel channel;
    private final Protocol protocol;
    private final State channelState;
    /**
     *   Tasks exist means current channel is not writable, incoming data should be wrapped into tasks list first, normally it's null
     */
    private Deque<Task> taskQueue;
    /**
     *   Whether the sender has been shutdown, normally it's null, could exist when tasks is not null
     */
    private Duration timeout;

    public ProtocolWriterNode(IntMap<WriterNode> nodeMap, Channel channel, Protocol protocol, State channelState) {
        this.nodeMap = nodeMap;
        this.channel = channel;
        this.protocol = protocol;
        this.channelState = channelState;
    }

    @Override
    public void onMsg(MemorySegment reserved, WriterTask writerTask) {
        if(writerTask.channel() == channel) {
            Object msg = writerTask.msg();
            WriterCallback writerCallback = writerTask.writerCallback();
            try(WriteBuffer writeBuffer = newWriteBuffer(reserved)) {
                try{
                    channel.encoder().encode(writeBuffer, msg);
                }catch (RuntimeException e) {
                    log.error("Err occurred in encoder", e);
                    close();
                    return ;
                }
                if(writeBuffer.writeIndex() > 0L) {
                    sendMsg(writeBuffer, writerCallback);
                }else if(writerCallback != null) {
                    writerCallback.invokeOnSuccess(channel);
                }
            }
        }
    }

    @Override
    public void onMultipleMsg(MemorySegment reserved, WriterTask writerTask) {
        if(writerTask.channel() == channel && writerTask.msg() instanceof Collection<?> msgs) {
            WriterCallback writerCallback = writerTask.writerCallback();
            try(WriteBuffer writeBuffer = newWriteBuffer(reserved)) {
                try{
                    for (Object msg : msgs) {
                        channel.encoder().encode(writeBuffer, msg);
                    }
                }catch (RuntimeException e) {
                    log.error("Err occurred in encoder", e);
                    close();
                    return ;
                }
                if(writeBuffer.writeIndex() > 0L) {
                    sendMsg(writeBuffer, writerCallback);
                }else if(writerCallback != null) {
                    writerCallback.invokeOnSuccess(channel);
                }
            }
        }
    }

    @Override
    public void onWritable(WriterTask writerTask) {
        if(writerTask.channel() == channel) {
            for( ; ; ) {
                Task task = taskQueue.pollFirst();
                if(task == null) {
                    taskQueue = null;
                    if(timeout != null) {
                        shutdown(timeout);
                    }
                    return ;
                }
                MemorySegment data = task.memorySegment();
                WriterCallback writerCallback = task.writerCallback();
                long len = data.byteSize();
                long r;
                for( ; ; ) {
                    try{
                        r = protocol.doWrite(data, len);
                        if(r > 0L && r < len) {
                            len = len - r;
                            data = data.asSlice(r, len);
                        }else {
                            break;
                        }
                    }catch (RuntimeException e) {
                        log.error("Failed to perform doWrite()", e);
                        close();
                        return ;
                    }
                }
                if(r == len) {
                    if(writerCallback != null) {
                        writerCallback.invokeOnSuccess(channel);
                    }
                }else {
                    taskQueue.addFirst(new Task(data, writerCallback));
                    if(r < 0L) {
                        handleEvent(r);
                    }
                }
            }
        }
    }

    @Override
    public void onShutdown(WriterTask writerTask) {
        if(writerTask.channel() == channel && writerTask.msg() instanceof Duration duration) {
            conditionalShutdown(duration);
        }
    }

    @Override
    public void onClose(WriterTask writerTask) {
        if(writerTask.channel() == channel) {
            close();
        }
    }

    @Override
    public void exit(Duration duration) {
        conditionalShutdown(duration);
    }

    /**
     *   If current channel is not writable, then the message would be written into a heap buffer for more efficiency
     */
    private WriteBuffer newWriteBuffer(MemorySegment reserved) {
        if(taskQueue == null) {
            return WriteBuffer.newReservedWriteBuffer(reserved, true);
        }else {
            return WriteBuffer.newHeapWriteBuffer(reserved.byteSize());
        }
    }

    /**
     *   Send msg over the channel, invoking its callback if successful, otherwise copy the data locally for channel to become writable
     */
    private void sendMsg(WriteBuffer writeBuffer, WriterCallback writerCallback) {
        MemorySegment data = writeBuffer.toSegment();
        if(taskQueue == null) {
            long len = data.byteSize();
            long r;
            for( ; ; ) {
                try{
                    r = protocol.doWrite(data, len);
                    if(r > 0 && r < len) {
                        len = len - r;
                        data = data.asSlice(r, len);
                    }else {
                        break;
                    }
                }catch (RuntimeException e) {
                    log.error("Failed to perform doWrite()", e);
                    close();
                    return ;
                }
            }
            if(r == len) {
                if(writerCallback != null) {
                    writerCallback.invokeOnSuccess(channel);
                }
            }else {
                taskQueue = new ArrayDeque<>();
                copyLocally(data, writerCallback);
                if(r < 0L) {
                    handleEvent(r);
                }
            }
        }else {
            copyLocally(data, writerCallback);
        }
    }

    private void copyLocally(MemorySegment segment, WriterCallback writerCallback) {
        long size = segment.byteSize();
        MemorySegment memorySegment = Allocator.HEAP.allocate(ValueLayout.JAVA_BYTE, size);
        MemorySegment.copy(segment, 0, memorySegment, 0, size);
        taskQueue.addLast(new Task(memorySegment, writerCallback));
    }

    private void handleEvent(long r) {
        if(r == Constants.NET_W || r == Constants.NET_R || r == Constants.NET_RW) {
            ctl(r);
        }else if(r != Constants.NET_IGNORED) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    private void ctl(long expected) {
        try(Mutex _ = channelState.withMutex()) {
            long state = channelState.get();
            if((state & Constants.NET_PC) == Constants.NET_PC) {
                close();
            }else {
                long current = state & Constants.NET_RW;
                long to = current | expected;
                if(to != current) {
                    osNetworkLibrary.ctlMux(channel.poller().mux(), channel.socket(), current, to);
                    channelState.set(state + (to - current));
                }
            }
        }
    }

    private void conditionalShutdown(Duration duration) {
        if(taskQueue == null) {
            shutdown(duration);
        }else {
            timeout = duration;
            Wheel.wheel().addJob(() -> channel.writer().submit(new WriterTask(WriterTaskType.CLOSE, channel, null, null)), duration);
        }
    }

    private void shutdown(Duration duration) {
        if(nodeMap.remove(channel.socket().intValue(), this)) {
            assert taskQueue == null;
            try(Mutex _ = channelState.withMutex()) {
                long current = channelState.get();
                channelState.set(current | Constants.NET_WC);
                if((current & Constants.NET_PC) > 0) {
                    closeProtocol();
                }else {
                    shutdownProtocol(duration);
                }
            }
            if(nodeMap.isEmpty()) {
                channel.writer().submit(new WriterTask(WriterTaskType.POTENTIAL_EXIT, null, null, null));
            }
        }
    }

    private void shutdownProtocol(Duration duration) {
        try{
            protocol.doShutdown();
            Wheel.wheel().addJob(() -> channel.poller().submit(new PollerTask(PollerTaskType.CLOSE, channel, null)), duration);
        }catch (RuntimeException e) {
            log.error("Failed to shutdown Protocol", e);
            channel.poller().submit(new PollerTask(PollerTaskType.CLOSE, channel, null));
        }
    }

    private void close() {
        if (nodeMap.remove(channel.socket().intValue(), this)) {
            if(taskQueue != null) {
                taskQueue.forEach(task -> {
                    WriterCallback writerCallback = task.writerCallback();
                    if(writerCallback != null) {
                        writerCallback.invokeOnFailure(channel);
                    }
                });
                taskQueue = null;
            }
            try (Mutex _ = channelState.withMutex()) {
                long current = channelState.get();
                channelState.set(current | Constants.NET_WC);
                if((current & Constants.NET_PC) == Constants.NET_PC) {
                    closeProtocol();
                }else {
                    channel.poller().submit(new PollerTask(PollerTaskType.CLOSE, channel, null));
                }
            }
            if(nodeMap.isEmpty()) {
                channel.writer().submit(new WriterTask(WriterTaskType.POTENTIAL_EXIT, null, null, null));
            }
        }
    }

    private void closeProtocol() {
        try{
            protocol.doClose();
        }catch (RuntimeException e) {
            log.error("Failed to close protocol from writer", e);
        }
    }
}
