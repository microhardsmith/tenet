package cn.zorcc.common.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.event.EventHandler;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.FileUtil;
import cn.zorcc.common.util.StringUtil;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *   Print log to the file,
 */
public final class FileLogEventHandler implements EventHandler<LogEvent> {
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS");
    private final String logDirPath;
    private final String logFileName;
    private final long maxFileSize;
    private final long maxRecordingTime;
    private final List<LogHandler> handlers;
    private final Arena reservedArena = Arena.ofConfined();
    private final MemorySegment reserved;
    private long allocationTimestamp;
    private long lastEventTimestamp;
    private long lastFlushTimestamp;
    private MemorySegment fileStream;
    private long currentWrittenIndex;
    public FileLogEventHandler(LogConfig logConfig) {
        try{
            String dir = logConfig.getLogFileDir();
            this.logDirPath = dir == null || dir.isEmpty() ? System.getProperty("user.dir") : dir;
            Path dirPath = Path.of(logDirPath);
            if(!Files.exists(dirPath)) {
                Files.createDirectory(dirPath);
            }
            this.logFileName = logConfig.getLogFileName();
            this.maxFileSize = logConfig.getMaxFileSize();
            if(maxFileSize <= Constants.MB) {
                throw new FrameworkException(ExceptionType.LOG, "The log file size limit is too small");
            }
            this.maxRecordingTime = logConfig.getMaxRecordingTime() <= Constants.ZERO ? Long.MIN_VALUE : logConfig.getMaxRecordingTime();
            this.reserved = reservedArena.allocate(logConfig.getFileBuffer());
            this.handlers = createLogHandlers(logConfig);
            openNewLogOutputFile();
        }catch (IOException e) {
            throw new FrameworkException(ExceptionType.LOG, "Unable to create log file");
        }
    }

    @Override
    public void handle(LogEvent event) {
        if(event == LogEvent.flushEvent) {
            flushFile();
        }else if(event == LogEvent.shutdownEvent) {
            closeFile();
        }else {
            writeFile(event);
        }
    }


    /**
     *  Write and flush current byte-array into the file
     *  if timestamp or file-size exceed the limit, reallocate the output-stream
     */
    private void writeFile(LogEvent event) {
        try(WriteBuffer writeBuffer = WriteBuffer.newReservedWriteBuffer(reserved)) {
            for (LogHandler logHandler : handlers) {
                logHandler.process(writeBuffer, event);
            }
            long written = writeBuffer.writeIndex();
            if(written > Constants.ZERO) {
                lastEventTimestamp = event.timestamp();
                FileUtil.fwrite(writeBuffer.content(), fileStream);
                currentWrittenIndex += written;
                if((maxFileSize > Constants.ZERO && currentWrittenIndex > maxFileSize) || (maxRecordingTime > Constants.ZERO && lastEventTimestamp - allocationTimestamp > maxRecordingTime)) {
                    FileUtil.fflush(fileStream);
                    FileUtil.fclose(fileStream);
                    openNewLogOutputFile();
                }
            }
        }
    }

    private void flushFile() {
        if(lastFlushTimestamp < lastEventTimestamp) {
            FileUtil.fflush(fileStream);
            lastFlushTimestamp = lastEventTimestamp;
        }
    }


    /**
     *   Flush and close current file output-stream
     */
    private void closeFile() {
        if(lastFlushTimestamp < lastEventTimestamp) {
            FileUtil.fflush(fileStream);
        }
        FileUtil.fclose(fileStream);
        reservedArena.close();
    }


    /**
     *   Allocate a new file stream
     */
    private void openNewLogOutputFile() {
        try(Arena arena = Arena.ofConfined()) {
            LocalDateTime now = LocalDateTime.now();
            allocationTimestamp = now.toInstant(Constants.LOCAL_ZONE_OFFSET).toEpochMilli();
            String absolutePath = logDirPath +
                    Constants.SEPARATOR +
                    logFileName + "-" +
                    dateTimeFormatter.format(now) +
                    Constants.LOG_FILE_TYPE;
            if(Files.exists(Path.of(absolutePath))) {
                throw new FrameworkException(ExceptionType.LOG, "Target log file already exist");
            }
            MemorySegment path = arena.allocateUtf8String(absolutePath);
            MemorySegment mode = arena.allocateUtf8String("a");
            fileStream = FileUtil.fopen(path, mode);
            currentWrittenIndex = Constants.ZERO;
        }
    }

    private static List<LogHandler> createLogHandlers(LogConfig logConfig) {
        List<LogHandler> logHandlers = new ArrayList<>();
        byte[] format = logConfig.getLogFormat().getBytes(StandardCharsets.UTF_8);
        int index = Constants.ZERO;
        for( ; ; ) {
            index = searchNormalStr(format, index, logHandlers);
            if(index < Constants.ZERO) {
                logHandlers.add((writeBuffer, logEvent) -> {
                    writeBuffer.writeByte(Constants.LF);
                    byte[] throwable = logEvent.throwable();
                    if(throwable != null) {
                        writeBuffer.writeBytes(throwable);
                    }
                });
                return logHandlers;
            }else {
                index = searchFormattedStr(format, index, logHandlers);
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

    private static int searchFormattedStr(byte[] format, int startIndex, List<LogHandler> handlers) {
        int nextIndex = StringUtil.searchStr(format, Constants.PERCENT, startIndex, s -> {
            LogHandler handler = switch (s) {
                case "time" -> (writeBuffer, logEvent) -> writeBuffer.writeBytes(logEvent.time());
                case "level" -> (writeBuffer, logEvent) -> writeBuffer.writeBytes(logEvent.level());
                case "className" -> (writeBuffer, logEvent) -> writeBuffer.writeBytes(logEvent.className());
                case "threadName" -> (writeBuffer, logEvent) -> writeBuffer.writeBytes(logEvent.threadName());
                case "msg" -> (writeBuffer, logEvent) -> writeBuffer.writeBytes(logEvent.msg());
                default -> throw new FrameworkException(ExceptionType.LOG, "Unresolved log format : %s".formatted(s));
            };
            handlers.add(handler);
        });
        if(nextIndex < Constants.ZERO) {
            throw new FrameworkException(ExceptionType.LOG, "Corrupted log format");
        }
        return nextIndex;
    }
}
