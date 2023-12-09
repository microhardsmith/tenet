package cn.zorcc.common.network;

import cn.zorcc.common.network.api.Channel;

public record WriterTask(
        WriterTaskType type,
        Channel channel,
        Object msg,
        WriterCallback writerCallback
) {

}
