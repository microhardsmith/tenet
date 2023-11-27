package cn.zorcc.common.structure;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *   Mutex is just like ReentrantLock, but in try-with-resources format to provide better readability
 */
public final class Mutex implements AutoCloseable {
    private final Lock lock = new ReentrantLock();

    public Mutex acquire() {
        lock.lock();
        return this;
    }

    @Override
    public void close() {
        lock.unlock();
    }
}
