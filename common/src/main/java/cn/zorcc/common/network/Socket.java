package cn.zorcc.common.network;

/**
 *  Socket abstraction, using long in windows, using int in Linux and MacOS
 */
public record Socket (
        long longValue,
        int intValue
){

    public Socket(int socket) {
        this(socket, socket);
    }

    public Socket(long socket) {
        this(socket, Long.hashCode(socket));
    }

    @Override
    public int hashCode() {
        return intValue;
    }

    @Override
    public String toString() {
        return String.valueOf(longValue);
    }
}
