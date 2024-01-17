package cn.zorcc.common.log;

import cn.zorcc.common.Constants;

public final class FileLogConfig {
    /**
     *   Dir path to store the log file
     */
    private String dir = Constants.EMPTY_STRING;
    /**
     *   Max flush threshold
     */
    private int flushThreshold = 128;
    /**
     *   Max file size before switching database file
     */
    private long maxFileSize = 256 * Constants.MB;
    /**
     *   Max existing time before switching recording file
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

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public long getMaxRecordingTime() {
        return maxRecordingTime;
    }

    public void setMaxRecordingTime(long maxRecordingTime) {
        this.maxRecordingTime = maxRecordingTime;
    }
}
