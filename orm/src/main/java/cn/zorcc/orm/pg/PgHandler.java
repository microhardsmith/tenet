package cn.zorcc.orm.pg;

import cn.zorcc.common.network.Channel;
import cn.zorcc.common.network.Handler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class PgHandler implements Handler {

    private final BlockingQueue<Object> channelQueue = new LinkedTransferQueue<>();
    @Override
    public void onConnected(Channel channel) {

        channel.send(new PgStartUpMsg());
    }

    @Override
    public void onRecv(Channel channel, Object data) {
        if(data instanceof PgMsg pgMsg) {

        }
    }

    @Override
    public void onRemoved(Channel channel) {

    }
}
