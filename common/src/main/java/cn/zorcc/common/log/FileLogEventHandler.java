package cn.zorcc.common.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ResizableByteArray;
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
import java.util.ArrayList;
import java.util.List;

/**
 *   Print log to the file,
 */
public final class FileLogEventHandler implements EventHandler<LogEvent> {
    private static final OpenOption[] options = new OpenOption[] {
            StandardOpenOption.CREATE_NEW,
            StandardOpenOption.SPARSE,
            StandardOpenOption.WRITE
    };
    private final String logDirPath;
    private final String logFileName;
    private final int maxFileSize;
    private final int maxRecordingTime;
    private final ResizableByteArray buffer;
    private final List<IOConsumer> consumers;
    /**
     *   Timestamp milli when last stream was allocated
     */
    private long allocMilli;
    /**
     *   Timestamp milli when last log event occur
     */
    private long eventMilli;
    /**
     *   Current file output stream
     */
    private OutputStream stream;
    /**
     *   File write index
     */
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
            this.buffer = new ResizableByteArray(logConfig.getFileBuffer());
            this.consumers = createIOConsumer(logConfig.getLogFormat());
            allocateNewStream();
        }catch (IOException e) {
            throw new FrameworkException(ExceptionType.LOG, "Unable to create log file");
        }
    }

    /**
     *  Write and flush current byte-array into the file
     *  if timestamp or filesize exceed the limit, reallocate the output-stream
     */
    private void writeAndFlush() {
        try{
            int bufferIndex = buffer.writeIndex();
            if(bufferIndex > 0) {
                if((maxFileSize > 0 && index + bufferIndex > maxFileSize) || (maxRecordingTime > 0 && eventMilli - allocMilli > maxRecordingTime)) {
                    stream.close();
                    allocateNewStream();
                }
                stream.write(buffer.array(), 0, bufferIndex);
                stream.flush();
                index = index + bufferIndex;
                buffer.reset();
            }
        }catch (IOException e) {
            throw new FrameworkException(ExceptionType.LOG, "Failed to write to file", e);
        }
    }

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS");
    /**
     *   Allocate a new file output-stream
     */
    private void allocateNewStream() {
        try{
            LocalDateTime now = LocalDateTime.now();
            allocMilli = now.toInstant(Constants.LOCAL_ZONE_OFFSET).toEpochMilli();
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

    /**
     *   Flush and close current file output-stream
     */
    private void flushAndClose() {
        try{
            int bufferIndex = buffer.writeIndex();
            if(bufferIndex > 0) {
                stream.write(buffer.array(), 0, bufferIndex);
                stream.flush();
            }
            stream.close();
        }catch (IOException e) {
            throw new FrameworkException(ExceptionType.LOG, "IOException caught when shutting down FileLogEventHandler");
        }
    }

    @Override
    public void handle(LogEvent event) {
        if(event == LogEvent.flushEvent) {
            writeAndFlush();
        }else if(event == LogEvent.shutdownEvent) {
            flushAndClose();
        }else {
            eventMilli = event.timestamp();
            for (IOConsumer consumer : consumers) {
                consumer.accept(buffer, event);
            }
        }
    }

    @FunctionalInterface
    interface IOConsumer {
        void accept(ResizableByteArray buffer, LogEvent event);
    }

    /**
     *   Parsing log-format to a lambda consumer list
     */
    private static List<IOConsumer> createIOConsumer(String logFormat) {
        List<IOConsumer> result = new ArrayList<>();
        ResizableByteArray arr = new ResizableByteArray(Constants.KB);
        byte[] bytes = logFormat.getBytes(StandardCharsets.UTF_8);
        for(int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            if(b == Constants.b10) {
                if(arr.writeIndex() > 0) {
                    final byte[] array = arr.toArray();
                    result.add((resizableByteArray, logEvent) -> resizableByteArray.write(array));
                    arr.reset();
                }
                int j = i + 1;
                for( ; ; ) {
                    if(bytes[j] == Constants.b10) {
                        String s = new String(bytes, i + 1, j - i - 1);
                        switch (s) {
                            case "time" -> result.add((resizableByteArray, logEvent) -> resizableByteArray.write(logEvent.time()));
                            case "level" -> result.add((resizableByteArray, logEvent) -> resizableByteArray.write(logEvent.level()));
                            case "className" -> result.add((resizableByteArray, logEvent) -> resizableByteArray.write(logEvent.className()));
                            case "threadName" -> result.add((resizableByteArray, logEvent) -> resizableByteArray.write(logEvent.threadName()));
                            case "msg" -> result.add((resizableByteArray, logEvent) -> resizableByteArray.write(logEvent.msg()));
                            default -> throw new FrameworkException(ExceptionType.LOG, "Unresolved log format : %s".formatted(s));
                        }
                        break;
                    }else if(++j == bytes.length){
                        throw new FrameworkException(ExceptionType.LOG, "LogFormat corrupted");
                    }
                }
                i = j;
            }else {
                arr.write(b);
            }
        }
        if(arr.writeIndex() > 0) {
            final byte[] array = arr.toArray();
            result.add((resizableByteArray, logEvent) -> resizableByteArray.write(array));
            arr.reset();
        }
        // automatically add \n and throwable
        result.add((resizableByteArray, logEvent) -> {
            resizableByteArray.write(Constants.LF);
            byte[] throwable = logEvent.throwable();
            if(throwable != null) {
                resizableByteArray.write(throwable);
            }
        });
        return result;
    }
}
