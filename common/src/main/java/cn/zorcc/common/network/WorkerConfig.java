package cn.zorcc.common.network;

import cn.zorcc.common.Constants;

public final class WorkerConfig {
    /**
     * Max length for a single mux call
     * Note that for each mux, maxEvents * readBufferSize native memory were pre-allocated
     */
    private int maxEvents = 16;
    /**
     * Max blocking time in milliseconds for a mux call
     */
    private int muxTimeout = 25;
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
}
