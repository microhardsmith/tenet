package cn.zorcc.common;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

/**
 *   Lifecycle interface for all stateful components
 */
public interface LifeCycle {
    /**
     *  Initialize a component
     */
    void init();
    /**
     *  Shutdown a component
     */
    void exit() throws InterruptedException;

    /**
     *  Shutdown a component with InterruptedException eaten
     */
    default void UninterruptibleExit() {
        try{
            exit();
        }catch (InterruptedException e) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Exit was interrupted");
        }
    }
}
