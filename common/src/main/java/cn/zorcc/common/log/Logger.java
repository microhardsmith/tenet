package cn.zorcc.common.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ConfigUtil;
import cn.zorcc.common.util.ThreadUtil;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.LegacyAbstractLogger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;

/**
 *  tenet logger
 */
public class Logger extends LegacyAbstractLogger {
    private static final Map<Level, byte[]> levelMap = Map.of(
            Level.DEBUG, Constants.DEBUG_BYTES,
            Level.TRACE, Constants.TRACE_BYTES,
            Level.INFO, Constants.INFO_BYTES,
            Level.WARN, Constants.WARN_BYTES,
            Level.ERROR, Constants.ERROR_BYTES
    );
    private static final LogConfig config;
    private static final int level;
    private static final DateTimeFormatter formatter;
    private static final TimeResolver timeResolver;
    private static final TransferQueue<LogEvent> queue = new LinkedTransferQueue<>();

    static {
        config = ConfigUtil.loadJsonConfig(Constants.DEFAULT_LOG_CONFIG_NAME, LogConfig.class);
        formatter = DateTimeFormatter.ofPattern(config.getTimeFormat(), Locale.getDefault());
        String resolver = config.getTimeResolver();
        if(resolver != null && !resolver.isEmpty()) {
            try {
                Class<?> timeResolverClass = Class.forName(resolver);
                if(!TimeResolver.class.isAssignableFrom(timeResolverClass)) {
                    throw new FrameworkException(ExceptionType.LOG, "Target TimeResolver is not valid");
                }
                timeResolver = (TimeResolver) timeResolverClass.getConstructor().newInstance();
            } catch (Exception e) {
                throw new FrameworkException(ExceptionType.LOG, "Unable to instantiate target TimeResolver", e);
            }
        }else {
            timeResolver = null;
        }
        level = switch (config.getLevel()) {
            case Constants.TRACE -> Constants.TRACE;
            case Constants.INFO -> Constants.INFO;
            case Constants.WARN -> Constants.WARN;
            case Constants.ERROR -> Constants.ERROR;
            case Constants.DEBUG -> Constants.DEBUG;
            default -> throw new FrameworkException(ExceptionType.LOG, "Unsupported Log default level");
        };
    }

    public static LogConfig config() {
        return config;
    }

    public static TransferQueue<LogEvent> queue() {
        return queue;
    }

    public Logger(String name) {
        this.name = name;
    }

    @Override
    protected String getFullyQualifiedCallerName() {
        return null;
    }

    /**
     *  日志输出,日志分为五个部分： 时间 等级 线程名 类名 日志消息
     */
    @Override
    protected void handleNormalizedLoggingCall(Level level, Marker marker, String s, Object[] objects, Throwable throwable) {
        Instant instant = Constants.SYSTEM_CLOCK.instant();
        LocalDateTime now = LocalDateTime.ofEpochSecond(instant.getEpochSecond(), instant.getNano(), Constants.LOCAL_ZONE_OFFSET);
        long timestamp = instant.toEpochMilli();

        byte[] time = timeResolver == null ? formatter.format(now).getBytes(StandardCharsets.UTF_8) : timeResolver.format(now);
        byte[] lv = levelMap.get(level);
        byte[] threadName = ThreadUtil.threadName().getBytes(StandardCharsets.UTF_8);
        byte[] className = getName().getBytes(StandardCharsets.UTF_8);
        byte[] th = null;
        if(throwable != null) {
            // set log's throwable
            StringWriter stringWriter = new StringWriter();
            throwable.printStackTrace(new PrintWriter(stringWriter));
            th = stringWriter.toString().getBytes(StandardCharsets.UTF_8);
        }
        byte[] msg = processMsg(s, objects);
        LogEvent logEvent = new LogEvent(false, timestamp, time, lv, threadName, className, th, msg);
        if (!queue.offer(logEvent)) {
            throw new FrameworkException(ExceptionType.LOG, Constants.UNREACHED);
        }
    }

    /**
     *   process msg, replacing the {} with target args
     */
    public static byte[] processMsg(String msg, Object[] args) {
        byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
        if(args == null || args.length == 0) {
            return msgBytes;
        } else {
            List<byte[]> list = new ArrayList<>(args.length);
            int reserved = 0;
            for (Object arg : args) {
                byte[] b = arg.toString().getBytes(StandardCharsets.UTF_8);
                list.add(b);
                reserved += b.length;
            }
            byte[] result = new byte[reserved + msgBytes.length];
            int len = 0;
            for(int i = 0, index = 0; i < msgBytes.length; i++) {
                byte b = msgBytes[i];
                if(b == Constants.b5 && i + 1 < msgBytes.length && msgBytes[i + 1] == Constants.b6) {
                    // reaching {}, need to parse the arg
                    byte[] argBytes = list.get(index++);
                    System.arraycopy(argBytes, 0, result, len, argBytes.length);
                    len += argBytes.length;
                    i++; // escape "{}"
                }else {
                    result[len++] = b;
                }
            }
            return Arrays.copyOf(result, len);
        }
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
