package cn.zorcc.common;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * 对象池实现,等ScopedValue稳定后可以考虑替代
 */
public class Pool<T> {
    /**
     * 存放对象的并发队列
     */
    private final BlockingQueue<T> queue;

    public Pool(int capacity) {
        this.queue = new ArrayBlockingQueue<>(capacity);
    }

    /**
     *  填充对象池,填充次数建议与capacity保持一致
     */
    public void offer(T obj) {
        if (!queue.offer(obj)) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Unable to fill the ObjPool with instance");
        }
    }

    /**
     * 从对象池中获取一个对象
     */
    public T obtain() {
        try{
            return queue.take();
        }catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FrameworkException(ExceptionType.CONTEXT, "Thread interrupt", e);
        }
    }

    /**
     * 将对象释放,使其回到对象池
     */
    public void free(T obj) {
        try{
            queue.put(obj);
        }catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FrameworkException(ExceptionType.CONTEXT, "thread interrupt", e);
        }
    }
}
