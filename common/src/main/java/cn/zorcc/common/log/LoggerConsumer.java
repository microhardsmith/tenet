package cn.zorcc.common.log;

import cn.zorcc.common.AbstractLifeCycle;
import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.structure.Allocator;
import cn.zorcc.common.structure.Wheel;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 *   Singleton log consumer, init a LoggerConsumer to start the whole log processing procedure
 */
public final class LoggerConsumer extends AbstractLifeCycle {
    private static final AtomicBoolean instanceFlag = new AtomicBoolean(false);
    private final Thread consumerThread;
    public LoggerConsumer() {
        if(instanceFlag.compareAndSet(false, true)) {
            LogConfig logConfig = Logger.getLogConfig();
            this.consumerThread = createConsumerThread(logConfig);
        }else {
            throw new FrameworkException(ExceptionType.LOG, Constants.UNREACHED);
        }
    }

    private static Thread createConsumerThread(LogConfig logConfig) {
        return Thread.ofPlatform().name("tenet-log").unstarted(() -> {
            try(Allocator allocator = Allocator.newDirectAllocator()) {
                MemorySegment reserved = allocator.allocate(ValueLayout.JAVA_BYTE, logConfig.getBufferSize());
                List<Consumer<LogEvent>> handlers = createEventHandlerList(logConfig, reserved);
                TransferQueue<LogEvent> queue = Logger.queue();
                Wheel.wheel().addPeriodicJob(() -> {
                    if (!queue.offer(LogEvent.FLUSH_EVENT)) {
                        throw new FrameworkException(ExceptionType.LOG, Constants.UNREACHED);
                    }
                }, Duration.ZERO, Duration.ofMillis(logConfig.getFlushInterval()));
                for( ; ; ){
                    LogEvent logEvent = queue.take();
                    handlers.forEach(consumer -> consumer.accept(logEvent));
                    if(logEvent == LogEvent.SHUTDOWN_EVENT) {
                        break;
                    }
                }
            }catch (InterruptedException e) {
                throw new FrameworkException(ExceptionType.LOG, Constants.UNREACHED, e);
            }
        });
    }

    private static List<Consumer<LogEvent>> createEventHandlerList(LogConfig logConfig, MemorySegment reserved) {
        List<Consumer<LogEvent>> handlers = new ArrayList<>();
        ConsoleLogConfig consoleLogConfig = logConfig.getConsole();
        if(consoleLogConfig != null) {
            handlers.add(new ConsoleLogEventHandler(logConfig, reserved));
        }
        FileLogConfig fileLogConfig = logConfig.getFile();
        if(fileLogConfig != null) {
            handlers.add(new FileLogEventHandler(logConfig, reserved));
        }
        SqliteLogConfig sqliteLogConfig = logConfig.getSqlite();
        if(sqliteLogConfig != null) {
            handlers.add(new SqliteLogEventHandler(logConfig, reserved));
        }
        return Collections.unmodifiableList(handlers);
    }

    @Override
    public void doInit() {
        consumerThread.start();
    }

    @Override
    public void doExit() throws InterruptedException {
        if (!Logger.queue().offer(LogEvent.SHUTDOWN_EVENT)) {
            throw new FrameworkException(ExceptionType.LOG, Constants.UNREACHED);
        }
        consumerThread.join();
    }
}
