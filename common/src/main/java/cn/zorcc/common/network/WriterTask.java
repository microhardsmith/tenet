package cn.zorcc.common.network;

public record WriterTask(
        WriterTaskType type,
        Channel channel,
        Object msg,
        WriterCallback writerCallback
) {

}
