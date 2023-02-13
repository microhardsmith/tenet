package cn.zorcc.common.net;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ConfigUtil;

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
}
