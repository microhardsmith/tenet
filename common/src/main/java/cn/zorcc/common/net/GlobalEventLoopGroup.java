package cn.zorcc.common.net;

import cn.zorcc.common.Constants;
import cn.zorcc.common.util.PlatformUtil;
import cn.zorcc.common.util.ThreadUtil;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.nio.ByteBuffer;
import java.util.concurrent.ThreadFactory;

/**
 *  全局EventLoopGroup,用于在Client和Server之间进行复用
 */
public enum GlobalEventLoopGroup {

    INSTANCE;
    private static final String BOSS_THREAD_NAME = "Boss";
    private static final String WORKER_THREAD_NAME = "Worker";
    private final EventLoopGroup eventLoopGroup;

    GlobalEventLoopGroup() {
        ThreadFactory threadFactory = ThreadUtil.threadFactory(WORKER_THREAD_NAME);
        int cpuCores = PlatformUtil.getCpuCores();
        this.eventLoopGroup = Epoll.isAvailable() ? new EpollEventLoopGroup(cpuCores, threadFactory) : new NioEventLoopGroup(cpuCores, threadFactory);
    }

    /**
     *  用于获取全局通用worker group
     */
    public static EventLoopGroup workerEventLoopGroup() {
        return INSTANCE.eventLoopGroup;
    }

    /**
     *  用于创建Server使用的boss group
     */
    public static EventLoopGroup bossEventLoopGroup() {
        ThreadFactory threadFactory = ThreadUtil.threadFactory(BOSS_THREAD_NAME);
        return Epoll.isAvailable() ? new EpollEventLoopGroup(Constants.ONE, threadFactory) : new NioEventLoopGroup(Constants.ONE, threadFactory);
    }
}
