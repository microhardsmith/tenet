package cn.zorcc.common.log;

import cn.zorcc.common.Constants;

public final class SqliteLogConfig {
    private long buffer = 64 * Constants.KB;
    private String dir = Constants.EMPTY_STRING;
    private int flushThreshold = 128;
    private long maxRowCount = Constants.MB;
    private long maxRecordingTime = Constants.DAY;

    public long getBuffer() {
        return buffer;
    }

    public void setBuffer(long buffer) {
        this.buffer = buffer;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public int getFlushThreshold() {
        return flushThreshold;
    }

    public void setFlushThreshold(int flushThreshold) {
        this.flushThreshold = flushThreshold;
    }

    public long getMaxRowCount() {
        return maxRowCount;
    }

    public void setMaxRowCount(long maxRowCount) {
        this.maxRowCount = maxRowCount;
    }

    public long getMaxRecordingTime() {
        return maxRecordingTime;
    }

    public void setMaxRecordingTime(long maxRecordingTime) {
        this.maxRecordingTime = maxRecordingTime;
    }
}
