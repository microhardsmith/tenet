package cn.zorcc.common.pojo;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

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
        if(!checkIp(ip)) {
            throw new FrameworkException(ExceptionType.NETWORK, "IpAddress is not valid : " + ip);
        }
        if(!checkPort(port)) {
            throw new FrameworkException(ExceptionType.NETWORK, "Port is not valid : " + port);
        }
    }


    /**
     *   Check ipv4 address string format
     */
    public static boolean checkIp(String ip) {
        if (ip.isBlank()) {
            return false;
        }
        String[] strings = ip.split("\\.");
        for (String s : strings) {
            try{
                int value = Integer.parseInt(s);
                if(value < 0 || value > 255) {
                    return false;
                }
            }catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    /**
     *   Check network port range
     */
    public static boolean checkPort(int port) {
        return port >= 0 && port <= 65535;
    }

    @Override
    public String toString() {
        return "[" + ip + ":" + port + "]";
    }
}
