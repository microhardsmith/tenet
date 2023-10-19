package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.structure.Loc;
import cn.zorcc.common.structure.Wheel;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.IntFunction;

/**
 *   Network channel abstraction, could only get evolved from acceptor
 *   in channel state, the socket read and write operation would be taken over by Encoder, Decoder and Handler using Protocol
 */
public final class Channel implements Actor {
    private static final Logger log = new Logger(Channel.class);
    private static final OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;
    private static final Duration defaultShutdownDuration = Duration.ofSeconds(5);
    private static final Duration defaultSendTimeoutDuration = Duration.ofSeconds(30);
    private static final Wheel wheel = Wheel.wheel();
    private final AtomicInteger tagGenerator = new AtomicInteger(Constants.ZERO);
    private final Socket socket;
    private final AtomicInteger state;
    private final Encoder encoder;
    private final Decoder decoder;
    private final Handler handler;
    private final Protocol protocol;
    private final Worker worker;
    /**
     *   Representing remote server address
     */
    private final Loc loc;
    /**
     *   Current read buffer temp zone, visible only for its worker
     */
    private WriteBuffer tempBuffer;

    public Channel(Socket socket, AtomicInteger state, Encoder e, Decoder d, Handler h, Protocol protocol, Loc loc, Worker worker) {
        this.socket = socket;
        this.state = state;
        this.encoder = e;
        this.decoder = d;
        this.handler = h;
        this.protocol = protocol;
        this.loc = loc;
        this.worker = worker;
    }

    @Override
    public int hashCode() {
        return socket.intValue();
    }

    public Socket socket() {
        return socket;
    }

    public AtomicInteger state() {
        return state;
    }

    public Handler handler() {
        return handler;
    }

    public Protocol protocol() {
        return protocol;
    }

    public Worker worker() {
        return worker;
    }

    public Loc loc() {
        return loc;
    }

    public Encoder encoder() {
        return encoder;
    }

    @Override
    public void canRead(MemorySegment buffer) {
        protocol.canRead(this, buffer);
    }

    @Override
    public void canWrite() {
        protocol.canWrite(this);
    }

    @Override
    public void canShutdown(Duration duration) {
        shutdown(duration);
    }

    /**
     *   Deal with incoming ReadBuffer, should only be accessed in reader thread
     */
    public void onReadBuffer(ReadBuffer buffer) {
        if(tempBuffer != null) {
            tempBuffer.writeSegment(buffer.rest());
            ReadBuffer readBuffer = new ReadBuffer(tempBuffer.content());
            tryRead(readBuffer);
            if(readBuffer.readIndex() < readBuffer.size()) {
                tempBuffer = tempBuffer.truncate(readBuffer.readIndex());
            }else {
                tempBuffer.close();
                tempBuffer = null;
            }
        }else {
            tryRead(buffer);
            if(buffer.readIndex() < buffer.size()) {
                tempBuffer = WriteBuffer.newDefaultWriteBuffer(Arena.ofConfined(), buffer.size());
                tempBuffer.writeSegment(buffer.rest());
            }
        }
    }

    /**
     *   Try reading from ReadBuffer, should only be accessed in reader thread
     *   the actual onRecv method should create a new virtual thread to handle the actual blocking business logic
     */
    private void tryRead(ReadBuffer buffer) {
        try{
            Object result = decoder.decode(buffer);
            if(result != null) {
                handler.onRecv(this, result);
            }
        }catch (FrameworkException e) {
            log.error(STR."Failed to perform reading from channel : \{loc}", e);
            shutdown();
        }
    }

    /**
     *   Send msg over the channel, this method could be invoked from any thread
     *   the msg will be processed by the writer thread, there is no guarantee that the msg will delivery, the callBack only indicates that calling operating system's API was successful
     *   the caller should provide a timeout mechanism to ensure the msg is not dropped, such as retransmission timeout
     */
    public void sendMsg(Object msg, WriterCallback callback) {
        if(msg == null) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
        worker.submitWriterTask(new WriterTask(WriterTask.WriterTaskType.MSG, this, msg, callback));
    }

    public void sendMsg(Object msg) {
        sendMsg(msg, null);
    }

