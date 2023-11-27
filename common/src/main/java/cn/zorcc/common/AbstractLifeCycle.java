package cn.zorcc.common;

import cn.zorcc.common.exception.FrameworkException;

import java.util.concurrent.atomic.AtomicInteger;

/**
 *   Abstract lifecycle interface implementation, the init() and exit() methods are limited to be only invoked once
 */
public abstract class AbstractLifeCycle implements LifeCycle {
    private final AtomicInteger state = new AtomicInteger(Constants.INITIAL);
    protected abstract void doInit();
    protected abstract void doExit() throws InterruptedException;

    @Override
    public void init() {
        if(state.compareAndSet(Constants.INITIAL, Constants.RUNNING)) {
            doInit();
        }else {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
        }
    }

    @Override
    public void exit() throws InterruptedException {
        if(state.compareAndSet(Constants.RUNNING, Constants.STOPPED)) {
            doExit();
        }else {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
        }
    }
}
