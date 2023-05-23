package cn.zorcc.common.net;

import cn.zorcc.common.network.Channel;
import cn.zorcc.common.network.Handler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestHandler implements Handler {
    @Override
    public void onConnected(Channel channel) {
        log.info("Connected to {}", channel.loc());
    }

    @Override
    public void onRecv(Channel channel, Object data) {
        log.info("receive data : {}", data);
    }

    @Override
    public void onClose(Channel channel) {
        log.info("Channel close : {}", channel.loc());
    }
}
