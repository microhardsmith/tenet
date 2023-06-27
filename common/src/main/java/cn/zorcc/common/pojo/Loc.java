package cn.zorcc.common.pojo;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ConfigUtil;

/**
 * 标识一台特定机器在网络中所处的位置
 * @param ip 机器网卡ipv4地址
 * @param port 机器暴露给外部的端口号
 */
public record Loc (
        String ip,
        short port
) {

    public static final Loc DEFAULT = new Loc("0.0.0.0", (short) 8001);

    /**
     *   Validate current loc configuration
     */
    public void validate() {
        if(!ConfigUtil.checkIp(ip)) {
            throw new FrameworkException(ExceptionType.NETWORK, "IpAddress is not valid : " + ip);
        }
        if(!ConfigUtil.checkPort(port)) {
            throw new FrameworkException(ExceptionType.NETWORK, "Port is not valid : " + port);
        }
    }

    @Override
    public String toString() {
        return "[" + ip + ":" + port + "]";
    }
}
