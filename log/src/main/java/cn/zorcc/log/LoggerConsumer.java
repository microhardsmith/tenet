package cn.zorcc.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.LifeCycle;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.event.EventHandler;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ThreadUtil;
import cn.zorcc.common.wheel.Wheel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *   singleton log consumer
 */
public final class LoggerConsumer implements LifeCycle {
    private static final String NAME = "tenet-log";
    private static final AtomicBoolean instanceFlag = new AtomicBoolean(false);
    private final LogConfig logConfig;
    private final TransferQueue<LogEvent> queue;
    private final List<EventHandler<LogEvent>> handlers = new ArrayList<>();
    private final Thread consumerThread;
    public LoggerConsumer() {
        if(instanceFlag.compareAndSet(false, true)) {
            this.logConfig = Logger.config();
            this.queue = Logger.queue();
            this.consumerThread = ThreadUtil.platform(NAME, () -> {
                // initializing log handlers
                if(logConfig.isUsingConsole()) {
                    handlers.add(new ConsoleLogEventHandler(logConfig));
                }
                if(logConfig.isUsingFile()) {
                    handlers.add(new FileLogEventHandler(logConfig));
                }
                if(logConfig.isUsingMetrics()) {
                    handlers.add(new MetricsLogEventHandler(logConfig));
                }
                // add periodic flush job
                if(logConfig.isUsingFile() || logConfig.isUsingMetrics()) {
                    Wheel.wheel().addPeriodicJob(() -> {
                        if (!queue.offer(LogEvent.flushEvent)) {
                            throw new FrameworkException(ExceptionType.LOG, Constants.UNREACHED);
                        }
                    }, 0L, logConfig.getFlushInterval(), TimeUnit.MILLISECONDS);
                }
                // start consuming
                Thread currentThread = Thread.currentThread();
                try{
                    while (!currentThread.isInterrupted()) {
                        LogEvent logEvent = queue.take();
                        if(logEvent == LogEvent.shutdownEvent) {
                            break;
                        }
                        for (EventHandler<LogEvent> handler : handlers) {
                            handler.handle(logEvent);
                        }
                    }
                }catch (InterruptedException e) {
                    currentThread.interrupt();
                }
            });
        }else {
            throw new FrameworkException(ExceptionType.LOG, "LoggerConsumer could only have a single instance");
        }
    }

    @Override
    public void init() {
        consumerThread.start();
    }

    @Override
    public void shutdown() {
        try{
            if (!queue.offer(LogEvent.shutdownEvent)) {
                throw new FrameworkException(ExceptionType.LOG, Constants.UNREACHED);
            }
            consumerThread.join();
            for (EventHandler<LogEvent> handler : handlers) {
                // handle a flush event for the last time
                handler.handle(LogEvent.flushEvent);
                if(handler instanceof FileLogEventHandler fileLogEventHandler) {
                    fileLogEventHandler.closeStream();
                }
            }
        }catch (InterruptedException e) {
            throw new FrameworkException(ExceptionType.NETWORK, "Shutting down Log failed because of thread interruption");
        }
    }
}
