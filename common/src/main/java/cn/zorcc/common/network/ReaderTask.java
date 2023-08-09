package cn.zorcc.common.network;


public record ReaderTask (
    ReaderTaskType type,
    Acceptor acceptor,
    Channel channel,
    Shutdown shutdown
) {
    enum ReaderTaskType {
        ADD_ACCEPTOR,
        CLOSE_ACCEPTOR,
        CLOSE_CHANNEL,
        GRACEFUL_SHUTDOWN,
        POSSIBLE_SHUTDOWN,
    }
}
