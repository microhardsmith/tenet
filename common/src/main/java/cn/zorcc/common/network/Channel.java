package cn.zorcc.common.network;

import cn.zorcc.common.Carrier;
import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.structure.Loc;
import cn.zorcc.common.structure.Wheel;

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
 *   Note that there's no method as sendMultipleTaggedMsg() or sendMultipleRoundedMsg(), developers should make sure when using virtual thread, the response will be decoded as exactly one Object
 *   Many developers would like to make encoder and decoder as simple as possible, to divide the message flow into object pieces, which could cause inconsistency problems in virtual threading model
 *   For example, if 10 messages were expected to unpark the sender thread, and only 1 were received, what should
 */
public record Channel(
        Socket socket,
        Encoder encoder,
        Decoder decoder,
        Handler handler,
        Worker worker,
        Loc loc,
        AtomicInteger state,
        AtomicInteger taggedMsgGenerator,
        AtomicInteger synchronousMsgGenerator
) {
    private static final Logger log = new Logger(Channel.class);
    private static final OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;
    private static final Duration defaultShutdownDuration = Duration.ofSeconds(5);
    private static final Duration defaultSendTimeoutDuration = Duration.ofSeconds(30);
    private static final int tagMapSize = 16;
    private static final Wheel wheel = Wheel.wheel();

    public Channel(Socket socket,
                   Encoder encoder,
                   Decoder decoder,
                   Handler handler,
                   Worker worker,
                   Loc loc,
                   AtomicInteger state) {
        this(socket, encoder, decoder, handler, worker, loc, state, new AtomicInteger(1), new AtomicInteger(-1));
    }

    /**
     *   Send msg over the channel, this method could be invoked from any thread
     *   the msg will be processed by the writer thread, there is no guarantee that the msg will deliver, the callBack only indicates that calling operating system's API was successful
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

    /**
     *  Send multiple msg over the channel, aggregate several msg together could reduce the system call times for better efficiency
     */
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
     *   SynchronousMsg is just like taggedMsg, but without a tag,
     *   it's targeted at network-protocol which for each request, a new request is only initiated after receiving a reply
     */
    public Object sendSynchronousMsg(Object msg, Duration timeout, WriterCallback callback) {
        if(msg == null) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
        int tag = synchronousMsgGenerator.getAndUpdate(i -> i > 0 ? -1 : i - 1);
        return sendMsgInVirtualThread(msg, tag, timeout, callback);
    }

    public Object sendSynchronousMsg(Object msg) {
        return sendSynchronousMsg(msg, defaultSendTimeoutDuration, null);
    }

    public Object sendSynchronousMsg(Object msg, Duration timeout) {
        return sendSynchronousMsg(msg, timeout, null);
    }

    public Object sendSynchronousMsg(Object msg, WriterCallback callback) {
        return sendSynchronousMsg(msg, defaultSendTimeoutDuration, callback);
    }

    /**
     *   Send msg using a taggedFunction, this approach allows a virtual thread to wait until the response
     *   there are several things developers should take care:
     *   1. You must only use this method in virtual threads only, platform thread blocking is much more expensive than virtual threads, there is no point in using this method in platform threads
     *   2. The request and the response should always be in the same channel, or it won't be detected
     *   3. We don't care how you deal with the tag in your own msg payload, but it must be resolved from the response
     */
    public Object sendTaggedMsg(IntFunction<Object> taggedFunction, Duration timeout, WriterCallback callBack) {
        if(taggedFunction == null) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
        int t = taggedMsgGenerator.updateAndGet(i -> i < 0 ? 1 : i + 1);
        Object msg = taggedFunction.apply(t);
        if(msg == null) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
        return sendMsgInVirtualThread(msg, t, timeout, callBack);
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
     *   Used for sending message in virtual thread, park until receiving response, return null if timeout
     */
    private Object sendMsgInVirtualThread(Object msg, int t, Duration timeout, WriterCallback callBack) {
        TaggedMsg taggedMsg = new TaggedMsg(t, Carrier.create());
        if(timeout != null) {
            wheel.addJob(() -> {
                Carrier carrier = taggedMsg.carrier();
                AtomicReference<Object> target = carrier.target();
                if(target != null && target.compareAndSet(Carrier.HOLDER, null)) {
                    LockSupport.unpark(carrier.thread());
                    worker.submitReaderTask(new ReaderTask(ReaderTask.ReaderTaskType.UNREGISTER_MSG, null, this, null, taggedMsg));
                }
            }, timeout);
        }
        worker.submitReaderTask(new ReaderTask(ReaderTask.ReaderTaskType.REGISTER_MSG, null, this, null, taggedMsg));
        worker.submitWriterTask(new WriterTask(WriterTask.WriterTaskType.MSG, this, msg, callBack));
        LockSupport.park();
        return taggedMsg.carrier().target().get();
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
}
