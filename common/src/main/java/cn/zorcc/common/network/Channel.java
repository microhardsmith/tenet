package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.structure.Wheel;

import java.lang.foreign.MemorySegment;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

public sealed interface Channel permits Channel.ChannelImpl {
    /**
     *   Indicates current msg has been failed
     */
    Object FAILED = new Object();
    /**
     *   Indicates current msg has been timeout
     */
    Object TIMEOUT = new Object();
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
     *   Send a tagged msg over the channel, the sender must be a virtual thread, and tag must be a heap segment
     *   the response will automatically awaken the caller thread, or failed with timeout
     */
    Object sendTaggedMsg(Object msg, MemorySegment tag, Duration timeout);

    default Object sendTaggedMsg(Object msg, MemorySegment tag) {
        return sendTaggedMsg(msg, tag, defaultSendTimeoutDuration);
    }

    Object sendMultipleTaggedMsg(Collection<Object> msgs, MemorySegment tag, Duration timeout);

    default Object sendMultipleTaggedMsg(Collection<Object> msgs, MemorySegment tag) {
        return sendMultipleTaggedMsg(msgs, tag, defaultSendTimeoutDuration);
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
        return new ChannelImpl(socket, encoder, decoder, handler, poller, writer, loc, new AtomicBoolean(false));
    }

    record ChannelImpl(
            Socket socket,
            Encoder encoder,
            Decoder decoder,
            Handler handler,
            Poller poller,
            Writer writer,
            Loc loc,
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
        public Object sendTaggedMsg(Object msg, MemorySegment tag, Duration timeout) {
            if(msg == null || tag == null || tag == MemorySegment.NULL || tag.isNative()) {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
            return sendMsgWithTimeout(msg, tag, timeout);
        }

        @Override
        public Object sendMultipleTaggedMsg(Collection<Object> msgs, MemorySegment tag, Duration timeout) {
            if(msgs == null || msgs.isEmpty() || tag == null || tag == MemorySegment.NULL || tag.isNative()) {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
            return sendMultipleMsgWithTimeout(msgs, tag, timeout);
        }

        @Override
        public Object sendCircleMsg(Object msg, Duration timeout) {
            if(msg == null) {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
            return sendMsgWithTimeout(msg, MemorySegment.NULL, timeout);
        }

        @Override
        public Object sendMultipleCircleMsg(Collection<Object> msgs, Duration timeout) {
            if(msgs == null || msgs.isEmpty()) {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
            return sendMultipleMsgWithTimeout(msgs, MemorySegment.NULL, timeout);
        }

        private Object sendMsgWithTimeout(Object msg, MemorySegment tag, Duration timeout) {
            TagWithRef t = new TagWithRef(tag);
            Duration d = timeout == null ? defaultSendTimeoutDuration : timeout;
            poller.submit(new PollerTask(PollerTaskType.REGISTER, this, t));
            writer.submit(new WriterTask(WriterTaskType.SINGLE_MSG, this, msg, new WriterCallback() {
                @Override
                public void onSuccess(Channel channel) {
                    Wheel.wheel().addJob(() -> channel.poller().submit(new PollerTask(PollerTaskType.UNREGISTER, channel, t)), d);
                }

                @Override
                public void onFailure(Channel channel) {
                    t.ref().assign(FAILED);
                    poller.submit(new PollerTask(PollerTaskType.UNREGISTER, channel, t));
                }
            }));
            return t.ref().fetch();
        }

        private Object sendMultipleMsgWithTimeout(Collection<Object> msgs, MemorySegment tag, Duration timeout) {
            TagWithRef t = new TagWithRef(tag);
            Duration d = timeout == null ? defaultSendTimeoutDuration : timeout;
            poller.submit(new PollerTask(PollerTaskType.REGISTER, this, t));
            writer.submit(new WriterTask(WriterTaskType.MULTIPLE_MSG, this, msgs, new WriterCallback() {
                @Override
                public void onSuccess(Channel channel) {
                    Wheel.wheel().addJob(() -> channel.poller().submit(new PollerTask(PollerTaskType.UNREGISTER, channel, t)), d);
                }

                @Override
                public void onFailure(Channel channel) {
                    t.ref().assign(FAILED);
                    poller.submit(new PollerTask(PollerTaskType.UNREGISTER, channel, t));
                }
            }));
            return t.ref().fetch();
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

