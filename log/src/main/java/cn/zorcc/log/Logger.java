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
import cn.zorcc.common.wheel.TimeWheel;
import cn.zorcc.common.wheel.TimerJob;
import org.jctools.queues.atomic.MpmcAtomicArrayQueue;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.LegacyAbstractLogger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    private static final int bufferSize;
    private static final DateTimeFormatter formatter;
    private static final EventPipeline<LogEvent> pipeline;
    private static final int pipelineSize;
    private static final TimerJob flushJob;


    static {
        LogConfig logConfig = ConfigUtil.loadJsonConfig(Constants.DEFAULT_LOG_CONFIG_NAME, LogConfig.class);
        bufferSize = Math.max(logConfig.getBufferSize(), Constants.KB);
        formatter = DateTimeFormatter.ofPattern(logConfig.getTimeFormat());
        switch (logConfig.getLevel()) {
            case Constants.TRACE -> level = Constants.TRACE;
            case Constants.INFO -> level = Constants.INFO;
            case Constants.WARN -> level = Constants.WARN;
            case Constants.ERROR -> level = Constants.ERROR;
            case Constants.DEBUG -> level = Constants.DEBUG;
            default -> throw new FrameworkException(ExceptionType.LOG, "Unsupported Log default level");
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
        flushJob = TimeWheel.instance().addPeriodicJob(() -> pipeline.fireEvent(LogEvent.flushEvent), 0L, logConfig.getFlushInterval(), TimeUnit.MILLISECONDS);
    }

    /**
     *  获取日志事件pipeline大小
     */
    public static int pipelineSize() {
        return pipelineSize;
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
        LogEvent logEvent = new LogEvent(bufferSize);
        Arena arena = logEvent.getArena();

        LocalDateTime now = LocalDateTime.now();
        logEvent.setTimestamp(now.toInstant(Constants.LOCAL_ZONE_OFFSET).toEpochMilli());
        logEvent.setTime(arena.allocateArray(ValueLayout.JAVA_BYTE, formatter.format(now).getBytes(StandardCharsets.UTF_8)));
        logEvent.setLevel(levelMap.get(level));
        logEvent.setThreadName(arena.allocateArray(ValueLayout.JAVA_BYTE, ThreadUtil.threadName().getBytes(StandardCharsets.UTF_8)));
        logEvent.setClassName(arena.allocateArray(ValueLayout.JAVA_BYTE, getName().getBytes(StandardCharsets.UTF_8)));

        if(throwable != null) {
            // set log's throwable
            StringWriter stringWriter = new StringWriter();
            throwable.printStackTrace(new PrintWriter(stringWriter));
            logEvent.setThrowable(arena.allocateArray(ValueLayout.JAVA_BYTE, stringWriter.toString().getBytes(StandardCharsets.UTF_8)));
        }

        final SegmentBuilder builder = logEvent.getBuilder();
        builder.reset();
        builder.append(logEvent.getTime())
                .append(Constants.b2)
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
        logEvent.test(); //DEBUG
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
