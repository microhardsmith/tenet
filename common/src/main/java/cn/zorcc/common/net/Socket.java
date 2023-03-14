package cn.zorcc.common.net;

public final class Socket {
    private final boolean w;
    private final long l;
    private final int i;

    public Socket(int socket) {
        this.l = socket;
        this.i = socket;
        this.w = false;
    }

    public Socket(long socket) {
        this.l = socket;
        this.i = (int) (l % Integer.MAX_VALUE);
        this.w = true;
    }

    /**
     *   是否使用windows系统
     */
    public boolean usingWindows() {
        return w;
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
}
