package cn.zorcc.common.network;

public final class NetConfig {
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
     *  Net map size, normally 64 would be enough
     *  Ideal parameters should match the actual port being listened to
     */
    private int mapSize = 64;

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
}
