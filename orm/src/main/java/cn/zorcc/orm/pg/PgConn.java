package cn.zorcc.orm.pg;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ThreadUtil;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *   Representing a basic postgresql connection
 */
public final class PgConn {
    /**
     *   Global counter for established postgresql connections
     */
    private static final AtomicInteger counter = new AtomicInteger(Constants.ZERO);
    private final AtomicBoolean available;
    private final BlockingQueue<Object> msgQueue;
    private final Thread consumerThread;
    private final PgVariable variable = new PgVariable();

    public PgConn(AtomicBoolean available, BlockingQueue<Object> msgQueue) {
        this.available = available;
        this.msgQueue = msgQueue;
        this.consumerThread = ThreadUtil.virtual("pgConn-" + counter.getAndIncrement(), () -> {
            Thread currentThread = Thread.currentThread();
            while (!currentThread.isInterrupted()) {
                try {
                    onMsg(msgQueue.take());
                } catch (InterruptedException e) {
                    currentThread.interrupt();
                }
            }
        });
        consumerThread.start();
    }

    private void onMsg(Object msg) {
        switch (msg) {

            default -> throw new FrameworkException(ExceptionType.POSTGRESQL, Constants.UNREACHED);
        }
    }

    public void stop() {
        consumerThread.interrupt();
    }
}
