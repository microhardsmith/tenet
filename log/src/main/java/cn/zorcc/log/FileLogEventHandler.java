package cn.zorcc.log;

import cn.zorcc.common.BlockingQ;
import cn.zorcc.common.Constants;
import cn.zorcc.common.MpscBlockingQ;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.event.EventHandler;
import cn.zorcc.common.exception.FrameworkException;

import java.io.File;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentScope;
import java.lang.foreign.ValueLayout;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Set;

public class FileLogEventHandler implements EventHandler<LogEvent> {
    private static final String NAME = "logFileHandler";
    private static final Set<OpenOption> option = Set.of(StandardOpenOption.CREATE,
            StandardOpenOption.SPARSE,
            StandardOpenOption.READ,
            StandardOpenOption.WRITE);
    private final String logDirPath;
    private final String logFileName;
    private final int maxFileSize;
    private final int maxRecordingTime;
    private long timestamp;
    private FileChannel fc;
    private MemorySegment segment;
    private final BlockingQ<LogEvent> blockingQ;
    /**
     *  已写入日志字节数
     */
    private long index;
    public FileLogEventHandler(LogConfig logConfig) {
        String logDirPath = logConfig.getLogFileDir();
        if(logDirPath == null || logDirPath.isBlank()) {
            File defaultDir = new File(Constants.DEFAULT_LOG_DIR);
            if(defaultDir.exists()) {
                if(!defaultDir.isDirectory()) {
                    throw new FrameworkException(ExceptionType.LOG, "Target log dir has been occupied by normal file");
                }
            }else if(!defaultDir.mkdir()){
                throw new FrameworkException(ExceptionType.LOG, "Unable to create log dir");
            }
            this.logDirPath = defaultDir.getAbsolutePath();
        }else {
            this.logDirPath = logDirPath;
        }
        // set a few things
        this.logFileName = logConfig.getLogFileName();
        this.maxFileSize = logConfig.getMaxFileSize() <= 0 ? Constants.GB : logConfig.getMaxFileSize();
        if(maxFileSize <= Constants.MB) {
            throw new FrameworkException(ExceptionType.LOG, "Max log file-size is too small, will possibly overflow");
        }
        this.maxRecordingTime = logConfig.getMaxRecordingTime() <= 0 ? -1 : logConfig.getMaxRecordingTime();
        reAllocate();
        this.blockingQ = new MpscBlockingQ<>(NAME, logEvent -> {
            if(logEvent.isFlush()) {
                flush();
            }else {
                if(maxRecordingTime > 0 && logEvent.getTimestamp() - timestamp > maxRecordingTime) {
                    flush();
                    reAllocate();
                }
                final SegmentBuilder builder = logEvent.getBuilder();
                printToFile(builder.segment(), builder.index());
                final MemorySegment throwable = logEvent.getThrowable();
                if(throwable != null) {
                    printToFile(throwable, throwable.byteSize());
                }
            }
        });
    }

    /**
     *  将指定内存块的size大小的字节输入到文件
     */
    private void printToFile(MemorySegment memorySegment, long size) {
        if(index + size + 1>= maxFileSize) {
            flush();
            reAllocate();
        }
        final MemorySegment targetSegment = memorySegment.byteSize() == size ? memorySegment : memorySegment.asSlice(0, size);
        MemorySegment.copy(targetSegment, 0, segment, index, size);
        segment.set(ValueLayout.JAVA_BYTE, index + size, Constants.b9);
        index = index + size + 1;
    }

    /**
     *   重新分配日志文件
     */
    private void reAllocate() {
        try{
            this.fc.close();
            LocalDateTime now = LocalDateTime.now();
            this.timestamp = now.toInstant(Constants.LOCAL_ZONE_OFFSET).toEpochMilli();
            String path = logDirPath +
                    Constants.SEPARATOR +
                    logFileName + "-" +
                    Constants.FORMATTER.format(now) +
                    Constants.LOG_FILE_TYPE;
            if(new File(path).exists()) {
                throw new FrameworkException(ExceptionType.LOG, "Target log file already exist");
            }
            this.fc = FileChannel.open(Path.of(path), option);
            this.segment = fc.map(FileChannel.MapMode.READ_WRITE, 0, maxFileSize, SegmentScope.auto());
            this.index = 0L;
        }catch (IOException e) {
            throw new FrameworkException(ExceptionType.LOG, "IOException caught when allocate", e);
        }
    }

    /**
     *  刷新文件缓冲区
     */
    private void flush() {
        this.segment.force();
    }

    @Override
    public void init() {
        this.blockingQ.start();
    }

    @Override
    public void shutdown() {
        try{
            this.blockingQ.shutdown();
            this.fc.close();
        }catch (IOException e) {
            throw new FrameworkException(ExceptionType.LOG, "IOException caught when shutting down FileLogEventHandler");
        }

    }

    @Override
    public void handle(LogEvent event) {
        this.blockingQ.put(event);
    }
}
