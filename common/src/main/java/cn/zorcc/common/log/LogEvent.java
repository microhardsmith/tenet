package cn.zorcc.common.log;

/**
 * Log event entity
 * @param flush whether it's a flush event
 * @param timestamp nanosecond timestamp
 * @param time
 * @param level
 * @param threadName
 * @param className
 * @param throwable
 * @param msg actual log msg which has been formatted
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
