package cn.zorcc.common.log;

import cn.zorcc.common.Constants;

public final class ConsoleLogConfig {
    /**
     *  Flush threshold for console output
     */
    private int flushThreshold = 5;
    /**
     *  Reserved length for log level output
     */
    private int levelLen = 5;
    /**
     *  Reserved length for log thread-name output
     */
    private int threadNameLen = 10;
    /**
     *  Reserved length for log class-name output
     */
    private int classNameLen = 40;
    /**
     *  Default size for containing buffer for stdout
     */
    private int outBuffer = 4 * Constants.KB;
    /**
     *  Default size of containing buffer for stderr
     */
    private int errBuffer = 12 * Constants.KB;
    /**
     *  Console color for log time output
     */
    private String timeColor = Constants.BLUE;
    /**
     *  Console color for log time output
     */
    private String threadNameColor = Constants.MAGENTA;
    /**
     *  Console color for log time output
     */
    private String classNameColor = Constants.CYAN;
    /**
     *  Console color for log time output
     */
    private String msgColor = Constants.DEFAULT;

    public int getFlushThreshold() {
        return flushThreshold;
    }

    public void setFlushThreshold(int flushThreshold) {
        this.flushThreshold = flushThreshold;
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

    public int getOutBuffer() {
        return outBuffer;
    }

    public void setOutBuffer(int outBuffer) {
        this.outBuffer = outBuffer;
    }

    public int getErrBuffer() {
        return errBuffer;
    }

    public void setErrBuffer(int errBuffer) {
        this.errBuffer = errBuffer;
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
}
