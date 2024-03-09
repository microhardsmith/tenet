package cn.zorcc.common.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.bindings.TenetBinding;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.structure.WriteBuffer;
import cn.zorcc.common.util.FileUtil;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class ConsoleLogEventHandler implements Consumer<LogEvent> {
    private final List<LogHandler> handlers;
    private final List<LogEvent> eventList = new ArrayList<>();
    private final int flushThreshold;
    private final MemorySegment reserved;
    private final MemorySegment stdout = TenetBinding.stdout();
    private final MemorySegment stderr = TenetBinding.stderr();

    public ConsoleLogEventHandler(LogConfig logConfig, MemorySegment m) {
        ConsoleLogConfig config = logConfig.getConsole();
        reserved = m;
        handlers = Logger.createLogHandlers(logConfig.getLogFormat(), s -> switch (s) {
            case "time" -> timeHandler(config);
            case "level" -> levelHandler(config);
            case "className" -> classNameHandler(config);
            case "threadName" -> threadNameHandler(config);
            case "msg" -> msgHandler(config);
            default -> throw new FrameworkException(ExceptionType.LOG, STR."Unresolved log format : \{s}");
        });
        flushThreshold = config.getFlushThreshold();
        if (FileUtil.setvbuf(stdout, MemorySegment.NULL, TenetBinding.nbf(), 0) != 0) {
            throw new FrameworkException(ExceptionType.LOG, "Failed to set stdout to nbf mode");
        }
        if (FileUtil.setvbuf(stderr, MemorySegment.NULL, TenetBinding.nbf(), 0) != 0) {
            throw new FrameworkException(ExceptionType.LOG, "Failed to set stderr to nbf mode");
        }
    }

    @Override
    public void accept(LogEvent event) {
        switch (event.eventType()) {
            case Msg -> {
                eventList.add(event);
                if((flushThreshold > 0 && eventList.size() > flushThreshold) || event.throwable() != null) {
                    flush();
                }
            }
            case Flush, Shutdown -> {
                if(!eventList.isEmpty()) {
                    flush();
                }
            }
        }
    }

    private void flush() {
        if(NativeUtil.isRunningFromJar()) {
            flushToStdoutAndStderr();
        }else {
            flushToStdout();
        }
    }

    /**
     *   When using IDE like Intellij Idea, it has a long unfixed bug about using both stdout and stderr for more than ten years
     *   The output to stdout and stderr will be mixed together
     *   So when running in an IDE, let's just flush everything to stdout to keep things working
     */
    private void flushToStdout() {
        try(WriteBuffer outBuffer = WriteBuffer.newReservedWriteBuffer(reserved, false)) {
            for (LogEvent event : eventList) {
                handlers.forEach(logHandler -> logHandler.process(outBuffer ,event));
                if(event.throwable() != null) {
                    outBuffer.writeSegment(Constants.ANSI_PREFIX);
                    outBuffer.writeSegment(Constants.RED_BYTES);
                    outBuffer.writeSegment(event.throwable());
                    outBuffer.writeSegment(Constants.ANSI_SUFFIX);
                }
            }
            if(outBuffer.writeIndex() > 0L) {
                FileUtil.fwrite(outBuffer.asSegment(), stdout);
            }
        }finally {
            eventList.clear();
        }
    }

    /**
     *   This is the default console-logging behaviour
     */
    private void flushToStdoutAndStderr() {
        try(WriteBuffer outBuffer = WriteBuffer.newReservedWriteBuffer(reserved, false);
            WriteBuffer errBuffer = WriteBuffer.newDefaultWriteBuffer(Constants.KB)) {
            for (LogEvent event : eventList) {
                handlers.forEach(logHandler -> logHandler.process(outBuffer, event));
                if(event.throwable() != null) {
                    errBuffer.writeSegment(event.throwable());
                }
            }
            if(outBuffer.writeIndex() > 0L) {
                FileUtil.fwrite(outBuffer.asSegment(), stdout);
            }
            if(errBuffer.writeIndex() > 0L) {
                FileUtil.fwrite(errBuffer.asSegment(), stderr);
            }
        }finally {
            eventList.clear();
        }
    }

    /**
     *   Get the color byte array that belong to the level bytes
     */
    private static MemorySegment levelColorBytes(MemorySegment level) {
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
    private static MemorySegment colorBytes(String color) {
        return switch (color) {
            case Constants.RED -> Constants.RED_BYTES;
            case Constants.GREEN -> Constants.GREEN_BYTES;
            case Constants.YELLOW -> Constants.YELLOW_BYTES;
            case Constants.BLUE -> Constants.BLUE_BYTES;
            case Constants.MAGENTA -> Constants.MAGENTA_BYTES;
            case Constants.CYAN -> Constants.CYAN_BYTES;
            default -> null;
        };
    }

    private static LogHandler timeHandler(ConsoleLogConfig config) {
        MemorySegment timeColorBytes = colorBytes(config.getTimeColor());
        if (timeColorBytes != null) {
            return (writeBuffer, logEvent) -> {
                writeBuffer.writeSegment(Constants.ANSI_PREFIX);
                writeBuffer.writeSegment(timeColorBytes);
                writeBuffer.writeSegment(logEvent.time());
                writeBuffer.writeSegment(Constants.ANSI_SUFFIX);
            };
        } else {
            return (writeBuffer, logEvent) -> writeBuffer.writeSegment(logEvent.time());
        }
    }

    private static LogHandler levelHandler(ConsoleLogConfig config) {
        return (writeBuffer, logEvent) -> {
            MemorySegment level = logEvent.level();
            writeBuffer.writeSegment(Constants.ANSI_PREFIX);
            writeBuffer.writeSegment(levelColorBytes(level));
            writeBuffer.writeSegmentWithPadding(level, config.getLevelLen(), Constants.SPACE);
            writeBuffer.writeSegment(Constants.ANSI_SUFFIX);
        };
    }

    private static LogHandler classNameHandler(ConsoleLogConfig config) {
        MemorySegment classNameColorBytes = colorBytes(config.getClassNameColor());
        if (classNameColorBytes != null) {
            return (writeBuffer, logEvent) -> {
                writeBuffer.writeSegment(Constants.ANSI_PREFIX);
                writeBuffer.writeSegment(classNameColorBytes);
                writeBuffer.writeSegmentWithPadding(logEvent.className(), config.getClassNameLen(), Constants.SPACE);
                writeBuffer.writeSegment(Constants.ANSI_SUFFIX);
            };
        } else {
            return (writeBuffer, logEvent) -> writeBuffer.writeSegmentWithPadding(logEvent.className(), config.getClassNameLen(), Constants.SPACE);
        }
    }

    private static LogHandler threadNameHandler(ConsoleLogConfig config) {
        MemorySegment threadNameColorBytes = colorBytes(config.getThreadNameColor());
        if (threadNameColorBytes != null) {
            return (writeBuffer, logEvent) -> {
                writeBuffer.writeSegment(Constants.ANSI_PREFIX);
                writeBuffer.writeSegment(threadNameColorBytes);
                writeBuffer.writeSegmentWithPadding(logEvent.threadName(), config.getThreadNameLen(), Constants.SPACE);
                writeBuffer.writeSegment(Constants.ANSI_SUFFIX);
            };
        } else {
            return (writeBuffer, logEvent) -> writeBuffer.writeSegmentWithPadding(logEvent.threadName(), config.getThreadNameLen(), Constants.SPACE);
        }
    }

    private static LogHandler msgHandler(ConsoleLogConfig config) {
        MemorySegment msgColorBytes = colorBytes(config.getMsgColor());
        if (msgColorBytes != null) {
            return (writeBuffer, logEvent) -> {
                writeBuffer.writeSegment(Constants.ANSI_PREFIX);
                writeBuffer.writeSegment(msgColorBytes);
                writeBuffer.writeSegment(logEvent.msg());
                writeBuffer.writeSegment(Constants.ANSI_SUFFIX);
            };
        } else {
            return (writeBuffer, logEvent) -> writeBuffer.writeSegment(logEvent.msg());
        }
    }
}
