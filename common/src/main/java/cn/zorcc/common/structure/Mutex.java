package cn.zorcc.common.structure;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *   Mutex is just like ReentrantLock, but in try-with-resources format to provide better readability
 */
public record Mutex(
        Lock lock
) implements AutoCloseable {

    public Mutex {
        if(lock == null) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
        }
    }

    public Mutex() {
        this(new ReentrantLock());
    }

    public Mutex acquire() {
        lock.lock();
        return this;
    }

    @Override
    public void close() {
        lock.unlock();
    }
}
