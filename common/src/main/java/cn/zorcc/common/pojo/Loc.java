package cn.zorcc.common.pojo;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

/**
 *  Ipv4 address, Note that port is a number between 0 and 65535, in C normally represented as u_short, so here we did some transformation
 */
public record Loc (
        IpType ipType,
        String ip,
        int port
) {
    private static final int PORT_MAX = 65535;
    /**
     *   Convert a int port to a unsigned short type, this method force retain the lower 16bits, the result could be negative
     */
    public static short toShortPort(int port) {
        if(port < Constants.ZERO || port > PORT_MAX) {
            throw new FrameworkException(ExceptionType.NETWORK, "Port overflow");
        }
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

    @Override
    public String toString() {
        return STR."[\{ip}:\{port}]";
    }
}
