package cn.zorcc.common.network;


import java.time.Duration;

public record ReaderTask (
    ReaderTaskType type,
    Acceptor acceptor,
    Channel channel,
    Duration duration,
    TaggedMsg taggedMsg
) {
    enum ReaderTaskType {
        ADD_ACCEPTOR,
        CLOSE_ACCEPTOR,
        CLOSE_CHANNEL,
        ADD_TAG,
        REMOVE_TAG,
        GRACEFUL_SHUTDOWN,
        POSSIBLE_SHUTDOWN,
    }
    public static final ReaderTask POSSIBLE_SHUTDOWN_TASK = new ReaderTask(ReaderTaskType.POSSIBLE_SHUTDOWN, null, null, null, null);

    public static ReaderTask createAddAcceptorTask(Acceptor acceptor) {
        return new ReaderTask(ReaderTaskType.ADD_ACCEPTOR, acceptor, null, null, null);
    }

    public static ReaderTask createCloseAcceptorTask(Acceptor acceptor) {
        return new ReaderTask(ReaderTaskType.CLOSE_ACCEPTOR, acceptor, null, null, null);
    }

    public static ReaderTask createCloseChannelTask(Channel channel) {
        return new ReaderTask(ReaderTaskType.CLOSE_CHANNEL, null, channel, null, null);
    }

    public static ReaderTask createAddTagTask(Channel channel, TaggedMsg taggedMsg) {
        return new ReaderTask(ReaderTaskType.ADD_TAG, null, channel, null, taggedMsg);
    }

    public static ReaderTask createRemoveTagTask(Channel channel, TaggedMsg taggedMsg) {
        return new ReaderTask(ReaderTaskType.REMOVE_TAG, null, channel, null, taggedMsg);
    }

    public static ReaderTask createGracefulShutdownTask(Duration duration) {
        return new ReaderTask(ReaderTaskType.GRACEFUL_SHUTDOWN, null, null, duration, null);
    }
}