    public void sendMultipleMsg(List<Object> msgList, WriterCallback callback) {
        if(msgList == null || msgList.isEmpty()) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
        worker.submitWriterTask(new WriterTask(WriterTask.WriterTaskType.MULTIPLE_MSG, this, msgList, callback));
    }

    public void sendMultipleMsg(List<Object> msgList) {
        sendMultipleMsg(msgList, null);
    }

    /**
     *   Send msg using a taggedFunction, this approach allows a virtual thread to wait until the response
     *   there are several things developers should take care:
     *   1. You must only use this method in virtual threads, since platform thread blocking is much more expensive than virtual threads, there is no point in using this method in platform threads
     *   2. The request and the response should always be in the same channel, or it won't be detected
     *   3. We don't care how you deal with the tag in your own msg payload, but it must could be resolved from the response
     */
    public Object sendTaggedMsg(IntFunction<Object> taggedFunction, Duration timeout, WriterCallback callBack) {
        int tag = tagGenerator.getAndIncrement();
        Object msg = taggedFunction.apply(tag);
        TaggedMsg taggedMsg = new TaggedMsg(tag);
        wheel.addJob(() -> {
            AtomicReference<Object> target = taggedMsg.getTarget();
            if(target != null && target.compareAndSet(TaggedMsg.HOLDER, null)) {
                LockSupport.unpark(taggedMsg.getThread());
                worker.submitReaderTask(ReaderTask.createRemoveTagTask(this, taggedMsg));
            }
        }, timeout);
        worker.submitReaderTask(ReaderTask.createAddTagTask(this, taggedMsg));
        sendMsg(msg, callBack);
        LockSupport.park();
        return taggedMsg.getTarget().get();
    }

    public Object sendTaggedMsg(IntFunction<Object> taggedFunction, Duration timeout) {
        return sendTaggedMsg(taggedFunction, timeout, null);
    }

    public Object sendTaggedMsg(IntFunction<Object> taggedFunction, WriterCallback callBack) {
        return sendTaggedMsg(taggedFunction, defaultSendTimeoutDuration, callBack);
    }

    public Object sendTaggedMsg(IntFunction<Object> taggedFunction) {
        return sendTaggedMsg(taggedFunction, defaultSendTimeoutDuration, null);
    }

    /**
     *   After receiving a msg with a tag, this method could be invoked in Handler.onRecv() to unpark the sender thread
     */
    public void receiveTaggedMsg(int tag, Object received) {
        worker.receiveTaggedMsg(this, tag, received);
    }

    /**
     *   Shutdown current channel's write side, this method could be invoked from any thread
     *   Note that shutdown current channel doesn't block the recv operation, the other side will recv 0 and close the socket
     *   in fact, socket will only be closed when worker thread recv EOF from remote peer
     *   Calling shutdown for multiple times doesn't matter since writerThread will exit when handling the first shutdown signal
     */
    public void shutdown(Duration duration) {
        handler.onShutdown(this);
        worker.submitWriterTask(new WriterTask(WriterTask.WriterTaskType.SHUTDOWN, this, duration, null));
    }

    /**
     *   Shutdown with default timeout configuration
     */
    public void shutdown() {
        shutdown(defaultShutdownDuration);
    }

    /**
     *   Close current channel, release it from the worker and clean up, could only be invoked from reader thread
     */
    public void close() {
        if(Thread.currentThread() != worker.reader()) {
            throw new FrameworkException(ExceptionType.NETWORK, "Not in worker thread");
        }
        // make sure the channel could only be removed once
        if(worker.unregister(socket, this)) {
            // Force close the sender instance
            worker.submitWriterTask(new WriterTask(WriterTask.WriterTaskType.REMOVE, this, null, null));
            int current = state.getAndSet(OsNetworkLibrary.REGISTER_NONE);
            if(current > OsNetworkLibrary.REGISTER_NONE) {
                osNetworkLibrary.ctl(worker.mux(), socket, current, OsNetworkLibrary.REGISTER_NONE);
            }
            protocol.doClose(this);
            handler.onRemoved(this);
            if (worker.counter().decrementAndGet() == Constants.ZERO) {
                worker.submitReaderTask(ReaderTask.POSSIBLE_SHUTDOWN_TASK);
            }
        }
    }
}
