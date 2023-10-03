package cn.zorcc.common.log;

import java.lang.foreign.MemorySegment;

public record LogEvent (
        LogEventType eventType,
        long timestamp,
        MemorySegment time,
        MemorySegment level,
        MemorySegment threadName,
        MemorySegment className,
        MemorySegment throwable,
        MemorySegment msg
){
    public static final LogEvent FLUSH_EVENT = of(LogEventType.Flush);
    public static final LogEvent SHUTDOWN_EVENT = of(LogEventType.Shutdown);
    private static LogEvent of(LogEventType type) {
        return new LogEvent(type, Long.MIN_VALUE, null, null, null, null, null, null);
    }

}
