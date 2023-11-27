package cn.zorcc.common.network;

import cn.zorcc.common.network.api.Callback;

public record WriterTask(
        WriterTaskType type,
        Channel channel,
        Object msg,
        Callback callback
) {

}
