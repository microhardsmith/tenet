package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
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

public record ChannelImpl(
        Socket socket,
        Encoder encoder,
        Decoder decoder,
        Handler handler,
        Poller poller,
        Writer writer,
        Loc loc,
        AtomicInteger tg,
        AtomicBoolean st
) implements Channel {
    private static final Logger log = new Logger(ChannelImpl.class);

    public ChannelImpl(Socket socket, Encoder encoder, Decoder decoder, Handler handler, Poller poller, Writer writer, Loc loc) {
        this(socket, encoder, decoder, handler, poller, writer, loc, new AtomicInteger(Channel.SEQ + 1), new AtomicBoolean(false));
    }

    @Override
    public void sendMsg(Object msg, WriterCallback writerCallback) {
        if(msg == null) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
        writer.submit(new WriterTask(WriterTaskType.SINGLE_MSG, this, msg, writerCallback));
    }

    @Override
    public void sendMultipleMsg(Collection<Object> msgs, WriterCallback writerCallback) {
        if(msgs == null || msgs.isEmpty()) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
        writer.submit(new WriterTask(WriterTaskType.MULTIPLE_MSG, this, msgs, writerCallback));
    }

    @Override
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

    @Override
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

    @Override
    public Object sendCircleMsg(Object msg, Duration timeout) {
        if(msg == null) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
        return sendMsgWithTimeout(msg, SEQ, timeout);
    }

    @Override
    public Object sendMultipleCircleMsg(Collection<Object> msgs, Duration timeout) {
        if(msgs == null || msgs.isEmpty()) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
        return sendMultipleMsgWithTimeout(msgs, SEQ, timeout);
    }

    private Object sendMsgWithTimeout(Object msg, int tag, Duration timeout) {
        TaggedMsg taggedMsg = new TaggedMsg(tag);
        Duration duration = timeout == null ? defaultSendTimeoutDuration : timeout;
        poller.submit(new PollerTask(PollerTaskType.REGISTER, this, taggedMsg));
        writer.submit(new WriterTask(WriterTaskType.SINGLE_MSG, this, msg, new WriterCallback() {
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
        writer.submit(new WriterTask(WriterTaskType.MULTIPLE_MSG, this, msgs, new WriterCallback() {
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

    @Override
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
