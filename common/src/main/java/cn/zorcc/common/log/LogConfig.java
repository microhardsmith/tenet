package cn.zorcc.common.log;

import cn.zorcc.common.Constants;

public final class LogConfig {
    /**
     *   Global buffer size allocated for the logging thread
     */
    private long bufferSize = Constants.MB;
    /**
     *   Global minimal log level
     */
    private String level = "DEBUG";
    /**
     *   Default global flush interval in milliseconds
     */
    private int flushInterval = 200;
    /**
     *   Whether using TimeResolver SPI or not, if not found, fall back to use timeFormat
     */
    private boolean usingTimeResolver = true;
    /**
     *   TimeFormat string, note that TimeResolver with hand-crafted mechanism should be preferred with regard to performance
     */
    private String timeFormat = Constants.DEFAULT_TIME_FORMAT;
    /**
     *   Complete log format
     */
    private String logFormat = "{time} {level} [{threadName}] {className} - {msg}";
    /**
     *   Whether using console log output
     */
    private ConsoleLogConfig console = new ConsoleLogConfig();
    /**
     *   Whether using file log output
     */
    private FileLogConfig file;
    /**
     *   Whether using sqlite log output
     */
    private SqliteLogConfig sqlite;

    public long getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(long bufferSize) {
        this.bufferSize = bufferSize;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public int getFlushInterval() {
        return flushInterval;
    }

    public void setFlushInterval(int flushInterval) {
        this.flushInterval = flushInterval;
    }

    public boolean isUsingTimeResolver() {
        return usingTimeResolver;
    }

    public void setUsingTimeResolver(boolean usingTimeResolver) {
        this.usingTimeResolver = usingTimeResolver;
    }

    public String getTimeFormat() {
        return timeFormat;
    }

    public void setTimeFormat(String timeFormat) {
        this.timeFormat = timeFormat;
    }

    public String getLogFormat() {
        return logFormat;
    }

    public void setLogFormat(String logFormat) {
        this.logFormat = logFormat;
    }

    public ConsoleLogConfig getConsole() {
        return console;
    }

    public void setConsole(ConsoleLogConfig console) {
        this.console = console;
    }

    public FileLogConfig getFile() {
        return file;
    }

    public void setFile(FileLogConfig file) {
        this.file = file;
    }

    public SqliteLogConfig getSqlite() {
        return sqlite;
    }

    public void setSqlite(SqliteLogConfig sqlite) {
        this.sqlite = sqlite;
    }
}
