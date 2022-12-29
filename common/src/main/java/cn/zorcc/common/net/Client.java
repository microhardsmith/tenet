package cn.zorcc.common.net;

import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;

/**
 *  通用tcp客户端
 */
@Slf4j
public class Client {
    private final EventLoopGroup workerGroup = GlobalEventLoopGroup.workerEventLoopGroup();

    public Client() {

    }
}
