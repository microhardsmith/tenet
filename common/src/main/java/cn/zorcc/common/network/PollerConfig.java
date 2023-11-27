package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.util.NativeUtil;

public final class PollerConfig {
    /**
     *  PollerCount determines how many poller thread will be created
     */
    private int pollerCount = Math.max(NativeUtil.getCpuCores() >> 1, 4);
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
     *  The read buffer maximum size for each poller instance
     */
    private int readBufferSize = 64 * Constants.KB;
    /**
     *  Poller map size, normally 1024 would be enough
     *  if your application has to maintain thousands of connections at the same time, then you can increase this value appropriately
     */
    private int mapSize = Constants.KB;

    public int getPollerCount() {
        return pollerCount;
    }

    public void setPollerCount(int pollerCount) {
        this.pollerCount = pollerCount;
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

    public int getReadBufferSize() {
        return readBufferSize;
    }

    public void setReadBufferSize(int readBufferSize) {
        this.readBufferSize = readBufferSize;
    }

    public int getMapSize() {
        return mapSize;
    }

    public void setMapSize(int mapSize) {
        this.mapSize = mapSize;
    }
}
