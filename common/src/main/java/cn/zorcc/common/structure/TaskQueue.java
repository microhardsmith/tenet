package cn.zorcc.common.structure;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *   Task queue is a queue for multiple producer and single consumer pattern, normal MPSCQueue would heavily use spinWait() and linked list for performance boost
 *   However, it's hard to design a structure which works well for both platform thread and virtual thread, ReentrantLock might be the best we could do at present time.
 */
public final class TaskQueue<T> {
    private final Lock lock = new ReentrantLock();
    private final int initialSize;
    private Object[] elements;
    private int index;

    public TaskQueue(int initialSize) {
        this.initialSize = initialSize;
        this.elements = new Object[initialSize];
        this.index = 0;
    }

    /**
     *   Submit a task to current task queue
     */
    public void offer(T element) {
        lock.lock();
        try{
            int nextIndex = index + 1;
            if(elements.length == nextIndex) {
                int newLen = nextIndex + (nextIndex >> 1);
                if(newLen < 0) {
                    throw new FrameworkException(ExceptionType.CONTEXT, "Size overflow");
                }
                elements = Arrays.copyOf(elements, newLen);
            }
            elements[nextIndex] = element;
            index = nextIndex;
        }finally {
            lock.unlock();
        }
    }

    /**
     *   Poll all the tasks from current task queue
     */
    @SuppressWarnings("unchecked")
    public T[] elements() {
        lock.lock();
        try{
            if(index > 0) {
                Object[] current = elements;
                elements = new Object[initialSize];
                index = 0;
                return (T[]) current;
            }else {
                return (T[]) Constants.EMPTY_ARRAY;
            }
        }finally {
            lock.unlock();
        }
    }
}
