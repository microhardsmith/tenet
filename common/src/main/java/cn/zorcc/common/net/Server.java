package cn.zorcc.common.net;

import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;

/**
 *  通用tcp服务器
 */
@Slf4j
public class Server {
    private final EventLoopGroup bossGroup = GlobalEventLoopGroup.bossEventLoopGroup();
    private final EventLoopGroup workerGroup = GlobalEventLoopGroup.workerEventLoopGroup();

    public Server() {

    }
}
