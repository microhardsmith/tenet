package cn.zorcc.common.network;

public final class SocketOptions {
    /**
     *  whether or not allow reuse addr, only affect server socket, recommended to open
     */
    private boolean reuseAddr = true;
    /**
     *  whether or not using tcp heartbeat, recommended to close, application should develop heartbeat mechanism in their protocol level
     */
    private boolean keepAlive = false;
    /**
     *  whether or not closing Nagle algorithm, recommended to open, so every packet gets flushed immediately
     */
    private boolean tcpNoDelay = true;
    /**
     *  if this option is set to true, then master with ipv6 loc specified will only receive ipv6 connections
     *  this option is recommended to close, so a dual ipv4-ipv6 stack will co-exist
     */
    private boolean ipv6Only = false;

    public SocketOptions setReuseAddr(boolean reuseAddr) {
        this.reuseAddr = reuseAddr;
        return this;
    }

    public SocketOptions setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
        return this;
    }

    public SocketOptions setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
        return this;
    }

    public SocketOptions setIpv6Only(boolean ipv6Only) {
        this.ipv6Only = ipv6Only;
        return this;
    }

    public boolean isReuseAddr() {
        return reuseAddr;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public boolean isIpv6Only() {
        return ipv6Only;
    }
}
