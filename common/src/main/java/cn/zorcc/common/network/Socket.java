package cn.zorcc.common.network;

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
        this(Math.toIntExact(socket), socket);
    }

    @Override
    public int hashCode() {
        return intValue;
    }
}
