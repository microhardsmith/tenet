package cn.zorcc.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Context;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.event.EventHandler;
import cn.zorcc.common.event.EventPipeline;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ConfigUtil;
import cn.zorcc.common.util.ThreadUtil;
import org.jctools.queues.atomic.MpmcAtomicArrayQueue;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.LegacyAbstractLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * 负责打印lithiasis项目中使用的日志
 */
public class Logger extends LegacyAbstractLogger {
    private static final int level;
    private static final MpmcAtomicArrayQueue<LogEvent> eventQueue;
    private static final EventPipeline<LogEvent> pipeline;
    private static final int pipelineSize;


    static {
        LogConfig logConfig = ConfigUtil.loadJsonConfig(Constants.DEFAULT_LOG_CONFIG_NAME, LogConfig.class);
        switch (logConfig.getLevel()) {
            case Constants.TRACE -> level = Constants.TRACE;
            case Constants.INFO -> level = Constants.INFO;
            case Constants.WARN -> level = Constants.WARN;
            case Constants.ERROR -> level = Constants.ERROR;
            case Constants.DEBUG -> level = Constants.DEBUG;
            default -> throw new FrameworkException(ExceptionType.LOG, "Unsupported Log default level");
        }
        int queueSize = logConfig.getQueueSize();
        eventQueue = new MpmcAtomicArrayQueue<>(queueSize);
        for (int i = 0; i < queueSize; i++) {
            if (!eventQueue.offer(new LogEvent(true))) {
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

    @Override
    protected void handleNormalizedLoggingCall(Level level, Marker marker, String s, Object[] objects, Throwable throwable) {
        LogEvent logEvent = eventQueue.poll();
        if(logEvent == null) {
            // 当事件队列中无可用事件时，新建LogEvent使用
            logEvent = new LogEvent(false);
        }
        logEvent.setThreadName(ThreadUtil.threadName());
        logEvent.setLevel(level);
        logEvent.setClassName(getName());
        logEvent.setOriginMsg(s);
        logEvent.setArgs(objects);
        logEvent.setThrowable(throwable);
        final StringBuilder builder = logEvent.getBuilder();
        if (objects == null || objects.length == 0) {
            logEvent.setMsg(s);
        } else {
            char[] chars = s.toCharArray();
            int length = chars.length;
            for (int i = 0, argIndex = 0; i < length; i++) {
                char c = chars[i];
                if (c == Constants.L_BRACE && i < length - 1 && chars[i + 1] == Constants.R_BRACE && argIndex < objects.length) {
                    builder.append(objects[argIndex]);
                    argIndex++;
                    i++;
                } else {
                    builder.append(c);
                }
            }
            logEvent.setMsg(builder.toString());
            builder.setLength(Constants.ZERO);
        }
        builder.append(logEvent.getLogTime().timeArray())
                .append(Constants.BLANK)
                .append(logEvent.getLevel())
                .append(Constants.BLANK)
                .append(Constants.L_SQUARE)
                .append(logEvent.getThreadName())
                .append(Constants.R_SQUARE)
                .append(Constants.BLANK)
                .append(logEvent.getClassName())
                .append(Constants.HYPHEN)
                .append(logEvent.getMsg())
                .append(Constants.LINE_SEPARATOR);
        logEvent.setLine(builder.toString());
        builder.setLength(Constants.ZERO);
        // 触发日志事件
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
