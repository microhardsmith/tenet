package cn.zorcc.common.network;

import cn.zorcc.common.Carrier;
import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.structure.IntHolder;
import cn.zorcc.common.structure.Wheel;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.IntFunction;

public sealed interface Channel permits Channel.ChannelImpl {
    int SEQ = 0;
    /**
     *   Default send timeout for each channel
     */
    Duration defaultSendTimeoutDuration = Duration.ofSeconds(30);
    /**
     *   Default shutdown timeout for each channel
     */
    Duration defaultShutdownDuration = Duration.ofSeconds(5);

    /**
     *   Get the underlying socket associated with this channel
     */
    Socket socket();

    /**
     *   Get the underlying encoder associated with this channel
     */
    Encoder encoder();

    /**
     *   Get the underlying decoder associated with this channel
     */
    Decoder decoder();

    /**
     *   Get the underlying handler associated with this channel
     */
    Handler handler();

    /**
     *   Get the underlying poller associated with this channel
     */
    Poller poller();

    /**
     *   Get the underlying writer associated with this channel
     */
    Writer writer();

    /**
     *   Get the underlying loc associated with this channel
     *   For client, it represents the remote server address
     *   For server, it represents the remote client address
     */
    Loc loc();

    /**
     *   Get the underlying channel state associated with current channel
     *   Note that the state variable should only be internally used by the framework itself
     */
    IntHolder state();

    /**
     *   Send msg over the channel, this method could be invoked from any thread
     *   the msg will be processed by the writer thread, there is no guarantee that the msg will deliver, the callBack only indicates that calling operating system's API was successful
     *   the caller should provide a timeout mechanism to ensure the msg is not dropped, such as retransmission timeout
     */
    void sendMsg(Object msg, WriterCallback writerCallback);

    default void sendMsg(Object msg) {
        sendMsg(msg, null);
    }

    /**
     *   Send multiple msg over the channel, aggregate several msg together could reduce the system call times for better efficiency
     */
    void sendMultipleMsg(Collection<Object> msgs, WriterCallback writerCallback);

    default void sendMultipleMsg(Collection<Object> msgs) {
        sendMultipleMsg(msgs, null);
    }

    /**
     *   Send a tagged msg over the channel, the sender must be a virtual thread
     *   the response will automatically awaken the caller thread, or failed with timeout
     */
    Object sendTaggedMsg(IntFunction<Object> taggedFunction, Duration timeout);

    default Object sendTaggedMsg(IntFunction<Object> taggedFunction) {
        return sendTaggedMsg(taggedFunction, defaultSendTimeoutDuration);
    }

    Object sendMultipleTaggedMsg(IntFunction<Collection<Object>> taggedFunctions, Duration timeout);

    default Object sendMultipleTaggedMsg(IntFunction<Collection<Object>> taggedFunctions) {
        return sendMultipleTaggedMsg(taggedFunctions, defaultSendTimeoutDuration);
    }

    Object sendCircleMsg(Object msg, Duration timeout);

    default Object sendCircleMsg(Object msg) {
        return sendCircleMsg(msg, defaultSendTimeoutDuration);
    }

    Object sendMultipleCircleMsg(Collection<Object> msgs, Duration timeout);

    default Object sendMultipleCircleMsg(Collection<Object> msgs) {
        return sendMultipleCircleMsg(msgs, defaultSendTimeoutDuration);
    }

    void shutdown(Duration duration);

    default void shutdown() {
        shutdown(defaultShutdownDuration);
    }

    /**
     *   When the channel was first created, the state must be NET_W
     */
    static Channel newChannel(Socket socket, Encoder encoder, Decoder decoder, Handler handler, Poller poller, Writer writer, Loc loc) {
        return new ChannelImpl(socket, encoder, decoder, handler, poller, writer, loc, new IntHolder(Constants.NET_W), new AtomicInteger(Channel.SEQ + 1), new AtomicBoolean(false));
    }

    record ChannelImpl(
            Socket socket,
            Encoder encoder,
            Decoder decoder,
            Handler handler,
            Poller poller,
            Writer writer,
            Loc loc,
            IntHolder state,
            AtomicInteger tg,
            AtomicBoolean st
    ) implements Channel {
        private static final Logger log = new Logger(ChannelImpl.class);

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
                    Wheel.wheel().addJob(() -> channel.poller().submit(new PollerTask(PollerTaskType.UNREGISTER, channel, taggedMsg)), duration);
                }

                @Override
                public void onFailure(Channel channel) {
                    taggedMsg.carrier().cas(Carrier.HOLDER, Carrier.FAILED);
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
                    Wheel.wheel().addJob(() -> channel.poller().submit(new PollerTask(PollerTaskType.UNREGISTER, channel, taggedMsg)), duration);
                }

                @Override
                public void onFailure(Channel channel) {
                    taggedMsg.carrier().cas(Carrier.HOLDER, Carrier.FAILED);
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
}

