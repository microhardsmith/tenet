package cn.zorcc.common.network;

import cn.zorcc.common.util.NativeUtil;

/**
 *  Socket abstraction, using long in windows, using int in Linux and macOS
 */
public record Socket(
        int intValue,
        long longValue
) {
    public Socket(int socket) {
        this(socket, socket);
    }

    public Socket(long socket) {
        this(NativeUtil.castInt(socket), socket);
    }
}
