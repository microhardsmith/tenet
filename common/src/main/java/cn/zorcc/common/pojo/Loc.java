package cn.zorcc.common.pojo;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ConfigUtil;

/**
 *  Ipv4 address, Note that port is a number between 0 and 65535, in C normally represented as u_short, so here we did some transformation
 */
public record Loc (
        String ip,
        int port
) {

    /**
     *   Convert a int port to a unsigned short type
     */
    public static short toShortPort(int port) {
        if(port < 0 || port > 65535) {
            throw new FrameworkException(ExceptionType.NETWORK, "Port overflow");
        }
        // force retain the lower 16bits, the answer could be negative
        return (short) port;
    }

    /**
     *   Convert a unsigned short type to a int
     */
    public static int toIntPort(short port) {
        return 0xFFFF & port;
    }

    public short shortPort() {
        return toShortPort(port);
    }

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
