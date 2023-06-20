package cn.zorcc.common.log;

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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *   Singleton log consumer, init a LoggerConsumer to start the whole log processing procedure
 */
public final class LoggerConsumer implements LifeCycle {
    private static final String NAME = "tenet-log";
    private static final AtomicBoolean instanceFlag = new AtomicBoolean(false);
    private final Thread consumerThread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Lock lock = new ReentrantLock();
    public LoggerConsumer() {
        if(instanceFlag.compareAndSet(false, true)) {
            this.consumerThread = createConsumerThread(Logger.config());
        }else {
            throw new FrameworkException(ExceptionType.LOG, Constants.UNREACHED);
        }
    }

    private static Thread createConsumerThread(LogConfig logConfig) {
            return ThreadUtil.platform(NAME, () -> {
                List<EventHandler<LogEvent>> handlers = new ArrayList<>();
                TransferQueue<LogEvent> queue = Logger.queue();
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
                // Add periodic flush job
                if(logConfig.isUsingFile() || logConfig.isUsingMetrics()) {
                    Wheel.wheel().addPeriodicJob(() -> {
                        if (!queue.offer(LogEvent.flushEvent)) {
                            throw new FrameworkException(ExceptionType.LOG, Constants.UNREACHED);
                        }
                    }, 0L, logConfig.getFlushInterval(), TimeUnit.MILLISECONDS);
                }
                // Start consuming logEvent
                try{
                    for( ; ; ){
                        LogEvent logEvent = queue.take();
                        for (EventHandler<LogEvent> handler : handlers) {
                            handler.handle(logEvent);
                        }
                        if(logEvent == LogEvent.shutdownEvent) {
                            break;
                        }
                    }
                }catch (InterruptedException e) {
                    throw new FrameworkException(ExceptionType.LOG, Constants.UNREACHED, e);
                }
            });
    }

    @Override
    public void init() {
        lock.lock();
        try {
            if(running.compareAndSet(false, true)) {
                consumerThread.start();
            }
        }finally {
            lock.unlock();
        }
    }

    @Override
    public void shutdown() {
        lock.lock();
        try{
            if(running.compareAndSet(true, false)) {
                if (!Logger.queue().offer(LogEvent.shutdownEvent)) {
                    throw new FrameworkException(ExceptionType.LOG, Constants.UNREACHED);
                }
                consumerThread.join();
            }
        }catch (InterruptedException e) {
            throw new FrameworkException(ExceptionType.NETWORK, "Shutting down Log failed because of thread interruption");
        }finally {
            lock.unlock();
        }
    }
}
