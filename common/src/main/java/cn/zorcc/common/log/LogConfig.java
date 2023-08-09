package cn.zorcc.common.log;

import cn.zorcc.common.Constants;

/**
 * 日志配置文件,默认从当前工程resources目录下寻找log-config.json读取
 */
public class LogConfig {
    /**
     * 日志默认允许输出的最低级别,与Slf4j规定的日志级别吻合
     */
    private int level = Constants.DEBUG;
    /**
     * 日志时间格式
     */
    private String timeFormat = Constants.TIME_FORMAT;
    /**
     * 日志时间格式解析类名,可自定义时间字段的解析方式
     */
    private String timeResolver = Constants.TIME_RESOLVER;
    /**
     *  flush间隔,单位毫秒
     */
    private long flushInterval = 200L;
    /**
     *  初始日志行内存大小
     */
    private long bufferSize = 4 * Constants.KB;
    /**
     *  日志格式字符串
     */
    private String logFormat = "%time% %level% [%threadName%] %className% - %msg%";
    /**
     * 是否将日志输出到控制台,默认为true,基于性能考虑可以考虑关闭该项(输出至文件会比输出至控制台快许多)
     */
    private boolean usingConsole = true;
    /**
     *  日志等级Console输出预留长度
     */
    private int levelLen = 5;
    /**
     *  线程名Console输出预留长度
     */
    private int threadNameLen = 10;
    /**
     *  类名Console输出预留长度
     */
    private int classNameLen = 40;
    /**
     *  打印日志时间的颜色
     */
    private String timeColor = Constants.BLUE;
    /**
     *  打印日志线程名的颜色
     */
    private String threadNameColor = Constants.MAGENTA;
    /**
     *  打印日志类名的颜色
     */
    private String classNameColor = Constants.CYAN;
    /**
     *  打印日志消息的颜色
     */
    private String msgColor = Constants.DEFAULT;
    /**
     * 是否将日志输出到文件,默认为false
     */
    private boolean usingFile = false;
    /**
     *  日志文件缓冲区大小,单位byte
     */
    private int fileBuffer = 64 * Constants.KB;
    /**
     * 日志文件目录路径,如果未指定则使用项目目录下新建log目录存储
     */
    private String logFileDir = Constants.EMPTY_STRING;
    /**
     * 日志文件名称前缀,默认为app
     */
    private String logFileName = Constants.DEFAULT_LOG_FILE_NAME;
    /**
     * 日志文件大小限制设置,超过将重命名旧日志文件为当前时间,将新日志写入新文件中,单位字节
     * 设置该值小于等于0则使用默认 1GB 的大小限制
     */
    private int maxFileSize = Constants.GB;
    /**
     *  日志记录时间间隔,默认每隔一天新建日志文件进行写入,单位毫秒
     *  设置该值小于等于0表示不限制日志时间
     */
    private int maxRecordingTime = Constants.DAY;
    /**
     *  是否启用滚动更新
     */
    private boolean enableRolling = false;
    /**
     *  允许产生的日志文件数,旧文件会以 logFileName-%d.log进行命名
     */
    private int rollingFileCount = 1;
    /**
     * 是否将日志传输至Metrics保存,默认为false
     */
    private boolean usingMetrics = false;

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getTimeFormat() {
        return timeFormat;
    }

    public void setTimeFormat(String timeFormat) {
        this.timeFormat = timeFormat;
    }

    public String getTimeResolver() {
        return timeResolver;
    }

    public void setTimeResolver(String timeResolver) {
        this.timeResolver = timeResolver;
    }

    public long getFlushInterval() {
        return flushInterval;
    }

    public void setFlushInterval(long flushInterval) {
        this.flushInterval = flushInterval;
    }

    public long getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(long bufferSize) {
        this.bufferSize = bufferSize;
    }

    public String getLogFormat() {
        return logFormat;
    }

    public void setLogFormat(String logFormat) {
        this.logFormat = logFormat;
    }

    public boolean isUsingConsole() {
        return usingConsole;
    }

    public void setUsingConsole(boolean usingConsole) {
        this.usingConsole = usingConsole;
    }

    public int getLevelLen() {
        return levelLen;
    }

    public void setLevelLen(int levelLen) {
        this.levelLen = levelLen;
    }

    public int getThreadNameLen() {
        return threadNameLen;
    }

    public void setThreadNameLen(int threadNameLen) {
        this.threadNameLen = threadNameLen;
    }

    public int getClassNameLen() {
        return classNameLen;
    }

    public void setClassNameLen(int classNameLen) {
        this.classNameLen = classNameLen;
    }

    public String getTimeColor() {
        return timeColor;
    }

    public void setTimeColor(String timeColor) {
        this.timeColor = timeColor;
    }

    public String getThreadNameColor() {
        return threadNameColor;
    }

    public void setThreadNameColor(String threadNameColor) {
        this.threadNameColor = threadNameColor;
    }

    public String getClassNameColor() {
        return classNameColor;
    }

    public void setClassNameColor(String classNameColor) {
        this.classNameColor = classNameColor;
    }

    public String getMsgColor() {
        return msgColor;
    }

    public void setMsgColor(String msgColor) {
        this.msgColor = msgColor;
    }

    public boolean isUsingFile() {
        return usingFile;
    }

    public void setUsingFile(boolean usingFile) {
        this.usingFile = usingFile;
    }

    public int getFileBuffer() {
        return fileBuffer;
    }

    public void setFileBuffer(int fileBuffer) {
        this.fileBuffer = fileBuffer;
    }

    public String getLogFileDir() {
        return logFileDir;
    }

    public void setLogFileDir(String logFileDir) {
        this.logFileDir = logFileDir;
    }

    public String getLogFileName() {
        return logFileName;
    }

    public void setLogFileName(String logFileName) {
        this.logFileName = logFileName;
    }

    public int getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(int maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public int getMaxRecordingTime() {
        return maxRecordingTime;
    }

    public void setMaxRecordingTime(int maxRecordingTime) {
        this.maxRecordingTime = maxRecordingTime;
    }

    public boolean isEnableRolling() {
        return enableRolling;
    }

    public void setEnableRolling(boolean enableRolling) {
        this.enableRolling = enableRolling;
    }

    public int getRollingFileCount() {
        return rollingFileCount;
    }

    public void setRollingFileCount(int rollingFileCount) {
        this.rollingFileCount = rollingFileCount;
    }

    public boolean isUsingMetrics() {
        return usingMetrics;
    }

    public void setUsingMetrics(boolean usingMetrics) {
        this.usingMetrics = usingMetrics;
    }
}
