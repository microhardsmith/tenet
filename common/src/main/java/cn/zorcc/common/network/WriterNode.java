package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.structure.*;

import java.lang.foreign.MemorySegment;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

public sealed interface WriterNode permits WriterNode.ProtocolWriterNode {
    OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;
    /**
     *   This function would be invoked when channel wants to send a msg
     */
    void onMsg(MemorySegment reserved, WriterTask writerTask);

    /**
     *   This function would be invoked when channel wants to send multiple msgs
     */
    void onMultipleMsg(MemorySegment reserved, WriterTask writerTask);

    /**
     *   This function would be invoked when channel become writable
     */
    void onWritable(WriterTask writerTask);

    /**
     *   This function would be invoked when channel wants to shutdown
     */
    void onShutdown(WriterTask writerTask);

    /**
     *   This function would be invoked when channel wants to force-close
     */
    void onClose(WriterTask writerTask);

    /**
     *   Exit current writerNode
     */
    void exit(Duration duration);

    /**
     *   Currently, writerNode has only one implementation, so it's designed as a utility class rather than an interface
     */
    final class ProtocolWriterNode implements WriterNode {
        private static final Logger log = new Logger(ProtocolWriterNode.class);
        private record Task(
                MemorySegment base,
                MemorySegment data,
                WriterCallback writerCallback
        ) {

        }

        private final IntMap<WriterNode> nodeMap;
        private final Channel channel;
        private final Protocol protocol;
        private final Mutex mutex;
        private final MemApi memApi;
        /**
         *   Tasks exist means current channel is not writable, incoming data should be wrapped into tasks list first, normally it's null
         */
        private Deque<Task> taskQueue;
        /**
         *   Whether the sender has been shutdown, normally it's null, could exist when tasks is not null
         */
        private Duration timeout;

        public ProtocolWriterNode(IntMap<WriterNode> nodeMap, Channel channel, Protocol protocol, Mutex mutex, MemApi memApi) {
            this.nodeMap = nodeMap;
            this.channel = channel;
            this.protocol = protocol;
            this.mutex = mutex;
            this.memApi = memApi;
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
                        sendMsg(writeBuffer, reserved, writerCallback);
                    }else if(writerCallback != null) {
                        // if nothing needs to be written, assume that's a success move
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
                        sendMsg(writeBuffer, reserved, writerCallback);
                    }else if(writerCallback != null) {
                        // if nothing needs to be written, assume that's a success move
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
                    MemorySegment base = task.base();
                    MemorySegment data = task.data();
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
                        // msg has been successfully transferred
                        memApi.freeMemory(base);
                        if(writerCallback != null) {
                            writerCallback.invokeOnSuccess(channel);
                        }
                    }else {
                        // partly transferred, waiting for next signal
                        taskQueue.addFirst(new Task(base, data, writerCallback));
                        if(r < 0L) {
                            handleEvent(Math.toIntExact(-r));
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
         *   If current channel is not writable, then the message would be written into a new allocated memory, which would save us a memcpy()
         */
        private WriteBuffer newWriteBuffer(MemorySegment reserved) {
            if(taskQueue == null) {
                return WriteBuffer.newReservedWriteBuffer(memApi, reserved);
            }else {
                return WriteBuffer.newNativeWriteBuffer(memApi, reserved.byteSize());
            }
        }

        /**
         *   Send msg over the channel, invoking its callback if successful, otherwise copy the data locally for channel to become writable
         */
        private void sendMsg(WriteBuffer writeBuffer, MemorySegment reserved, WriterCallback writerCallback) {
            final MemorySegment base = writeBuffer.asSegment();
            MemorySegment data = base;
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
                    // msg has been successfully transferred
                    if(writerCallback != null) {
                        writerCallback.invokeOnSuccess(channel);
                    }
                }else {
                    // store the data locally
                    taskQueue = new ArrayDeque<>();
                    if(base.address() == reserved.address()) {
                        // data still in reserved, manually copy it
                        MemorySegment newData = memApi.allocateMemory(len).reinterpret(len);
                        MemorySegment.copy(data, 0L, newData, 0L, len);
                        taskQueue.addLast(new Task(base, newData, writerCallback));
                    }else {
                        // data could be reused
                        taskQueue.addLast(new Task(base, data, writerCallback));
                    }
                    if(r < 0L) {
                        handleEvent(Math.toIntExact(-r));
                    }
                }
            }else {
                // here we know that data must not be reserved, so we could directly cache it
                taskQueue.addLast(new Task(base, data, writerCallback));
            }
        }

        private void handleEvent(int r) {
            switch (r) {
                case Constants.NET_PR -> ctl(Constants.NET_R);
                case Constants.NET_PW -> ctl(Constants.NET_W);
                case Constants.NET_PRW -> ctl(Constants.NET_RW);
                case Constants.NET_IGNORED -> {}
                default -> throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
        }

        private void ctl(int expected) {
            int state = mutex.wLock();
            try{
                if((state & Constants.NET_PC) != 0) {
                    // Poller has been closed, and it's not writable, so let's just close the Writer as well
                    clearTaskQueue();
                    closeProtocol();
                    checkPotentialExit();
                }else {
                    int from = state & Constants.NET_RW;
                    int to = from | expected;
                    if(to != from) {
                        osNetworkLibrary.ctlMux(channel.poller().mux(), channel.socket(), from, to, memApi);
                        state += to - from;
                    }
                }
            } finally {
                mutex.wUnlock(state);
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

        /**
         *   When shutdown is invoked, taskQueue must be null
         */
        private void shutdown(Duration duration) {
            if(nodeMap.remove(channel.socket().intValue(), this)) {
                int state = mutex.wLock();
                try {
                    if((state & Constants.NET_PC) != 0) {
                        closeProtocol();
                    }else {
                        shutdownProtocol(duration);
                    }
                    state |= Constants.NET_WC;
                } finally {
                    mutex.wUnlock(state);
                }
                checkPotentialExit();
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
                clearTaskQueue();
                int state = mutex.wLock();
                try {
                    if((state & Constants.NET_PC) != 0) {
                        closeProtocol();
                    }else {
                        channel.poller().submit(new PollerTask(PollerTaskType.CLOSE, channel, null));
                    }
                    state |= Constants.NET_WC;
                } finally {
                    mutex.wUnlock(state);
                }
                checkPotentialExit();
            }
        }

        private void clearTaskQueue() {
            if(taskQueue != null) {
                taskQueue.forEach(task -> {
                    memApi.freeMemory(task.base());
                    WriterCallback writerCallback = task.writerCallback();
                    if(writerCallback != null) {
                        writerCallback.invokeOnFailure(channel);
                    }
                });
                taskQueue = null;
            }
        }

        private void checkPotentialExit() {
            if(nodeMap.isEmpty()) {
                channel.writer().submit(new WriterTask(WriterTaskType.POTENTIAL_EXIT, null, null, null));
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
}
