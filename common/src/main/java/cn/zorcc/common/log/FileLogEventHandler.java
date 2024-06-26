package cn.zorcc.common.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.bindings.TenetBinding;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.structure.Allocator;
import cn.zorcc.common.structure.MemApi;
import cn.zorcc.common.structure.WriteBuffer;
import cn.zorcc.common.util.FileUtil;

import java.io.IOException;
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
 *   Print log to the file
 */
public final class FileLogEventHandler implements Consumer<LogEvent> {
    private final List<LogHandler> handlers;
    private final List<LogEvent> eventList = new ArrayList<>();
    private final String dir;
    private final int flushThreshold;
    private final long maxFileSize;
    private final long maxRecordingTime;
    private final MemorySegment reserved;
    private final MemApi memApi;
    private long currentCreateTime;
    private long currentWrittenIndex;
    private MemorySegment fileStream;
    public FileLogEventHandler(LogConfig logConfig, MemorySegment reserved, MemApi memApi) {
        try{
            FileLogConfig config = logConfig.getFile();
            List<LogHandler> logHandlers = Logger.createLogHandlers(logConfig.getLogFormat(), s -> switch (s) {
                case "time" -> (writeBuffer, logEvent) -> writeBuffer.writeSegment(logEvent.time());
                case "level" -> (writeBuffer, logEvent) -> writeBuffer.writeSegment(logEvent.level());
                case "className" -> (writeBuffer, logEvent) -> writeBuffer.writeSegment(logEvent.className());
                case "threadName" -> (writeBuffer, logEvent) -> writeBuffer.writeSegment(logEvent.threadName());
                case "msg" -> (writeBuffer, logEvent) -> writeBuffer.writeSegment(logEvent.msg());
                default -> throw new FrameworkException(ExceptionType.LOG, STR. "Unresolved log format : \{ s }" );
            });
            logHandlers.add((writeBuffer, logEvent) -> {
                MemorySegment throwable = logEvent.throwable();
                if(throwable != null) {
                    writeBuffer.writeSegment(throwable);
                }
            });
            this.handlers = logHandlers;
            this.dir = FileUtil.normalizePath(config.getDir() == null || config.getDir().isEmpty() ? System.getProperty("user.dir") : config.getDir());
            Path dirPath = Path.of(dir);
            if(!Files.exists(dirPath)) {
                Files.createDirectory(dirPath);
            }
            this.flushThreshold = config.getFlushThreshold() <= 0 ? Integer.MIN_VALUE : config.getFlushThreshold();
            this.maxFileSize = config.getMaxFileSize() <= 0 ? Long.MIN_VALUE : config.getMaxFileSize();
            this.maxRecordingTime = config.getMaxRecordingTime() <= 0 ? Long.MIN_VALUE : config.getMaxRecordingTime();
            this.reserved = reserved;
            this.memApi = memApi;
            openNewLogOutputFile();
        }catch (IOException e) {
            throw new FrameworkException(ExceptionType.LOG, "Unable to create log file");
        }
    }

    private void openNewLogOutputFile() {
        if(fileStream != null) {
            FileUtil.fclose(fileStream);
        }
        Instant instant = Constants.SYSTEM_CLOCK.instant();
        LocalDateTime now = LocalDateTime.ofEpochSecond(instant.getEpochSecond(), instant.getNano(), Constants.LOCAL_ZONE_OFFSET);
        String absolutePath = dir +
                Constants.SEPARATOR +
                DateTimeFormatter.ofPattern(Constants.LOG_FILE_NAME_PATTERN).format(now) +
                Constants.LOG_FILE_TYPE;
        if(Files.exists(Path.of(absolutePath))) {
            throw new FrameworkException(ExceptionType.LOG, "Target log file already exist");
        }
        try(Allocator allocator = Allocator.newDirectAllocator(memApi)) {
            MemorySegment path = allocator.allocateFrom(absolutePath);
            MemorySegment mode = allocator.allocateFrom("a");
            fileStream = FileUtil.fopen(path, mode);
            if (FileUtil.setvbuf(fileStream, MemorySegment.NULL, TenetBinding.nbf(), 0) != 0) {
                throw new FrameworkException(ExceptionType.LOG, "Failed to set filestream to nbf mode");
            }
            currentCreateTime = now.toInstant(Constants.LOCAL_ZONE_OFFSET).toEpochMilli();
            currentWrittenIndex = 0;
        }
    }

    @Override
    public void accept(LogEvent event) {
        switch (event.eventType()) {
            case Msg -> {
                eventList.add(event);
                if(flushThreshold > 0 && eventList.size() > flushThreshold) {
                    flush();
                    checkFile();
                }
            }
            case Flush -> {
                if(!eventList.isEmpty()) {
                    flush();
                    checkFile();
                }
            }
            case Shutdown -> {
                if(!eventList.isEmpty()) {
                    flush();
                }
                FileUtil.fclose(fileStream);
            }
        }
    }

    private void flush() {
        try(WriteBuffer writeBuffer = WriteBuffer.newReservedWriteBuffer(memApi, reserved)) {
            for (LogEvent event : eventList) {
                handlers.forEach(logHandler -> logHandler.process(writeBuffer, event));
            }
            FileUtil.fwrite(writeBuffer.asSegment(), fileStream);
            currentWrittenIndex += writeBuffer.writeIndex();
        }finally {
            eventList.clear();
        }
    }

    private void checkFile() {
        if((maxFileSize > 0 && currentWrittenIndex > maxFileSize)
                || (maxRecordingTime > 0 && eventList.getLast().timestamp() - currentCreateTime > maxRecordingTime)) {
            openNewLogOutputFile();
        }
    }
}
