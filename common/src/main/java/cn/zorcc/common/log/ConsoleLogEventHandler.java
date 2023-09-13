package cn.zorcc.common.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.event.EventHandler;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.FileUtil;
import cn.zorcc.common.util.NativeUtil;
import cn.zorcc.common.util.StringUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *   Print log to the console, using C's puts and fflush mechanism
 *   Using fputs and fflush from C to replace Java's PrintStream regarding to benchmark, could be several times faster that normal System.out.println():
 *      PrintTest.testNative       avgt   25   759.945 ±  28.064  ns/op
 *      PrintTest.testSystemOut    avgt   25  2758.746 ± 108.389  ns/op
 */
public final class ConsoleLogEventHandler implements EventHandler<LogEvent> {
    private static final MemorySegment stdout = NativeUtil.stdout();
    private static final MemorySegment stderr = NativeUtil.stderr();
    private final List<LogHandler> handlers;
    private final Arena reservedArena;
    private final MemorySegment reserved;

    public ConsoleLogEventHandler(LogConfig logConfig) {
        this.handlers = createLogHandlers(logConfig);
        // Console builder should have a larger size than logEvent, the constructor method should be guaranteed to be called in log thread to keep arena safe
        this.reservedArena = Arena.ofConfined();
        this.reserved = reservedArena.allocateArray(ValueLayout.JAVA_BYTE, logConfig.getBufferSize() << Constants.ONE);
    }

    /**
     *   Get the color byte array that belong to the level bytes
     */
    private static byte[] levelColorBytes(byte[] level) {
        if(level == Constants.ERROR_BYTES) {
            return Constants.RED_BYTES;
        }else if(level == Constants.WARN_BYTES) {
            return Constants.YELLOW_BYTES;
        }else {
            return Constants.GREEN_BYTES;
        }
    }

    /**
     *   Get the color byte array that belong to the color string
     */
    private static byte[] colorBytes(String color) {
        switch (color) {
            case Constants.RED -> {
                return Constants.RED_BYTES;
            }
            case Constants.GREEN -> {
                return Constants.GREEN_BYTES;
            }
            case Constants.YELLOW -> {
                return Constants.YELLOW_BYTES;
            }
            case Constants.BLUE -> {
                return Constants.BLUE_BYTES;
            }
            case Constants.MAGENTA -> {
                return Constants.MAGENTA_BYTES;
            }
            case Constants.CYAN -> {
                return Constants.CYAN_BYTES;
            }
            default -> {
                // no color wrapped
                return null;
            }
        }
    }

    @Override
    public void handle(LogEvent event) {
        // Ignore flush or shutdown since the console will be flushed every time
        if(event == LogEvent.shutdownEvent) {
            reservedArena.close();
        }else if(event == LogEvent.flushEvent) {
            FileUtil.fflush(stdout);
        } else {
            print(event);
        }
    }

    private void print(LogEvent event) {
        try(WriteBuffer writeBuffer = WriteBuffer.newReservedWriteBuffer(reserved)) {
            for (LogHandler handler : handlers) {
                handler.process(writeBuffer, event);
            }
            if(writeBuffer.writeIndex() > Constants.ZERO) {
                writeBuffer.writeByte(Constants.LF);
                FileUtil.fwrite(writeBuffer.content(), stdout);
            }
        }
        byte[] throwable = event.throwable();
        if(throwable != null) {
            try(WriteBuffer writeBuffer = WriteBuffer.newReservedWriteBuffer(reserved)) {
                writeBuffer.writeBytes(throwable);
                FileUtil.fwrite(writeBuffer.content(), stderr);
                FileUtil.fflush(stdout);
                FileUtil.fflush(stderr);
            }
        }
    }

    /**
     *   Parsing log-format to a lambda consumer list
     */
    private static List<LogHandler> createLogHandlers(LogConfig logConfig) {
        List<LogHandler> logHandlers = new ArrayList<>();
        byte[] format = logConfig.getLogFormat().getBytes(StandardCharsets.UTF_8);
        int index = Constants.ZERO;
        for( ; ; ) {
            index = searchNormalStr(format, index, logHandlers);
            if(index < Constants.ZERO) {
                return logHandlers;
            }else {
                index = searchFormattedStr(format, index, logHandlers, logConfig);
            }
        }
    }

