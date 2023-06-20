package cn.zorcc.common.network;

public record WriterTask(
        WriterTaskType type,
        Channel channel,
        Object msg
) {
    enum WriterTaskType {
        INITIATE,
        WRITABLE,
        MIX_OF_MSG,
        MSG,
        SHUTDOWN,
        CLOSE,
        EXIT,
    }
}
