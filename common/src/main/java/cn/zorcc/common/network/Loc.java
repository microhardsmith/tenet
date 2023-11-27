package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

/**
 *  Ipv4 or Ipv6 address, Note that port is a number between 0 and 65535, in C normally represented as u_short, but in java there is no unsigned number, so here we did some transformation
 */
public record Loc (
        IpType ipType,
        String ip,
        int port
) {
    private static final int PORT_MAX = 65535;

    /**
     *   Create a default local address
     */
    public Loc(IpType ipType, int port) {
        this(ipType, Constants.EMPTY_STRING, port);
    }

    /**
     *   Convert an int port to an unsigned short type, this method force retain the lower 16bits, the result could be negative
     */
    public short shortPort() {
        if(port < 0 || port > PORT_MAX) {
            throw new FrameworkException(ExceptionType.NETWORK, "Port number overflow");
        }
        return (short) port;
    }

    @Override
    public String toString() {
        return STR."[\{ip == null || ip.isBlank() ? "localhost" : ip}:\{port}]";
    }
}
