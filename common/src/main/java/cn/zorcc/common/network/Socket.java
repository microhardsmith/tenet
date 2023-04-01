package cn.zorcc.common.network;

public final class Socket {
    private final long l;
    private final int i;

    public Socket(int socket) {
        this.l = this.i = socket;
    }

    public Socket(long socket) {
        this.l = socket;
        this.i = Long.hashCode(socket); // for hashcode()
    }

    /**
     *   返回socket的int值
     */
    public int intValue() {
        return i;
    }

    /**
     *   返回socket的long值
     */
    public long longValue() {
        return l;
    }

    @Override
    public int hashCode() {
        return i;
    }

    @Override
    public String toString() {
        return String.valueOf(l);
    }
}
