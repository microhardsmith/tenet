package cn.zorcc.common.log;

public final class LogConfig {
    private String level = "INFO";
    private long flushInterval = 200L;
    private String logFormat = "{time} {level} [{threadName}] {className} - {msg}";
    private ConsoleLogConfig console = new ConsoleLogConfig();
    private FileLogConfig file;
    private SqliteLogConfig sqlite;

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public long getFlushInterval() {
        return flushInterval;
    }

    public void setFlushInterval(long flushInterval) {
        this.flushInterval = flushInterval;
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
