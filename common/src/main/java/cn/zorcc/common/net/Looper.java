package cn.zorcc.common.net;

import cn.zorcc.common.LifeCycle;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ConfigUtil;

import java.lang.foreign.Arena;

/**
 *  selector thread
 */
public interface Looper extends LifeCycle {

    /**
     *  校验NetConfig字段
     */
    static void validate(NetConfig netConfig) {
        if(!ConfigUtil.checkIp(netConfig.getIp())) {
            throw new FrameworkException(ExceptionType.NET, "IP address is illegal");
        }
        if(netConfig.getPort() < 0) {
            throw new FrameworkException(ExceptionType.NET, "Port must be a positive number");
        }
        if(netConfig.getBacklog() < 1) {
            throw new FrameworkException(ExceptionType.NET, "Backlog must be a positive number");
        }
        if(netConfig.getMaxEvents() < 1) {
            throw new FrameworkException(ExceptionType.NET, "MaxEvents must be a positive number");
        }
    }

    /**
     *   创建多路复用资源(wepoll, epoll or kqueue)
     */
    void create(Arena arena);

    /**
     *   创建socket资源
     */
    void socket(Arena arena);

    /**
     *   绑定指定端口
     */
    void bind(Arena arena);

    /**
     *   监听指定端口
     */
    void listen(Arena arena);

    /**
     *   注册多路复用操作
     */
    void ctl(Arena arena);

    /**
     *   服务端主循环
     */
    void loop(Arena arena);
}
