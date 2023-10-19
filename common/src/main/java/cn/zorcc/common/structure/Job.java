package cn.zorcc.common.structure;

public interface Job {
    /**
     *   return current job execution count
     */
    long count();

    /**
     *   cancel current job execution, return if successfully canceled
     *   Note that cancel itself is a atomic operation, the canceller thread could never be the job execution thread
     *   so different threads are actually fight for the opportunity of either running or canceling, but only one would succeed
     */
    boolean cancel();
}
