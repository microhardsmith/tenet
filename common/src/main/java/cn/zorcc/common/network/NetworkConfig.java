package cn.zorcc.common.network;

import java.util.List;

/**
 *  Configuration of net
 */
public final class NetworkConfig {
    /**
     *  whether or not using tcp heartbeat for client connection, recommended to close, application should develop heartbeat mechanism in their protocol level
     */
    private boolean keepAlive = false;
    /**
     *  whether or not closing Nagle algorithm for client connection, recommended to open, so every packet gets flushed immediately
     */
    private boolean tcpNoDelay = true;
    /**
     *  graceful shutdown timeout in milliseconds
     */
    private long gracefulShutdownTimeout = 10000L;
    /**
     *  default connection timeout in milliseconds
     */
    private long defaultConnectionTimeout = 5000L;
    private List<MasterConfig> masters;
    private List<WorkerConfig> workers;

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
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

    public List<MasterConfig> getMasters() {
        return masters;
    }

    public void setMasters(List<MasterConfig> masters) {
        this.masters = masters;
    }

    public List<WorkerConfig> getWorkers() {
        return workers;
    }

    public void setWorkers(List<WorkerConfig> workers) {
        this.workers = workers;
    }
}
