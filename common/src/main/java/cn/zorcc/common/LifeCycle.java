package cn.zorcc.common;

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
    void shutdown() throws InterruptedException;
}
