package cn.zorcc.common.network;


import java.time.Duration;

public record ReaderTask (
    ReaderTaskType type,
    Connector connector,
    Channel channel,
    Duration duration,
    TaggedMsg taggedMsg
) {
    enum ReaderTaskType {
        ADD_CHANNEL,
        CLOSE_CHANNEL,
        CLOSE_ACTOR,
        REGISTER_MSG,
        UNREGISTER_MSG,
        GRACEFUL_SHUTDOWN,
        POSSIBLE_SHUTDOWN,
    }
    public static final ReaderTask POSSIBLE_SHUTDOWN_TASK = new ReaderTask(ReaderTaskType.POSSIBLE_SHUTDOWN, null, null, null);
}
