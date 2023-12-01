package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.State;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.network.api.Callback;
import cn.zorcc.common.network.api.Protocol;
import cn.zorcc.common.network.lib.OsNetworkLibrary;
import cn.zorcc.common.structure.IntMap;
import cn.zorcc.common.structure.Mutex;
import cn.zorcc.common.structure.Wheel;

import java.lang.foreign.Arena;
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
            Arena arena,
            MemorySegment memorySegment,
            Callback callback
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
            Callback callback = writerTask.callback();
            try(final WriteBuffer writeBuffer = WriteBuffer.newReservedWriteBuffer(reserved)) {
                try{
                    channel.encoder().encode(writeBuffer, msg);
                }catch (RuntimeException e) {
                    log.error("Err occurred in encoder", e);
                    close();
                    return ;
                }
                if(writeBuffer.writeIndex() > 0) {
                    sendMsg(writeBuffer, callback);
                }else if(callback != null) {
                    callback.invokeOnSuccess(channel);
                }
            }
        }
    }

    @Override
    public void onMultipleMsg(MemorySegment reserved, WriterTask writerTask) {
        if(writerTask.channel() == channel && writerTask.msg() instanceof Collection<?> msgs) {
            Callback callback = writerTask.callback();
            try(final WriteBuffer writeBuffer = WriteBuffer.newReservedWriteBuffer(reserved)) {
                try{
                    for (Object msg : msgs) {
                        channel.encoder().encode(writeBuffer, msg);
                    }
                }catch (RuntimeException e) {
                    log.error("Err occurred in encoder", e);
                    close();
                    return ;
                }
                if(writeBuffer.writeIndex() > 0) {
                    sendMsg(writeBuffer, callback);
                }else if(callback != null) {
                    callback.invokeOnSuccess(channel);
                }
            } catch (FrameworkException e) {
                log.error("WriterNode error while sending multiple msg", e);
                close();
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
                Callback callback = task.callback();
                int len = (int) data.byteSize();
                int r;
                try{
                    r = protocol.doWrite(data, len);
                }catch (RuntimeException e) {
                    log.error("Failed to perform doWrite()", e);
                    close();
                    return ;
                }
                if(r >= 0) {
                    taskQueue.addFirst(task);
                    if(r > 0) {
                        handleEvent(r);
                    }
                }else {
                    int written = -r;
                    if(written == len) {
                        if(callback != null) {
                            callback.invokeOnSuccess(channel);
                        }
                    }else {
                        taskQueue.addFirst(new Task(task.arena(), data.asSlice(r, len - r), callback));
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
     *   Send msg over the channel, invoking its callback if successful, otherwise copy the data locally for channel to become writable
     */
    private void sendMsg(WriteBuffer writeBuffer, Callback callback) {
        MemorySegment data = writeBuffer.toSegment();
        if(taskQueue == null) {
            int len = (int) data.byteSize();
            int r;
            try{
                r = protocol.doWrite(data, len);
            }catch (RuntimeException e) {
                log.error("Failed to perform doWrite()", e);
                close();
                return ;
            }
            if(r >= 0) {
                taskQueue = new ArrayDeque<>();
                copyLocally(data, callback);
                if(r > 0) {
                    handleEvent(r);
                }
            }else {
                int written = -r;
                if(written == len) {
                    if(callback != null) {
                        callback.invokeOnSuccess(channel);
                    }
                }else {
                    taskQueue = new ArrayDeque<>();
                    copyLocally(data.asSlice(written, len - written), callback);
                }
            }
        }else {
            copyLocally(data, callback);
        }
    }

    private void copyLocally(MemorySegment segment, Callback callback) {
        Arena arena = Arena.ofConfined();
        long size = segment.byteSize();
        MemorySegment memorySegment = arena.allocateArray(ValueLayout.JAVA_BYTE, size);
        MemorySegment.copy(segment, 0, memorySegment, 0, size);
        taskQueue.addLast(new Task(arena, memorySegment, callback));
    }

    private void handleEvent(int r) {
        if(r == Constants.NET_PW || r == Constants.NET_PR) {
            ctl(r);
        }else if(r != Constants.NET_IGNORED) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    private void ctl(int expected) {
        if(ctlWithStateChecked(expected)) {
            close();
        }
    }

    private boolean ctlWithStateChecked(int expected) {
        try(Mutex _ = channelState.withMutex()) {
            int current = channelState.get();
            if((current & Constants.NET_PC) > 0) {
                return true;
            }
            if(expected == Constants.NET_PW && (current & Constants.NET_W) == 0) {
                osNetworkLibrary.ctl(channel.poller().mux(), channel.socket(), current, current + Constants.NET_W);
                channelState.set(current + Constants.NET_W);
            }else if(expected == Constants.NET_PR && (current & Constants.NET_R) == 0) {
                osNetworkLibrary.ctl(channel.poller().mux(), channel.socket(), current, current + Constants.NET_R);
                channelState.set(current + Constants.NET_R);
            }else {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
            return false;
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
                int current = channelState.get();
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
                    task.arena().close();
                    Callback callback = task.callback();
                    if(callback != null) {
                        callback.invokeOnFailure(channel);
                    }
                });
                taskQueue = null;
            }
            try (Mutex _ = channelState.withMutex()) {
                int current = channelState.get();
                channelState.set(current | Constants.NET_WC);
                if((current & Constants.NET_PC) > 0) {
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