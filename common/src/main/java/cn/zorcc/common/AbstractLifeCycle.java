package cn.zorcc.common;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractLifeCycle implements LifeCycle {
    private static final int INITIAL = 0;
    private static final int RUNNING = 1;
    private static final int SHUTDOWN = 2;
    private final AtomicInteger state = new AtomicInteger(INITIAL);
    protected abstract void doInit();
    protected abstract void doExit() throws InterruptedException;

    @Override
    public void init() {
        if(state.compareAndSet(INITIAL, RUNNING)) {
            doInit();
        }else {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
        }
    }

    @Override
    public void exit() throws InterruptedException {
        if(state.compareAndSet(RUNNING, SHUTDOWN)) {
            doExit();
        }else {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
        }
    }
}
