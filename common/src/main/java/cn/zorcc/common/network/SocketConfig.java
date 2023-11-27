package cn.zorcc.common.network;

public final class SocketConfig {
    /**
     *  Whether allow reuse addr, only affect server socket, recommended to open
     */
    private boolean reuseAddr = true;
    /**
     *  Whether using tcp heartbeat, recommended to close, application should develop heartbeat mechanism in their protocol level
     */
    private boolean keepAlive = false;
    /**
     *  Whether closing Nagle algorithm, recommended to open, so every packet gets flushed immediately
     */
    private boolean tcpNoDelay = true;
    /**
     *  If this option is set to true, then master with IPv6 loc specified will only receive IPv6 connections
     *  this option is recommended to be closed, so a dual ipv4-ipv6 stack will co-exist when using IPv6 server socket
     */
    private boolean ipv6Only = false;

    public SocketConfig setReuseAddr(boolean reuseAddr) {
        this.reuseAddr = reuseAddr;
        return this;
    }

    public SocketConfig setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
        return this;
    }

    public SocketConfig setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
        return this;
    }

    public SocketConfig setIpv6Only(boolean ipv6Only) {
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
