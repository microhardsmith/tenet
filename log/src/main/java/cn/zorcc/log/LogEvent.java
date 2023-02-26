package cn.zorcc.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.event.Event;
import lombok.Getter;
import lombok.Setter;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentScope;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 日志事件
 */
@Getter
@Setter
public class LogEvent extends Event {
    /**
     *  用于刷新事件
     */
    public static final LogEvent flushEvent = new LogEvent();
    private final Arena arena = Arena.openShared();
    /**
     *  日志时间
     */
    private final LogTime logTime;
    /**
     *  是否为刷新事件
     */
    private final boolean flush;
    /**
     *  日志pipeline并发控制
     */
    private final AtomicInteger counter = new AtomicInteger(0);
    /**
     *  日志行直接内存
     */
    private final SegmentBuilder builder;
    /**
     *  日志时间戳
     */
    private long timestamp = -1L;
    /**
     * 日志等级
     */
    private MemorySegment level;
    /**
     *  线程名
     */
    private MemorySegment threadName;
    /**
     * 日志输出类名
     */
    private MemorySegment className;
    /**
     * 日志异常,注意打印异常的性能不如普通日志
     */
    private MemorySegment throwable;
    /**
     *  日志消息体(已格式化)
     */
    private MemorySegment msg;


    /**
     *  用于构建flush event
     */
    private LogEvent() {
        this.logTime = null;
        this.flush = true;
        this.builder = null;
    }

    /**
     *  用于构建可重用的日志事件,在使用完成后释放回队列
     */
    public LogEvent(int size, LocalDateTime time) {
        this.flush = false;
        this.logTime = new LogTime(time);
        this.builder = new SegmentBuilder(arena, size);
    }

    /**
     *  重置当前日志事件
     */
    public void reset() {

    }

}
