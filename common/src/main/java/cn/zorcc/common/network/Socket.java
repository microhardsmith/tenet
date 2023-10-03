package cn.zorcc.common.network;

import cn.zorcc.common.util.NativeUtil;

/**
 *  Socket abstraction, using long in windows, using int in Linux and MacOS
 *  Note this implementation is rather dangerous because Socket are used as HashMap's key internally, but it's also changeable
 *  Developers should take caution that the Socket used as Map's key types should never be modified by update() method
 */
public final class Socket {
    private int intValue;
    private long longValue;

    private Socket(int intValue, long longValue) {
        this.intValue = intValue;
        this.longValue = longValue;
    }

    public Socket(int socket) {
        this(socket, socket);
    }

    public Socket(long socket) {
        this(NativeUtil.castInt(socket), socket);
    }

    public void update(int intValue) {
        this.intValue = intValue;
        this.longValue = intValue;
    }

    public void update(long longValue) {
        this.intValue = NativeUtil.castInt(longValue);
        this.longValue = longValue;
    }

    public int intValue() {
        return intValue;
    }

    public long longValue() {
        return longValue;
    }

    @Override
    public int hashCode() {
        return intValue;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Socket s && s.intValue() == intValue();
    }

    @Override
    public String toString() {
        return String.valueOf(longValue);
    }
}
