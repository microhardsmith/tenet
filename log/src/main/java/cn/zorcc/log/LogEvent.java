package cn.zorcc.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.event.Event;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.event.Level;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 日志事件
 */
@Getter
@Setter
public class LogEvent extends Event {
    public static final LogEvent flushEvent = new LogEvent();
    /**
     *  是否为刷新事件
     */
    private final boolean flush;
    /**
     *  是否可重用
     */
    private final boolean reusable;
    /**
     * 日志时间记录
     */
    private final LogTime logTime;
    /**
     * 可不断复用的StringBuilder缓存
     */
    private final StringBuilder builder;
    /**
     *  日志pipeline并发控制
     */
    private final AtomicInteger counter;
    /**
     *  线程名
     */
    private String threadName;
    /**
     * 日志等级
     */
    private Level level;
    /**
     * 日志输出类名
     */
    private String className;
    /**
     * 原始日志消息
     */
    private String originMsg;
    /**
     * 日志消息参数
     */
    private Object[] args;
    /**
     * 日志异常参数
     */
    private Throwable throwable;
    /**
     * 经过格式化后的日志消息
     */
    private String msg;
    /**
     * 经过格式化后的日志行
     */
    private String line;

    private LogEvent() {
        this.flush = true;
        this.reusable = false;
        this.logTime = null;
        this.builder = null;
        this.counter = null;
    }

    public LogEvent(boolean reusable) {
        this.flush = false;
        this.reusable = reusable;
        this.logTime = new LogTime();
        this.builder = new StringBuilder(Constants.DEFAULT_STRING_BUILDER_SIZE);
        this.counter = reusable ? new AtomicInteger(Constants.ZERO) : null;
        this.threadName = Constants.EMPTY_STRING;
        this.level = Level.DEBUG;
        this.className = Constants.EMPTY_STRING;
        this.originMsg = Constants.EMPTY_STRING;
        this.msg = Constants.EMPTY_STRING;
    }

    /**
     *  重置当前日志事件
     */
    public void reset() {
        this.builder.setLength(Constants.ZERO);
        if(reusable) {
            this.counter.set(Constants.ZERO);
        }
        this.threadName = null;
        this.level = null;
        this.className = null;
        this.originMsg = null;
        this.msg = null;
        this.args = null;
        this.throwable = null;
        this.line = null;
    }


}
