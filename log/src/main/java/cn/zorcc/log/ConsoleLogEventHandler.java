package cn.zorcc.log;

import cn.zorcc.common.BlockingQ;
import cn.zorcc.common.Constants;
import cn.zorcc.common.MpscBlockingQ;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.event.EventHandler;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;
import org.slf4j.event.Level;

import java.io.PrintStream;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 将日志打印至控制台
 */
public class ConsoleLogEventHandler implements EventHandler<LogEvent> {
    private static final String NAME = "logConsoleHandler";
    private static final MemorySegment NORMAL = MemorySegment.ofArray(Constants.GREEN.getBytes(StandardCharsets.UTF_8));
    private static final MemorySegment WARN = MemorySegment.ofArray(Constants.YELLOW.getBytes(StandardCharsets.UTF_8));
    private static final MemorySegment ERROR = MemorySegment.ofArray(Constants.RED.getBytes(StandardCharsets.UTF_8));
    /**
     *  日志等级Console输出预留长度
     */
    private final long levelLen;
    /**
     *  线程名Console输出预留长度
     */
    private final long threadNameLen;
    /**
     *  类名Console输出预留长度
     */
    private final long classNameLen;
    /**
     *  打印日志时间的颜色
     */
    private final MemorySegment timeColor;
    /**
     *  打印日志线程名的颜色
     */
    private final MemorySegment threadNameColor;
    /**
     *  打印日志类名的颜色
     */
    private final MemorySegment classNameColor;
    /**
     *  打印日志消息的颜色
     */
    private final MemorySegment msgColor;
    /**
     *  日志消费者阻塞队列
     */
    private final BlockingQ<LogEvent> blockingQ;
    private final MethodHandle putsHandle;
    private final MethodHandle flushHandle;
    private final Arena arena;
    private final SegmentBuilder builder;

    public ConsoleLogEventHandler(LogConfig logConfig) {
        // console builder should have a larger size than logEvent
        this.arena = Arena.openConfined();
        this.builder = new SegmentBuilder(arena, logConfig.getBufferSize() << 1);
        SymbolLookup symbolLookup = NativeUtil.loadLibraryFromResource(NativeUtil.commonLib());
        this.putsHandle = NativeUtil.methodHandle(symbolLookup, "g_puts", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        this.flushHandle = NativeUtil.methodHandle(symbolLookup, "g_flush", FunctionDescriptor.ofVoid());

        this.levelLen = logConfig.getLevelLen();
        this.threadNameLen = logConfig.getThreadNameLen();
        this.classNameLen = logConfig.getClassNameLen();
        this.timeColor = MemorySegment.ofArray(logConfig.getTimeColor().getBytes(StandardCharsets.UTF_8));
        this.threadNameColor = MemorySegment.ofArray(logConfig.getThreadNameColor().getBytes(StandardCharsets.UTF_8));
        this.classNameColor = MemorySegment.ofArray(logConfig.getClassNameColor().getBytes(StandardCharsets.UTF_8));
        this.msgColor = MemorySegment.ofArray(logConfig.getMsgColor().getBytes(StandardCharsets.UTF_8));

        this.blockingQ = new MpscBlockingQ<>(NAME, logEvent -> {
            if(logEvent.isFlush()) {
                // need to flush the buffer
                flush();
            }else {
                builder.reset();
                MemorySegment level = logEvent.getLevel();
                builder.append(logEvent.getLogTime().segment(), timeColor)
                        .append(level, getLevelColor(level), levelLen)
                        .append(Constants.b2)
                        .append(Constants.b7)
                        .append(logEvent.getThreadName(), threadNameColor, threadNameLen)
                        .append(Constants.b8)
                        .append(Constants.b2)
                        .append(logEvent.getClassName(), classNameColor, classNameLen)
                        .append(Constants.b2)
                        .append(Constants.b1)
                        .append(Constants.b2).append(logEvent.getMsg(), msgColor);
                MemorySegment throwable = logEvent.getThrowable();
                if(throwable != null) {
                    builder.append(Constants.b9).append(throwable);
                }
                builder.append(Constants.b0);
                // DEBUG
                // puts(builder.segment());
            }
        });
    }

    /**
     *  向标准输出流打印数据
     */
    private void puts(MemorySegment memorySegment) {
        try{
            putsHandle.invokeExact(memorySegment);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.LOG, "Exception caught when invoking puts()", throwable);
        }
    }

    /**
     *  刷新当前缓冲区
     */
    private void flush() {
        try{
            flushHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.LOG, "Exception caught when invoking flush()", throwable);
        }
    }

    private MemorySegment getLevelColor(MemorySegment level) {
        if(level.equals(Constants.ERROR_SEGMENT)) {
            return ERROR;
        }else if(level.equals(Constants.WARN_SEGMENT)) {
            return WARN;
        }else {
            return NORMAL;
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
}
