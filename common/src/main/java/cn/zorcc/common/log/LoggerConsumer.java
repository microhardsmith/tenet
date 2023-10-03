package cn.zorcc.common.log;

import cn.zorcc.common.AbstractLifeCycle;
import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ConfigUtil;
import cn.zorcc.common.util.ThreadUtil;
import cn.zorcc.common.wheel.Wheel;

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
    public LoggerConsumer(LogConfig logConfig) {
        if(instanceFlag.compareAndSet(false, true)) {
            this.consumerThread = createConsumerThread(logConfig);
        }else {
            throw new FrameworkException(ExceptionType.LOG, Constants.UNREACHED);
        }
    }

    public LoggerConsumer() {
        this(ConfigUtil.loadJsonConfig(Constants.DEFAULT_LOG_CONFIG_NAME, LogConfig.class));
    }

    private static Thread createConsumerThread(LogConfig logConfig) {
        return ThreadUtil.platform("tenet-log", () -> {
            List<Consumer<LogEvent>> handlers = createEventHandlerList(logConfig);
            TransferQueue<LogEvent> queue = Logger.queue();
            Wheel.wheel().addPeriodicJob(() -> {
                if (!queue.offer(LogEvent.FLUSH_EVENT)) {
                    throw new FrameworkException(ExceptionType.LOG, Constants.UNREACHED);
                }
            }, Duration.ZERO, Duration.ofMillis(logConfig.getFlushInterval()));
            try{
                for( ; ; ){
                    final LogEvent logEvent = queue.take();
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

    private static List<Consumer<LogEvent>> createEventHandlerList(LogConfig logConfig) {
        List<Consumer<LogEvent>> handlers = new ArrayList<>();
        ConsoleLogConfig consoleLogConfig = logConfig.getConsole();
        if(consoleLogConfig != null) {
            handlers.add(new ConsoleLogEventHandler(logConfig));
        }
        FileLogConfig fileLogConfig = logConfig.getFile();
        if(fileLogConfig != null) {
            handlers.add(new FileLogEventHandler(logConfig));
        }
        SqliteLogConfig sqliteLogConfig = logConfig.getSqlite();
        if(sqliteLogConfig != null) {
            handlers.add(new SqliteLogEventHandler(logConfig));
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
