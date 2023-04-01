package cn.zorcc.log;

import cn.zorcc.common.SegmentBuilder;
import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.event.EventHandler;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;
import cn.zorcc.common.util.ThreadUtil;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;

/**
 * 将日志打印至控制台
 */
public class ConsoleLogEventHandler implements EventHandler<LogEvent> {
    private static final String NAME = "logConsoleHandler";
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
    private final BlockingQueue<LogEvent> queue;
    private final Thread thread;
    private final MethodHandle printHandle;
    private final SegmentBuilder builder;

    public ConsoleLogEventHandler(LogConfig logConfig) {
        // Console builder should have a larger size than logEvent
        this.builder = new SegmentBuilder(logConfig.getBufferSize() << 1);
        SymbolLookup symbolLookup = NativeUtil.loadLibraryFromResource(NativeUtil.commonLib());
        this.printHandle = NativeUtil.methodHandle(symbolLookup, "g_print", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

        this.levelLen = logConfig.getLevelLen();
        this.threadNameLen = logConfig.getThreadNameLen();
        this.classNameLen = logConfig.getClassNameLen();
        this.timeColor = getColorSegment(logConfig.getTimeColor());
        this.threadNameColor = getColorSegment(logConfig.getThreadNameColor());
        this.classNameColor = getColorSegment(logConfig.getClassNameColor());
        this.msgColor = getColorSegment(logConfig.getMsgColor());

        this.queue = new LinkedTransferQueue<>();
        this.thread = ThreadUtil.virtual(NAME, () -> {
            Thread currentThread = Thread.currentThread();
            while (!currentThread.isInterrupted()) {
                try{
                    LogEvent logEvent = queue.take();
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
                        // add '\0'
                        builder.append(Constants.b0);
                        // print to the console
                        print(builder.segment());
                    }
                }catch (InterruptedException e) {
                    currentThread.interrupt();
                }
            }
        });
    }

    /**
     *  向标准输出流打印数据
     */
    private void print(MemorySegment memorySegment) {
        try{
            printHandle.invokeExact(memorySegment);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.LOG, "Exception caught when invoking puts()", throwable);
        }
    }

    /**
     *   获取当前日志等级对应的MemorySegment
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

    /**
     *   获取颜色对应的MemorySegment
     */
    private static MemorySegment getColorSegment(String color) {
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
            default -> {
                // no color wrapped
                return null;
            }
        }
    }

    @Override
    public void init() {
        thread.start();
    }

    @Override
    public void handle(LogEvent event) {
        try {
            queue.put(event);
        } catch (InterruptedException e) {
            thread.interrupt();
        }
    }

    @Override
    public void shutdown() {
        thread.interrupt();
    }
}
