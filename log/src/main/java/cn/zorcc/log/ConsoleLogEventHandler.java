package cn.zorcc.log;

import cn.zorcc.common.BlockingQ;
import cn.zorcc.common.Constants;
import cn.zorcc.common.MpscBlockingQ;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.event.EventHandler;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

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
    private int counter;

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
            // Console output will ignore the flush event since it flushes every time
            if(!logEvent.isFlush()) {
                builder.reset();
                MemorySegment level = logEvent.getLevel();
                builder.append(logEvent.getTime(), timeColor)
                        .append(Constants.b2)
                        .append(level, getLevelColorSegment(level), levelLen)
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
                puts(builder.segment());
                flush();
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

    /**
     *  获取当前日志等级对应的颜色
     */
    private MemorySegment getLevelColorSegment(MemorySegment level) {
        if(level.equals(Constants.ERROR_SEGMENT)) {
            return Constants.RED_SEGMENT;
        }else if(level.equals(Constants.WARN_SEGMENT)) {
            return Constants.YELLOW_SEGMENT;
        }else {
            return Constants.GREEN_SEGMENT;
        }
    }

    private MemorySegment getColorSegment(String color) {
        switch (color) {
            case Constants.RED -> {
                return Constants.RED_SEGMENT;
            }
            case Constants.GREEN -> {
                return Constants.GREEN_SEGMENT;
            }
            case Constants.YELLOW -> {
                return Constants.YELLOW_SEGMENT;
            }
            case Constants.BLUE -> {
                return Constants.BLUE_SEGMENT;
            }
            case Constants.MAGENTA -> {
                return Constants.MAGENTA_SEGMENT;
            }
            case Constants.CYAN -> {
                return Constants.CYAN_SEGMENT;
            }
            default -> throw new FrameworkException(ExceptionType.LOG, "Unable to get target color");
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
        flush();
    }
}
