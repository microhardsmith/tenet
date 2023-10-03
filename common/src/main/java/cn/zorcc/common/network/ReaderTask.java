package cn.zorcc.common.network;


import java.time.Duration;

public record ReaderTask (
    ReaderTaskType type,
    Acceptor acceptor,
    Channel channel,
    Duration duration
) {
    enum ReaderTaskType {
        ADD_ACCEPTOR,
        CLOSE_ACCEPTOR,
        CLOSE_CHANNEL,
        GRACEFUL_SHUTDOWN,
        POSSIBLE_SHUTDOWN,
    }
}
