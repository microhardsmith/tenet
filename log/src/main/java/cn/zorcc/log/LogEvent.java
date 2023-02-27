package cn.zorcc.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.event.Event;
import cn.zorcc.common.util.NativeUtil;
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
    /**
     *  当前日志内存作用域
     */
    private final Arena arena = Arena.openShared();
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

    public void test() {
        NativeUtil.test(time, time.byteSize(), "time: ");
        NativeUtil.test(level, level.byteSize(), "level: ");
        NativeUtil.test(threadName, threadName.byteSize(), "threadName: ");
        NativeUtil.test(className, className.byteSize(), "className: ");
        NativeUtil.test(msg, msg.byteSize(), "msg: ");
    }

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

}
