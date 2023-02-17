package cn.zorcc.log;

import cn.zorcc.common.BlockingQ;
import cn.zorcc.common.Constants;
import cn.zorcc.common.MpscBlockingQ;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.event.EventHandler;
import cn.zorcc.common.exception.FrameworkException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 *  日志文件记录,将日志采用追加写的方式写入本地磁盘中
 */
public class FileLogEventHandler implements EventHandler<LogEvent> {
    private static final String NAME = "logFileHandler";
    /**
     *  日志文件名映射
     */
    private final Map<Integer, String> fileNameMap = new HashMap<>();
    /**
     *  日志文件夹地址
     */
    private final String logDirPath;
    /**
     *  日志文件地址
     */
    private final String logFilePath;
    /**
     *  日志文件名前缀
     */
    private final String prefix;
    /**
     *  滚动更新阈值
     */
    private final int rollingCount;
    /**
     *  最大单个日志文件大小
     */
    private final int maxFileSize;
    /**
     *  日志输出流
     */
    private OutputStream fileOutputStream;
    /**
     *  当前日志文件Index
     */
    private int currentIndex;
    /**
     *  当前日志文件大小
     */
    private long fileSize = 0L;
    /**
     *  上次Flush时文件大小
     */
    private long flushSize = 0L;
    /**
     *  日志消费者阻塞队列
     */
    private final BlockingQ<LogEvent> blockingQ;
    public FileLogEventHandler(LogConfig logConfig) {
        try{
            String logDirPath = logConfig.getLogFileDir();
            if(logDirPath == null || logDirPath.equals(Constants.EMPTY_STRING)) {
                File defaultDir = new File(Constants.DEFAULT_LOG_DIR);
                if((!defaultDir.exists() || !defaultDir.isDirectory()) && defaultDir.mkdir()) {
                    throw new FrameworkException(ExceptionType.LOG, "Unable to create log dir");
                }
                this.logDirPath = defaultDir.getAbsolutePath();
            }else {
                this.logDirPath = logDirPath;
            }
            String fileName = logConfig.getLogFileName();
            if(fileName == null || fileName.equals(Constants.EMPTY_STRING)) {
                fileName = Constants.DEFAULT_LOG_FILE_NAME.concat(Constants.LOG_FILE_TYPE);
            }else if(!fileName.contains(Constants.LOG_FILE_TYPE)){
                fileName = fileName.concat(Constants.LOG_FILE_TYPE);
            }
            this.logFilePath = this.logDirPath + Constants.SEPARATOR + fileName;
            this.prefix = fileName.substring(0, fileName.indexOf(Constants.LOG_FILE_TYPE)).concat("-");
            currentIndex = 0;
            File dir = new File(this.logDirPath);
            File[] files = dir.listFiles();
            if(files != null) {
                int biggestIndex = 0;
                for (File file : files) {
                    String name = file.getName();
                    if(name.startsWith(prefix)) {
                        currentIndex++;
                        int start = name.indexOf(prefix);
                        int end = name.indexOf(".");
                        int index = Integer.parseInt(name.substring(start + prefix.length(), end));
                        biggestIndex = Math.max(biggestIndex, index);
                    }
                }
                if(biggestIndex > currentIndex) {
                    throw new FrameworkException(ExceptionType.LOG, "Log files corrupted, Index out of range");
                }
            }
            this.fileOutputStream = new BufferedOutputStream(new FileOutputStream(logFilePath, true));
            this.maxFileSize = logConfig.getMaxFileSize() * Constants.KB;
            if(Boolean.TRUE.equals(logConfig.isEnableRolling())) {
                this.rollingCount = logConfig.getRollingFileCount();
            }else {
                this.rollingCount = -1;
            }
            this.blockingQ = new MpscBlockingQ<>(NAME, logEvent -> {
                try{
                    byte[] utf8FormattedMsg = logEvent.getLine().getBytes(StandardCharsets.UTF_8);
                    fileSize += utf8FormattedMsg.length;
                    if(fileSize > maxFileSize) {
                        archiveLog();
                    }
                    fileOutputStream.write(utf8FormattedMsg);
                    if(fileSize - flushSize > Constants.PAGE_SIZE) {
                        fileOutputStream.flush();
                        flushSize = fileSize;
                    }
                }catch (IOException e) {
                    throw new FrameworkException(ExceptionType.LOG, "IOException caught while writing log into file", e);
                }
            });
        }catch (IOException e) {
            throw new FrameworkException(ExceptionType.LOG, "IOException caught while initializing FileLogEventHandler", e);
        }
    }

    @Override
    public void init() {
        blockingQ.start();
    }

    @Override
    public void handle(LogEvent event) {
        blockingQ.put(event);
    }

    @Override
    public void shutdown() {
        blockingQ.shutdown();
    }

    /**
     *  对日志进行归档,如果是滚动更新且已达到滚动文件数最大限制会删除最旧的存档
     *  app.log会被重命名为app-1.log,依次类推,最新日志始终会被写入至app.log
     */
    private void archiveLog() {
        try{
            fileOutputStream.flush();
            fileOutputStream.close();
            int index = currentIndex;
            if(rollingCount != -1 && currentIndex == rollingCount) {
                // 删除最旧的存档
                File oldestFile = new File(getLogFile(index));
                if (!oldestFile.delete()) {
                    throw new FrameworkException(ExceptionType.LOG, "Unable to delete oldest log file");
                }
                index--;
            }
            for(int i = index; i >= 0; i--) {
                File oldFile = new File(getLogFile(i));
                File newFile = new File(getLogFile(i + 1));
                if (oldFile.renameTo(newFile)) {
                    throw new FrameworkException(ExceptionType.LOG, "Unable to rename old archive log file");
                }
            }
            this.fileOutputStream = new FileOutputStream(logFilePath, true);
            this.fileSize = 0L;
            this.flushSize = 0L;
        }catch (IOException e) {
            throw new FrameworkException(ExceptionType.LOG, "IOException caught when writing log into file", e);
        }
    }

    /**
     *  获取指定序号的日志文件的文件名
     */
    private String getLogFile(int index) {
        return fileNameMap.computeIfAbsent(index, k -> k == 0 ? logFilePath : logDirPath +
                Constants.SEPARATOR +
                prefix +
                k +
                Constants.LOG_FILE_TYPE);
    }
}
