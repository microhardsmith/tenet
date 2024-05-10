package cn.zorcc.common.network;

/**
 *   Used as writer message
 */
public record WriterTask(
        WriterTaskType type,
        Channel channel,
        Object msg,
        WriterCallback writerCallback
) {

}