    private static int searchNormalStr(byte[] format, int startIndex, List<LogHandler> handlers) {
        int nextIndex = StringUtil.searchBytes(format, Constants.PERCENT, startIndex, bytes -> handlers.add((writeBuffer, event) -> writeBuffer.writeBytes(bytes)));
        if(nextIndex < Constants.ZERO && startIndex < format.length) {
            final byte[] arr = Arrays.copyOfRange(format, startIndex, format.length);
            handlers.add((writeBuffer, event) -> writeBuffer.writeBytes(arr));
        }
        return nextIndex;
    }

    private static int searchFormattedStr(byte[] format, int startIndex, List<LogHandler> handlers, LogConfig logConfig) {
        int nextIndex = StringUtil.searchStr(format, Constants.PERCENT, startIndex, s -> {
            LogHandler handler = switch (s) {
                case "time" -> timeHandler(logConfig);
                case "level" -> levelHandler(logConfig);
                case "className" -> classNameHandler(logConfig);
                case "threadName" -> threadNameHandler(logConfig);
                case "msg" -> msgHandler(logConfig);
                default -> throw new FrameworkException(ExceptionType.LOG, "Unresolved log format : %s".formatted(s));
            };
            handlers.add(handler);
        });
        if(nextIndex < Constants.ZERO) {
            throw new FrameworkException(ExceptionType.LOG, "Corrupted log format");
        }
        return nextIndex;
    }

    private static LogHandler timeHandler(LogConfig logConfig) {
        byte[] timeColorBytes = colorBytes(logConfig.getTimeColor());
        if (timeColorBytes != null) {
            return (writeBuffer, logEvent) -> {
                writeBuffer.writeBytes(Constants.ANSI_PREFIX);
                writeBuffer.writeBytes(timeColorBytes);
                writeBuffer.writeBytes(logEvent.time());
                writeBuffer.writeBytes(Constants.ANSI_SUFFIX);
            };
        } else {
            return (writeBuffer, logEvent) -> writeBuffer.writeBytes(logEvent.time());
        }
    }

    private static LogHandler levelHandler(LogConfig logConfig) {
        return (writeBuffer, logEvent) -> {
            byte[] level = logEvent.level();
            writeBuffer.writeBytes(Constants.ANSI_PREFIX);
            writeBuffer.writeBytes(levelColorBytes(level));
            writeBuffer.writeBytesWithPadding(level, logConfig.getLevelLen(), Constants.SPACE);
            writeBuffer.writeBytes(Constants.ANSI_SUFFIX);
        };
    }

    private static LogHandler classNameHandler(LogConfig logConfig) {
        byte[] classNameColorBytes = colorBytes(logConfig.getClassNameColor());
        if (classNameColorBytes != null) {
            return (writeBuffer, logEvent) -> {
                writeBuffer.writeBytes(Constants.ANSI_PREFIX);
                writeBuffer.writeBytes(classNameColorBytes);
                writeBuffer.writeBytesWithPadding(logEvent.className(), logConfig.getClassNameLen(), Constants.SPACE);
                writeBuffer.writeBytes(Constants.ANSI_SUFFIX);
            };
        } else {
            return (writeBuffer, logEvent) -> writeBuffer.writeBytesWithPadding(logEvent.className(), logConfig.getClassNameLen(), Constants.SPACE);
        }
    }

    private static LogHandler threadNameHandler(LogConfig logConfig) {
        byte[] threadNameColorBytes = colorBytes(logConfig.getThreadNameColor());
        if (threadNameColorBytes != null) {
            return (writeBuffer, logEvent) -> {
                writeBuffer.writeBytes(Constants.ANSI_PREFIX);
                writeBuffer.writeBytes(threadNameColorBytes);
                writeBuffer.writeBytesWithPadding(logEvent.threadName(), logConfig.getThreadNameLen(), Constants.SPACE);
                writeBuffer.writeBytes(Constants.ANSI_SUFFIX);
            };
        } else {
            return (writeBuffer, logEvent) -> writeBuffer.writeBytesWithPadding(logEvent.threadName(), logConfig.getThreadNameLen(), Constants.SPACE);
        }
    }

    private static LogHandler msgHandler(LogConfig logConfig) {
        byte[] msgColorBytes = colorBytes(logConfig.getMsgColor());
        if (msgColorBytes != null) {
            return (writeBuffer, logEvent) -> {
                writeBuffer.writeBytes(Constants.ANSI_PREFIX);
                writeBuffer.writeBytes(msgColorBytes);
                writeBuffer.writeBytes(logEvent.msg());
                writeBuffer.writeBytes(Constants.ANSI_SUFFIX);
            };
        } else {
            return (writeBuffer, logEvent) -> writeBuffer.writeBytes(logEvent.msg());
        }
    }
}
