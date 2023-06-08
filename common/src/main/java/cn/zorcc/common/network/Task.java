package cn.zorcc.common.network;


public record Task(
    TaskType type,
    Acceptor acceptor,
    Channel channel,
    Shutdown shutdown
) {
    enum TaskType {
        ADD_ACCEPTOR,
        CLOSE_ACCEPTOR,
        CLOSE_CHANNEL,
        GRACEFUL_SHUTDOWN,
        POSSIBLE_SHUTDOWN,
    }
}
