package cn.zorcc.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.SegmentBuilder;
import cn.zorcc.common.event.Event;
import lombok.Getter;
import lombok.Setter;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
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
    /**
     *  当前日志内存作用域
     */
    private final Arena arena = Arena.openShared();
    /**
     *  是否为刷新事件
     */
    private final boolean flush;
    /**
     *  日志计数器,当日志事件被完全使用后进行释放
     */
    private final AtomicInteger counter = new AtomicInteger(Constants.ZERO);
    /**
     *  日志行直接内存
     */
    private final SegmentBuilder builder;
    /**
     *  日志时间戳
     */
    private long timestamp;
    /**
     *  日志时间
     */
    private MemorySegment time;
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
        this.flush = true;
        this.builder = null;
    }

    /**
     *  用于构建普通日志事件
     */
    public LogEvent(int size) {
        this.flush = false;
        this.builder = new SegmentBuilder(arena, size);
    }

    /**
     *  尝试释放LogEvent占用的内存资源
     */
    public void tryRelease() {
        if(counter.incrementAndGet() == Logger.pipelineSize) {
            arena.close();
        }
    }

}
