package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.network.api.Callback;
import cn.zorcc.common.network.api.Decoder;
import cn.zorcc.common.network.api.Encoder;
import cn.zorcc.common.network.api.Handler;
import cn.zorcc.common.structure.Wheel;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.IntFunction;

public final class Channel {
    public static final int SEQ = 0;
    private static final Logger log = new Logger(Channel.class);
    /**
     *   Default shutdown timeout for each channel
     */
    private static final Duration defaultShutdownDuration = Duration.ofSeconds(5);
    /**
     *   Default send timeout for each channel
     */
    private static final Duration defaultSendTimeoutDuration = Duration.ofSeconds(30);
    private final Socket socket;
    private final Encoder encoder;
    private final Decoder decoder;
    private final Handler handler;
    private final Poller poller;
    private final Writer writer;
    private final Loc loc;
    private final AtomicInteger tg = new AtomicInteger(SEQ + 1);
    private final AtomicBoolean st = new AtomicBoolean(false);

    public Channel(Socket socket, Encoder encoder, Decoder decoder, Handler handler, Poller poller, Writer writer, Loc loc) {
        this.socket = socket;
        this.encoder = encoder;
        this.decoder = decoder;
        this.handler = handler;
        this.poller = poller;
        this.writer = writer;
        this.loc = loc;
    }

    public Socket socket() {
        return socket;
    }

    public Encoder encoder() {
        return encoder;
    }

    public Decoder decoder() {
        return decoder;
    }

    public Handler handler() {
        return handler;
    }

    public Poller poller() {
        return poller;
    }

    public Writer writer() {
        return writer;
    }

    public Loc loc() {
        return loc;
    }

    /**
     *   Send msg over the channel, this method could be invoked from any thread
     *   the msg will be processed by the writer thread, there is no guarantee that the msg will deliver, the callBack only indicates that calling operating system's API was successful
     *   the caller should provide a timeout mechanism to ensure the msg is not dropped, such as retransmission timeout
     */
    public void sendMsg(Object msg, Callback callback) {
        if(msg == null) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
        writer.submit(new WriterTask(WriterTaskType.SINGLE_MSG, this, msg, callback));
    }

    public void sendMsg(Object msg) {
        sendMsg(msg, null);
    }

    /**
     *  Send multiple msg over the channel, aggregate several msg together could reduce the system call times for better efficiency
     */
    public void sendMultipleMsg(Collection<Object> msgs, Callback callback) {
        if(msgs == null || msgs.isEmpty()) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
        writer.submit(new WriterTask(WriterTaskType.MULTIPLE_MSG, this, msgs, callback));
    }

    public void sendMultipleMsg(Collection<Object> msgs) {
        sendMultipleMsg(msgs, null);
    }

    public Object sendTaggedMsg(IntFunction<Object> taggedFunction, Duration timeout) {
        if(taggedFunction == null) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
        int tag = tg.updateAndGet(i -> i == SEQ - 1 ? SEQ + 1 : i + 1);
        Object msg = taggedFunction.apply(tag);
        if(msg == null) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
        return sendMsgWithTimeout(msg, tag, timeout);
    }

    public Object sendTaggedMsg(IntFunction<Object> taggedFunction) {
        return sendTaggedMsg(taggedFunction, defaultSendTimeoutDuration);
    }

    public Object sendMultipleTaggedMsg(IntFunction<Collection<Object>> taggedFunctions, Duration timeout) {
        if(taggedFunctions == null) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
        int tag = tg.updateAndGet(i -> i == SEQ - 1 ? SEQ + 1 : i + 1);
        Collection<Object> msgs = taggedFunctions.apply(tag);
        if(msgs == null || msgs.isEmpty()) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
        return sendMultipleMsgWithTimeout(msgs, tag, timeout);
    }

    public Object sendMultipleTaggedMsg(IntFunction<Collection<Object>> taggedFunctions) {
        return sendMultipleTaggedMsg(taggedFunctions, defaultSendTimeoutDuration);
    }

    public Object sendCircleMsg(Object msg, Duration timeout) {
        if(msg == null) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
        return sendMsgWithTimeout(msg, SEQ, timeout);
    }

    public Object sendCircleMsg(Object msg) {
        return sendCircleMsg(msg, defaultSendTimeoutDuration);
    }

    public Object sendMultipleCircleMsg(Collection<Object> msgs, Duration timeout) {
        if(msgs == null || msgs.isEmpty()) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
        return sendMultipleMsgWithTimeout(msgs, SEQ, timeout);
    }

    public Object sendMultipleCircleMsg(Collection<Object> msgs) {
        return sendMultipleCircleMsg(msgs, defaultSendTimeoutDuration);
    }

    private Object sendMsgWithTimeout(Object msg, int tag, Duration timeout) {
        TaggedMsg taggedMsg = new TaggedMsg(tag);
        Duration duration = timeout == null ? defaultSendTimeoutDuration : timeout;
        poller.submit(new PollerTask(PollerTaskType.REGISTER, this, taggedMsg));
        writer.submit(new WriterTask(WriterTaskType.SINGLE_MSG, this, msg, new Callback() {
            @Override
            public void onSuccess(Channel channel) {
                Wheel.wheel().addJob(() -> channel.poller().submit(new PollerTask(PollerTaskType.TIMEOUT, channel, taggedMsg)), duration);
            }

            @Override
            public void onFailure(Channel channel) {
                poller.submit(new PollerTask(PollerTaskType.UNREGISTER, channel, taggedMsg));
            }
        }));
        LockSupport.park();
        return taggedMsg.carrier().target().get();
    }

    private Object sendMultipleMsgWithTimeout(Collection<Object> msgs, int tag, Duration timeout) {
        TaggedMsg taggedMsg = new TaggedMsg(tag);
        Duration duration = timeout == null ? defaultSendTimeoutDuration : timeout;
        poller.submit(new PollerTask(PollerTaskType.REGISTER, this, taggedMsg));
        writer.submit(new WriterTask(WriterTaskType.MULTIPLE_MSG, this, msgs, new Callback() {
            @Override
            public void onSuccess(Channel channel) {
                Wheel.wheel().addJob(() -> channel.poller().submit(new PollerTask(PollerTaskType.TIMEOUT, channel, taggedMsg)), duration);
            }

            @Override
            public void onFailure(Channel channel) {
                poller.submit(new PollerTask(PollerTaskType.UNREGISTER, channel, taggedMsg));
            }
        }));
        LockSupport.park();
        return taggedMsg.carrier().target().get();
    }

    public void shutdown() {
        shutdown(defaultShutdownDuration);
    }

    public void shutdown(Duration duration) {
        if(st.compareAndSet(false, true)) {
            try{
                handler.onShutdown(this);
            }catch (RuntimeException e) {
                log.error("Err occurred in onShutdown()", e);
                writer.submit(new WriterTask(WriterTaskType.CLOSE, this, null, null));
                return ;
            }
            writer.submit(new WriterTask(WriterTaskType.SHUTDOWN, this, duration, null));
        }
    }
}
