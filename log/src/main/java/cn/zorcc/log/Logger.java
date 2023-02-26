package cn.zorcc.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Context;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.event.EventHandler;
import cn.zorcc.common.event.EventPipeline;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ConfigUtil;
import cn.zorcc.common.util.NativeUtil;
import cn.zorcc.common.util.ThreadUtil;
import org.jctools.queues.atomic.MpmcAtomicArrayQueue;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.LegacyAbstractLogger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 负责打印tenet项目中使用的日志
 */
public class Logger extends LegacyAbstractLogger {
    private static final Map<Level, MemorySegment> levelMap = Map.of(
            Level.DEBUG, Constants.DEBUG_SEGMENT,
            Level.TRACE, Constants.TRACE_SEGMENT,
            Level.INFO, Constants.INFO_SEGMENT,
            Level.WARN, Constants.WARN_SEGMENT,
            Level.ERROR, Constants.ERROR_SEGMENT
    );
    private static final int level;
    private static final MpmcAtomicArrayQueue<LogEvent> eventQueue;
    private static final EventPipeline<LogEvent> pipeline;
    private static final int pipelineSize;


    static {
        LogConfig logConfig = ConfigUtil.loadJsonConfig(Constants.DEFAULT_LOG_CONFIG_NAME, LogConfig.class);
        int bufferSize = Math.max(logConfig.getBufferSize(), Constants.KB);
        switch (logConfig.getLevel()) {
            case Constants.TRACE -> level = Constants.TRACE;
            case Constants.INFO -> level = Constants.INFO;
            case Constants.WARN -> level = Constants.WARN;
            case Constants.ERROR -> level = Constants.ERROR;
            case Constants.DEBUG -> level = Constants.DEBUG;
            default -> throw new FrameworkException(ExceptionType.LOG, "Unsupported Log default level");
        }
        // since we are using virtual threads, the concurrency should be well controlled, cpu-cores should be an ideal parameter
        int queueSize = Math.max(logConfig.getQueueSize(), NativeUtil.getCpuCores() + 1);
        eventQueue = new MpmcAtomicArrayQueue<>(queueSize);
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < queueSize; i++) {
            LogEvent logEvent = new LogEvent(bufferSize, now);
            if (!eventQueue.offer(logEvent)) {
                throw new FrameworkException(ExceptionType.LOG, "BlockingQueue state corrupted");
            }
        }
        List<EventHandler<LogEvent>> eventHandlers = new ArrayList<>();
        if(logConfig.isUsingConsole()) {
            eventHandlers.add(new ConsoleLogEventHandler(logConfig));
        }
        if(logConfig.isUsingFile()) {
            eventHandlers.add(new FileLogEventHandler(logConfig));
        }
        if(logConfig.isUsingMetrics()) {
            eventHandlers.add(new MetricsLogEventHandler(logConfig));
        }
        pipeline = new EventPipeline<>(eventHandlers);
        pipelineSize = eventHandlers.size();
        Context.registerPipeline(LogEvent.class, pipeline);
    }

    /**
     *  获取日志事件pipeline大小
     */
    public static int pipelineSize() {
        return pipelineSize;
    }

    /**
     * 重用日志对象,将LogEvent重新放入MPMC队列中
     */
    public static void reuse(LogEvent logEvent) {
        logEvent.reset();
        if (!eventQueue.offer(logEvent)) {
            throw new FrameworkException(ExceptionType.LOG, "MPMCQueue state corrupted");
        }
    }

    public Logger(String name) {
        this.name = name;
    }

    @Override
    protected String getFullyQualifiedCallerName() {
        return null;
    }

    /**
     *  日志输出，日志分为五个部分： 时间 等级 线程名 类名 日志消息
     */
    @Override
    protected void handleNormalizedLoggingCall(Level level, Marker marker, String s, Object[] objects, Throwable throwable) {
        LogEvent logEvent = null;
        while (logEvent == null) {
            logEvent = eventQueue.poll();
        }

        final LogTime logTime = logEvent.getLogTime();
        logTime.refresh();
        logEvent.setTimestamp(logTime.timestamp());

        logEvent.setLevel(levelMap.get(level));
        logEvent.setThreadName(MemorySegment.ofArray(ThreadUtil.threadName().getBytes(StandardCharsets.UTF_8)));
        logEvent.setClassName(MemorySegment.ofArray(getName().getBytes(StandardCharsets.UTF_8)));

        if(throwable != null) {
            // set log's throwable
            StringWriter stringWriter = new StringWriter();
            throwable.printStackTrace(new PrintWriter(stringWriter));
            logEvent.setThrowable(MemorySegment.ofArray(stringWriter.toString().getBytes(StandardCharsets.UTF_8)));
        }

        final SegmentBuilder builder = logEvent.getBuilder();
        builder.reset();
        builder.append(logTime.segment())
                .append(logEvent.getLevel())
                .append(Constants.b2)
                .append(Constants.b7)
                .append(logEvent.getThreadName())
                .append(Constants.b8)
                .append(Constants.b2)
                .append(logEvent.getClassName())
                .append(Constants.b2)
                .append(Constants.b1)
                .append(Constants.b2);
        MemorySegment msg = MemorySegment.ofArray(s.getBytes(StandardCharsets.UTF_8));
        if(objects == null || objects.length == 0) {
            builder.append(msg);
            logEvent.setMsg(msg);
        }else {
            List<MemorySegment> list = Arrays.stream(objects).map(o -> MemorySegment.ofArray(o.toString().getBytes(StandardCharsets.UTF_8))).toList();
            logEvent.setMsg(builder.append(msg, list));
        }
        // fire logEvent, whether or not using throwable is depending on the handler
        pipeline.fireEvent(logEvent);
    }

    @Override
    public boolean isTraceEnabled() {
        return Logger.level <= Constants.TRACE;
    }

    @Override
    public boolean isDebugEnabled() {
        return Logger.level <= Constants.DEBUG;
    }

    @Override
    public boolean isInfoEnabled() {
        return Logger.level <= Constants.INFO;
    }

    @Override
    public boolean isWarnEnabled() {
        return Logger.level <= Constants.WARN;
    }

    @Override
    public boolean isErrorEnabled() {
        return Logger.level <= Constants.ERROR;
    }
}
