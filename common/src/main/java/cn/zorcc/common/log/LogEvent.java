package cn.zorcc.common.log;

/**
 * log event entity
 * @param flush 是否为刷新事件
 * @param timestamp 日志时间戳
 * @param time 日志时间
 * @param level 日志等级
 * @param threadName 线程名
 * @param className 类名
 * @param throwable 异常
 * @param msg 日志消息体(已格式化)
 */
public record LogEvent (
        boolean flush,
        long timestamp,
        byte[] time,
        byte[] level,
        byte[] threadName,
        byte[] className,
        byte[] throwable,
        byte[] msg
){
    /**
     *  used for flush
     */
    public static final LogEvent flushEvent = new LogEvent(true, Long.MIN_VALUE, null, null, null, null, null, null);
    /**
     *  used for shutdown
     */
    public static final LogEvent shutdownEvent = new LogEvent(false, Long.MIN_VALUE, null, null, null, null, null, null);

}
