package cn.zorcc.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.HeapBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.event.EventHandler;
import cn.zorcc.common.exception.FrameworkException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.BiConsumer;

public class FileLogEventHandler implements EventHandler<LogEvent> {
    private static final OpenOption[] options = new OpenOption[] {
            StandardOpenOption.CREATE_NEW,
            StandardOpenOption.SPARSE,
            StandardOpenOption.WRITE
    };
    private final String logDirPath;
    private final String logFileName;
    private final int maxFileSize;
    private final int maxRecordingTime;
    private final HeapBuffer buffer;
    private final BiConsumer<HeapBuffer, LogEvent> consumer;
    private boolean needsFlush;
    /**
     *   timestamp milli when last stream was allocated
     */
    private long timestamp;
    private OutputStream stream;
    private long index;
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
            if(maxFileSize > 0 && maxFileSize <= Constants.MB) {
                throw new FrameworkException(ExceptionType.LOG, "Max log file-size is too small, will possibly overflow");
            }
            this.maxRecordingTime = logConfig.getMaxRecordingTime() <= 0 ? -1 : logConfig.getMaxRecordingTime();
            this.buffer = new HeapBuffer(logConfig.getFileBuffer());
            this.consumer = parseLogFormat(logConfig.getLogFormat());
            allocateNewStream();
        }catch (IOException e) {
            throw new FrameworkException(ExceptionType.LOG, "Unable to create log file");
        }
    }

    /**
     *  将当前heap缓冲区的内容写入到文件中，如果会超过文件限额则重新指定文件流
     */
    private void writeAndFlush(HeapBuffer heapBuffer) {
        try{
            int bufferIndex = heapBuffer.index();
            if(maxFileSize > 0 && index + bufferIndex > maxFileSize) {
                stream.close();
                allocateNewStream();
            }
            stream.write(heapBuffer.array(), 0, bufferIndex);
            stream.flush();
            index = index + bufferIndex;
            heapBuffer.reset();
        }catch (IOException e) {
            throw new FrameworkException(ExceptionType.LOG, "Failed to write to file", e);
        }
    }

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS");
    /**
     *   分配新的日志文件
     */
    private void allocateNewStream() {
        try{
            LocalDateTime now = LocalDateTime.now();
            timestamp = now.toInstant(Constants.LOCAL_ZONE_OFFSET).toEpochMilli();
            Path path = Path.of(logDirPath +
                    Constants.SEPARATOR +
                    logFileName + "-" +
                    dateTimeFormatter.format(now) +
                    Constants.LOG_FILE_TYPE);
            if(Files.exists(path)) {
                throw new FrameworkException(ExceptionType.LOG, "Target log file already exist");
            }
            stream = Files.newOutputStream(path, options);
            index = 0L;
        }catch (IOException e) {
            throw new FrameworkException(ExceptionType.LOG, "IOException caught when allocate", e);
        }
    }

    public void closeStream() {
        try{
            stream.close();
        }catch (IOException e) {
            throw new FrameworkException(ExceptionType.LOG, "IOException caught when shutting down FileLogEventHandler");
        }
    }

    @Override
    public void handle(LogEvent event) {
        if(event.flush()) {
            if(needsFlush) {
                writeAndFlush(buffer);
                needsFlush = false;
            }
        }else {
            needsFlush = true;
            if(maxRecordingTime > 0 && event.timestamp() - timestamp > maxRecordingTime) {
                allocateNewStream();
            }
            consumer.accept(buffer, event);
        }
    }

    /**
     *   解析日志格式，生成lambda处理方法
     */
    public static BiConsumer<HeapBuffer, LogEvent> parseLogFormat(String logFormat) {
        BiConsumer<HeapBuffer, LogEvent> result = (writeBuffer, logEvent) -> {};
        byte[] bytes = logFormat.getBytes(StandardCharsets.UTF_8);
        for(int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            if(b == Constants.b10) {
                int j = i + 1;
                for( ; ; ) {
                    if(bytes[j] == Constants.b10) {
                        String s = new String(bytes, i + 1, j - i - 1);
                        switch (s) {
                            case "time" -> result = result.andThen((heapBuffer, logEvent) -> heapBuffer.write(logEvent.time()));
                            case "level" -> result = result.andThen((heapBuffer, logEvent) -> heapBuffer.write(logEvent.level()));
                            case "className" -> result = result.andThen((heapBuffer, logEvent) -> heapBuffer.write(logEvent.className()));
                            case "threadName" -> result = result.andThen((heapBuffer, logEvent) -> heapBuffer.write(logEvent.threadName()));
                            case "msg" -> result = result.andThen((heapBuffer, logEvent) -> heapBuffer.write(logEvent.msg()));
                            default -> throw new FrameworkException(ExceptionType.LOG, "Unresolved log format : %s".formatted(s));
                        }
                        break;
                    }else if(++j == bytes.length){
                        throw new FrameworkException(ExceptionType.LOG, "LogFormat corrupted");
                    }
                }
                i = j;
            }else {
                result = result.andThen((heapBuffer, logEvent) -> heapBuffer.write(b));
            }
        }
        // automatically add \n and throwable
        result = result.andThen((heapBuffer, logEvent) -> {
            heapBuffer.write(Constants.LF);
            byte[] throwable = logEvent.throwable();
            if(throwable != null) {
                heapBuffer.write(throwable);
            }
        });
        return result;
    }
}
