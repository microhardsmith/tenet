package cn.zorcc.common.network;

import cn.zorcc.common.network.api.Decoder;
import cn.zorcc.common.network.api.Encoder;
import cn.zorcc.common.network.api.Handler;

import java.time.Duration;
import java.util.Collection;
import java.util.function.IntFunction;

public sealed interface Channel permits ChannelImpl {
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
}

