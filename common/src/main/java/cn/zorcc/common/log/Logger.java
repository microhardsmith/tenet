package cn.zorcc.common.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ConfigUtil;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 *  Tenet logger, using a single thread to async log msg to console, file or remote
 */
public final class Logger {
    private static final Map<LogLevel, MemorySegment> levelMap = Map.of(
            LogLevel.DEBUG, Constants.DEBUG_BYTES,
            LogLevel.TRACE, Constants.TRACE_BYTES,
            LogLevel.INFO, Constants.INFO_BYTES,
            LogLevel.WARN, Constants.WARN_BYTES,
            LogLevel.ERROR, Constants.ERROR_BYTES
    );
    /**
     *   Default logging level would be INFO
     */
    private static final LogConfig logConfig;
    private static final int level;
    private static final TimeResolver timeResolver;
    private static final TransferQueue<LogEvent> queue = new LinkedTransferQueue<>();
    private final MemorySegment className;

    static {
        String configFile = Optional.ofNullable(System.getProperty("log.file")).orElse(Constants.DEFAULT_LOG_CONFIG_NAME);
        logConfig = ConfigUtil.loadJsonConfig(configFile, LogConfig.class);
        timeResolver = createTimeResolver();
        level = switch (logConfig.getLevel()) {
            case "TRACE"       -> Constants.TRACE;
            case "WARN"        -> Constants.WARN;
            case "ERROR"       -> Constants.ERROR;
            case "DEBUG"       -> Constants.DEBUG;
            case null, default -> Constants.INFO;
        };
    }

    public static LogConfig getLogConfig() {
        return logConfig;
    }

    private static TimeResolver createTimeResolver() {
        if(logConfig.isUsingTimeResolver()) {
            return ServiceLoader.load(TimeResolver.class).findFirst().orElseGet(DefaultTimeResolver::new);
        }else {
            String timeFormat = logConfig.getTimeFormat();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(timeFormat);
            return time -> MemorySegment.ofArray(dateTimeFormatter.format(time).getBytes(StandardCharsets.UTF_8));
        }
    }

    public Logger(Class<?> clazz) {
        className = MemorySegment.ofArray(clazz.getName().getBytes(StandardCharsets.UTF_8));
    }

    public static TransferQueue<LogEvent> queue() {
        return queue;
    }

    private void doLog(LogLevel level, String str, Throwable throwable) {
        Instant instant = Constants.SYSTEM_CLOCK.instant();
        LocalDateTime now = LocalDateTime.ofEpochSecond(instant.getEpochSecond(), instant.getNano(), Constants.LOCAL_ZONE_OFFSET);
        long timestamp = instant.toEpochMilli();
        MemorySegment time = timeResolver.format(now);
        MemorySegment lv = levelMap.get(level);
        MemorySegment threadName = MemorySegment.ofArray(getCurrentThreadName().getBytes(StandardCharsets.UTF_8));
        MemorySegment th = throwableToSegment(throwable);
        MemorySegment msg = MemorySegment.ofArray(str.getBytes(StandardCharsets.UTF_8));
        LogEvent logEvent = new LogEvent(LogEventType.Msg, timestamp, time, lv, threadName, className, th, msg);
        if (!queue.offer(logEvent)) {
            throw new FrameworkException(ExceptionType.LOG, Constants.UNREACHED);
        }
    }

    private static String getCurrentThreadName() {
        Thread currentThread = Thread.currentThread();
        String threadName = currentThread.getName();
        if(threadName.isEmpty()) {
            return (currentThread.isVirtual() ? Constants.VIRTUAL_THREAD : Constants.PLATFORM_THREAD) + currentThread.threadId();
        }else {
            return threadName;
        }
    }

    private MemorySegment throwableToSegment(Throwable throwable) {
        if(throwable == null) {
            return null;
        }else {
            StringWriter stringWriter = new StringWriter();
            throwable.printStackTrace(new PrintWriter(stringWriter));
            return MemorySegment.ofArray(stringWriter.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    public void trace(String msg) {
        trace(msg, null);
    }

    public void trace(String msg, Throwable throwable) {
        if(level <= Constants.TRACE) {
            doLog(LogLevel.TRACE, msg, throwable);
        }
    }
    public void debug(String msg) {
        debug(msg, null);
    }

    public void debug(String msg, Throwable throwable) {
        if (level <= Constants.DEBUG) {
            doLog(LogLevel.DEBUG, msg, throwable);
        }
    }

    public void info(String msg) {
        info(msg, null);
    }

    public void info(String msg, Throwable throwable) {
        if (level <= Constants.INFO) {
            doLog(LogLevel.INFO, msg, throwable);
        }
    }

    public void warn(String msg) {
        warn(msg, null);
    }

    public void warn(String msg, Throwable throwable) {
        if (level <= Constants.WARN) {
            doLog(LogLevel.WARN, msg, throwable);
        }
    }

    public void error(String msg) {
        error(msg, null);
    }

    public void error(String msg, Throwable throwable) {
        if (level <= Constants.ERROR) {
            doLog(LogLevel.ERROR, msg, throwable);
        }
    }

    public static List<LogHandler> createLogHandlers(String logFormat, Function<String, LogHandler> transformer) {
        List<LogHandler> logHandlers = new ArrayList<>();
        MemorySegment format = MemorySegment.ofArray(logFormat.getBytes(StandardCharsets.UTF_8));
        long index = 0;
        for( ; ; ) {
            index = search(format, index, logHandlers, transformer);
            if(index < 0) {
                return logHandlers;
            }
        }
    }

    private static long search(MemorySegment format, long startIndex, List<LogHandler> handlers, Function<String, LogHandler> transformer) {
        long nextIndex = searchBytes(format, Constants.LCB, startIndex, segment -> handlers.add((writeBuffer, _) -> writeBuffer.writeSegment(segment)));
        if(nextIndex < 0) {
            if(startIndex < format.byteSize()) {
                handlers.add((writeBuffer, _) -> writeBuffer.writeSegment(format.asSlice(startIndex, format.byteSize() - startIndex)));
            }
            handlers.add((writeBuffer, _) -> writeBuffer.writeByte(Constants.LF));
            return nextIndex;
        }
        nextIndex = searchBytes(format, Constants.RCB, nextIndex, segment -> handlers.add(transformer.apply(new String(segment.toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8))));
        if(nextIndex < 0) {
            handlers.add((writeBuffer, _) -> writeBuffer.writeByte(Constants.LF));
        }
        return nextIndex;
    }

    /**
     *   Find next expected byte in the data from startIndex, return the target index + 1 of the expected byte TODO using ReadBuffer patternSearch
     */
    private static long searchBytes(MemorySegment data, byte expected, long startIndex, Consumer<MemorySegment> consumer) {
        for(long index = startIndex; index < data.byteSize(); index++) {
            if(data.get(ValueLayout.JAVA_BYTE, index) == expected) {
                if(index > startIndex) {
                    consumer.accept(data.asSlice(startIndex, index - startIndex));
                }
                return index + 1 == data.byteSize() ? Long.MIN_VALUE : index + 1;
            }
        }
        return Long.MIN_VALUE;
    }
}
