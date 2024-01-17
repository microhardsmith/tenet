package cn.zorcc.common.log;

import cn.zorcc.common.Constants;

public final class SqliteLogConfig {
    /**
     *   Dir path to store the sqlite database file
     */
    private String dir = Constants.EMPTY_STRING;
    /**
     *   Max flush threshold
     */
    private int flushThreshold = 128;
    /**
     *   Max existing row count before switching database file
     */
    private long maxRowCount = Constants.MB;
    /**
     *   Max existing time before switching database file
     */
    private long maxRecordingTime = Constants.DAY;

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
