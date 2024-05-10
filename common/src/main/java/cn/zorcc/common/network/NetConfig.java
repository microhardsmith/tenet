package cn.zorcc.common.network;

import cn.zorcc.common.Constants;

public final class NetConfig {
    /**
     *  TenetBinding already embedded a custom RpMalloc allocator for thread-local memory allocation
     *  It's recommended to use RpMalloc inside the Net, Poller, and Writer for better performance
     */
    private boolean enableRpMalloc = true;
    /**
     *  Max length for a single mux call
     *  For each mux, maxEvents * readBufferSize bytes of native memory were pre-allocated
     */
    private int maxEvents = 16;

    /**
     *  Max blocking time in milliseconds for a mux call
     */
    private int muxTimeout = 25;

    /**
     *  Max length for full-connection queue
     */
    private int backlog = 64;

    /**
     *  Net map size, normally 16 would be enough
     *  Ideal parameters should match the actual port being listened to
     */
    private int mapSize = 16;

    /**
     *  Net queue initialSize, normally 16 would be enough
     *  Ideal parameters should match the actual port being listened to
     */
    private int queueSize = 16;

    /**
     *  Graceful shutdown timeout in seconds
     */
    private int gracefulShutdownTimeout = 30;

    /**
     *  PollerCount determines how many poller thread will be created
     */
    private int pollerCount = 4;

    /**
     *  Poller taskQueue initial size
     */
    private int pollerQueueSize = 64;

    /**
     *  Max length for a single mux call
     *  For each mux, maxEvents * readBufferSize bytes of native memory were pre-allocated
     */
    private int pollerMaxEvents = 16;

    /**
     *  Max blocking time in milliseconds for a mux call
     */
    private int pollerMuxTimeout = 25;

    /**
     *  The read buffer maximum size for each poller instance
     */
    private int pollerBufferSize = 64 * Constants.KB;

    /**
     *  Poller map size, normally 256 would be enough
     *  if your application has to maintain thousands of connections at the same time, then you can increase this value appropriately
     */
    private int pollerMapSize = 256;

    /**
     *  WriterCount determines how many writer thread will be created
     */
    private int writerCount = 4;

    /**
     *  The write buffer initial size for each writer instance
     */
    private int writerBufferSize = 64 * Constants.KB;

    /**
     *  Writer map size, normally 256 would be enough
     *  if your application has to maintain thousands of connections at the same time, then you can increase this value appropriately
     */
    private int writerMapSize = 256;

    public boolean isEnableRpMalloc() {
        return enableRpMalloc;
    }

    public void setEnableRpMalloc(boolean enableRpMalloc) {
        this.enableRpMalloc = enableRpMalloc;
    }

    public int getMaxEvents() {
        return maxEvents;
    }

    public void setMaxEvents(int maxEvents) {
        this.maxEvents = maxEvents;
    }

    public int getMuxTimeout() {
        return muxTimeout;
    }

    public void setMuxTimeout(int muxTimeout) {
        this.muxTimeout = muxTimeout;
    }

    public int getBacklog() {
        return backlog;
    }

    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }

    public int getMapSize() {
        return mapSize;
    }

    public void setMapSize(int mapSize) {
        this.mapSize = mapSize;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public int getGracefulShutdownTimeout() {
        return gracefulShutdownTimeout;
    }

    public void setGracefulShutdownTimeout(int gracefulShutdownTimeout) {
        this.gracefulShutdownTimeout = gracefulShutdownTimeout;
    }

    public int getPollerCount() {
        return pollerCount;
    }

    public void setPollerCount(int pollerCount) {
        this.pollerCount = pollerCount;
    }

    public int getPollerQueueSize() {
        return pollerQueueSize;
    }

    public void setPollerQueueSize(int pollerQueueSize) {
        this.pollerQueueSize = pollerQueueSize;
    }

    public int getPollerMaxEvents() {
        return pollerMaxEvents;
    }

    public void setPollerMaxEvents(int pollerMaxEvents) {
        this.pollerMaxEvents = pollerMaxEvents;
    }

    public int getPollerMuxTimeout() {
        return pollerMuxTimeout;
    }

    public void setPollerMuxTimeout(int pollerMuxTimeout) {
        this.pollerMuxTimeout = pollerMuxTimeout;
    }

    public int getPollerBufferSize() {
        return pollerBufferSize;
    }

    public void setPollerBufferSize(int pollerBufferSize) {
        this.pollerBufferSize = pollerBufferSize;
    }

    public int getPollerMapSize() {
        return pollerMapSize;
    }

    public void setPollerMapSize(int pollerMapSize) {
        this.pollerMapSize = pollerMapSize;
    }

    public int getWriterCount() {
        return writerCount;
    }

    public void setWriterCount(int writerCount) {
        this.writerCount = writerCount;
    }

    public int getWriterBufferSize() {
        return writerBufferSize;
    }

    public void setWriterBufferSize(int writerBufferSize) {
        this.writerBufferSize = writerBufferSize;
    }

    public int getWriterMapSize() {
        return writerMapSize;
    }

    public void setWriterMapSize(int writerMapSize) {
        this.writerMapSize = writerMapSize;
    }
}
