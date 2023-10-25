package cn.zorcc.common.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.StringUtil;
import cn.zorcc.common.util.ThreadUtil;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;
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
    private static final int level;
    private static final TimeResolver timeResolver;
    private static final TransferQueue<LogEvent> queue = new LinkedTransferQueue<>();
    private final MemorySegment className;

    static {
        timeResolver = ServiceLoader.load(TimeResolver.class).findFirst().orElseGet(DefaultTimeResolver::new);
        level = switch (System.getProperty("log.level")) {
            case "TRACE"       -> Constants.TRACE;
            case "WARN"        -> Constants.WARN;
            case "ERROR"       -> Constants.ERROR;
            case "DEBUG"       -> Constants.DEBUG;
            case null, default -> Constants.INFO;
        };
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
        MemorySegment threadName = MemorySegment.ofArray(ThreadUtil.threadName().getBytes(StandardCharsets.UTF_8));
        MemorySegment th = throwableToSegment(throwable);
        MemorySegment msg = MemorySegment.ofArray(str.getBytes(StandardCharsets.UTF_8));
        LogEvent logEvent = new LogEvent(LogEventType.Msg, timestamp, time, lv, threadName, className, th, msg);
        if (!queue.offer(logEvent)) {
            throw new FrameworkException(ExceptionType.LOG, Constants.UNREACHED);
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
        long nextIndex = StringUtil.searchBytes(format, Constants.LCB, startIndex, segment -> handlers.add((writeBuffer, event) -> writeBuffer.writeSegment(segment)));
        if(nextIndex < 0) {
            if(startIndex < format.byteSize()) {
                handlers.add((writeBuffer, event) -> writeBuffer.writeSegment(format.asSlice(startIndex, format.byteSize() - startIndex)));
            }
            handlers.add((writeBuffer, event) -> writeBuffer.writeByte(Constants.LF));
            return nextIndex;
        }
        nextIndex = StringUtil.searchBytes(format, Constants.RCB, nextIndex, segment -> handlers.add(transformer.apply(new String(segment.toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8))));
        if(nextIndex < 0) {
            handlers.add((writeBuffer, event) -> writeBuffer.writeByte(Constants.LF));
        }
        return nextIndex;
    }
}
