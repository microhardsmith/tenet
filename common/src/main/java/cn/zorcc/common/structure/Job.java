package cn.zorcc.common.structure;

public interface Job {

    /**
     *   Return current job execution count, this function is only useful for periodic jobs
     */
    long count();

    /**
     *   Cancel current job execution, return if successfully canceled
     *   Note that cancel itself is an atomic operation, the canceller thread shouldn't be the execution thread
     *   Different threads are actually compete for the opportunity of either running or canceling, with only one would succeed
     */
    boolean cancel();
}
