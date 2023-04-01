package cn.zorcc.common.net;

import cn.zorcc.common.network.Channel;
import cn.zorcc.common.network.ChannelHandler;
import cn.zorcc.common.pojo.Loc;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestHandler implements ChannelHandler {
    @Override
    public void onConnected(Loc loc) {
        log.info("Connected to {}", loc);
    }

    @Override
    public void onRecv(Channel channel, Object data) {
        log.info("receive data : {}", data);
    }

    @Override
    public void onClose() {
        log.info("Channel close");
    }
}
