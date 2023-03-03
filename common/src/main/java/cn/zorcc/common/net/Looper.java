package cn.zorcc.common.net;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ConfigUtil;

import java.lang.foreign.Arena;

/**
 *  selector thread
 */
public interface Looper {

    /**
     *  校验NetConfig字段
     */
    default void validate(NetConfig netConfig) {
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

    void create(Arena arena);

    void socket(Arena arena);

    void bind(Arena arena);

    void listen(Arena arena);

    void ctl(Arena arena);

    void loop(Arena arena);

    /**
     *   释放当前looper占用资源
     */
    void release();
}
