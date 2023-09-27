package cn.zorcc.common.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.binding.TenetBinding;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.FileUtil;
import cn.zorcc.common.util.NativeUtil;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 *   Print log to the file,
 */
public final class FileLogEventHandler implements Consumer<LogEvent> {
    private final List<LogHandler> handlers;
    private final List<LogEvent> eventList = new ArrayList<>();
    private final String dir;
    private final int flushThreshold;
    private final long maxFileSize;
    private final long maxRecordingTime;
    private final Arena reservedArena = Arena.ofConfined();
    private final MemorySegment reserved;
    private long currentCreateTime;
    private long currentWrittenIndex;
    private MemorySegment fileStream;
    public FileLogEventHandler(LogConfig logConfig) {
        try{
            FileLogConfig config = logConfig.getFile();
            handlers = Logger.createLogHandlers(logConfig.getLogFormat(), s -> switch (s) {
                case "time" -> (writeBuffer, logEvent) -> writeBuffer.writeSegment(logEvent.time());
                case "level" -> (writeBuffer, logEvent) -> writeBuffer.writeSegment(logEvent.level());
                case "className" -> (writeBuffer, logEvent) -> writeBuffer.writeSegment(logEvent.className());
                case "threadName" -> (writeBuffer, logEvent) -> writeBuffer.writeSegment(logEvent.threadName());
                case "msg" -> (writeBuffer, logEvent) -> writeBuffer.writeSegment(logEvent.msg());
                default -> throw new FrameworkException(ExceptionType.LOG, STR."Unresolved log format : \{s}");
            });
            dir = config.getDir() == null || config.getDir().isEmpty() ? System.getProperty("user.dir") : config.getDir();
            Path dirPath = Path.of(dir);
            if(!Files.exists(dirPath)) {
                Files.createDirectory(dirPath);
            }
            flushThreshold = config.getFlushThreshold() <= Constants.ZERO ? Integer.MIN_VALUE : config.getFlushThreshold();
            maxFileSize = config.getMaxFileSize() <= Constants.ZERO ? Long.MIN_VALUE : config.getMaxFileSize();
            maxRecordingTime = config.getMaxRecordingTime() <= Constants.ZERO ? Long.MIN_VALUE : config.getMaxRecordingTime();
            reserved = reservedArena.allocate(config.getBuffer());
            openNewLogOutputFile();
        }catch (IOException e) {
            throw new FrameworkException(ExceptionType.LOG, "Unable to create log file");
        }
    }

    private void openNewLogOutputFile() {
        if(fileStream != null) {
            FileUtil.fclose(fileStream);
        }
        try(Arena arena = Arena.ofConfined()) {
            Instant instant = Constants.SYSTEM_CLOCK.instant();
            LocalDateTime now = LocalDateTime.ofEpochSecond(instant.getEpochSecond(), instant.getNano(), Constants.LOCAL_ZONE_OFFSET);
                        String absolutePath = dir +
                    Constants.SEPARATOR +
                    DateTimeFormatter.ofPattern(Constants.LOG_FILE_NAME_PATTERN).format(now) +
                    Constants.LOG_FILE_TYPE;
            if(Files.exists(Path.of(absolutePath))) {
                throw new FrameworkException(ExceptionType.LOG, "Target log file already exist");
            }
            MemorySegment path = arena.allocateUtf8String(absolutePath);
            MemorySegment mode = arena.allocateUtf8String("a");
            fileStream = FileUtil.fopen(path, mode);
            if (FileUtil.setvbuf(fileStream, NativeUtil.NULL_POINTER, TenetBinding.nbf(), Constants.ZERO) != Constants.ZERO) {
                throw new FrameworkException(ExceptionType.LOG, "Failed to set filestream to nbf mode");
            }
            currentCreateTime = now.toInstant(Constants.LOCAL_ZONE_OFFSET).toEpochMilli();
            currentWrittenIndex = Constants.ZERO;
        }
    }

    @Override
    public void accept(LogEvent event) {
        switch (event.eventType()) {
            case Msg -> onMsg(event);
            case Flush -> flush();
            case Shutdown -> shutdown();
        }
    }

    private void onMsg(LogEvent event) {
        eventList.add(event);
        if(flushThreshold > Constants.ZERO && eventList.size() > flushThreshold) {
            flush();
        }
    }

    private void flush() {
        if(!eventList.isEmpty()) {
            try(WriteBuffer writeBuffer = WriteBuffer.newReservedWriteBuffer(reserved)) {
                for (LogEvent event : eventList) {
                    handlers.forEach(logHandler -> logHandler.process(writeBuffer, event));
                }
                FileUtil.fwrite(writeBuffer.toSegment(), fileStream);
                currentWrittenIndex += writeBuffer.writeIndex();
                if((maxFileSize > Constants.ZERO && currentWrittenIndex > maxFileSize) || (maxRecordingTime > Constants.ZERO && eventList.getLast().timestamp() - currentCreateTime > maxRecordingTime)) {
                    openNewLogOutputFile();
                }
            }finally {
                eventList.clear();
            }
        }
    }

    private void shutdown() {
        FileUtil.fclose(fileStream);
        reservedArena.close();
    }
}
