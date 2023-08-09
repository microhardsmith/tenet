package cn.zorcc.common.network;

import cn.zorcc.common.Constants;

/**
 *  Configuration of net
 */
public class NetworkConfig {
    /**
     *  read buffer maximum size for a read operation
     */
    private long readBufferSize = 64 * Constants.KB;
    /**
     *  write buffer initial size for a write operation
     */
    private long writeBufferSize = 64 * Constants.KB;
    /**
     *  socket map initial size, will automatically expand, could be changed according to specific circumstances
     */
    private int mapSize = 4 * Constants.KB;
    /**
     *   服务端是否开启TLS加密,默认不开启
     */
    private Boolean enableSsl = Boolean.FALSE;
    /**
     *   服务端证书公钥路径,必须为绝对路径
     */
    private String publicKeyFile = Constants.EMPTY_STRING;
    /**
     *   服务端证书私钥路径,必须为绝对路径
     */
    private String privateKeyFile = Constants.EMPTY_STRING;
    /**
     *  是否可复用端口,默认为true,该选项只会影响服务端socket在bind上的行为,对客户端socket设置不会产生影响
     */
    private int reuseAddr = Constants.ONE;
    /**
     *  是否周期性发送保活报文以维持连接,推荐关闭,应在应用层手动实现心跳
     */
    private int keepAlive = Constants.ZERO;
    /**
     *  是否关闭Nagle算法,推荐关闭,这样每个小包都能得到及时发送
     */
    private int tcpNoDelay = Constants.ONE;
    /**
     *  优雅停机超时时间,单位豪秒
     */
    private long gracefulShutdownTimeout = 10000L;
    /**
     *  默认建立连接超时时间,单位毫秒
     */
    private long defaultConnectionTimeout = 5000L;

    public long getReadBufferSize() {
        return readBufferSize;
    }

    public void setReadBufferSize(long readBufferSize) {
        this.readBufferSize = readBufferSize;
    }

    public long getWriteBufferSize() {
        return writeBufferSize;
    }

    public void setWriteBufferSize(long writeBufferSize) {
        this.writeBufferSize = writeBufferSize;
    }

    public int getMapSize() {
        return mapSize;
    }

    public void setMapSize(int mapSize) {
        this.mapSize = mapSize;
    }

    public Boolean getEnableSsl() {
        return enableSsl;
    }

    public void setEnableSsl(Boolean enableSsl) {
        this.enableSsl = enableSsl;
    }

    public String getPublicKeyFile() {
        return publicKeyFile;
    }

    public void setPublicKeyFile(String publicKeyFile) {
        this.publicKeyFile = publicKeyFile;
    }

    public String getPrivateKeyFile() {
        return privateKeyFile;
    }

    public void setPrivateKeyFile(String privateKeyFile) {
        this.privateKeyFile = privateKeyFile;
    }

    public int getReuseAddr() {
        return reuseAddr;
    }

    public void setReuseAddr(int reuseAddr) {
        this.reuseAddr = reuseAddr;
    }

    public int getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(int keepAlive) {
        this.keepAlive = keepAlive;
    }

    public int getTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(int tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    public long getGracefulShutdownTimeout() {
        return gracefulShutdownTimeout;
    }

    public void setGracefulShutdownTimeout(long gracefulShutdownTimeout) {
        this.gracefulShutdownTimeout = gracefulShutdownTimeout;
    }

    public long getDefaultConnectionTimeout() {
        return defaultConnectionTimeout;
    }

    public void setDefaultConnectionTimeout(long defaultConnectionTimeout) {
        this.defaultConnectionTimeout = defaultConnectionTimeout;
    }
}
