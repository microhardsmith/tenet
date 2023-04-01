package cn.zorcc.common;

import java.util.concurrent.TimeUnit;

public interface ObjPool<T> {

    /**
     *   Blocking get object
     */
    T get();

    /**
     *   Blocking get object with max timeout
     */
    T get(long timeout, TimeUnit timeUnit);

    /**
     *   release a object back to pool
     */
    void release(T t);

    /**
     *   add a object to current pool
     */
    void add(T t);

    /**
     *   return current pool's object counts
     */
    int count();
}
