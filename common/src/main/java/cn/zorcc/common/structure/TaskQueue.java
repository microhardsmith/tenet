package cn.zorcc.common.structure;

import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *   Task queue is a queue for multiple producer and single consumer pattern, normal MPSCQueue would heavily use spinWait() and linked list for performance boost
 *   However, it's hard to design a structure which works well for both platform thread and virtual thread, ReentrantLock might be the best we could do at present time.
 */
public final class TaskQueue<T> {
    private static final Object[] EMPTY_ARRAY = {};
    private final Itr<T> EMPTY_ITR = new Itr<>(EMPTY_ARRAY);
    private final Lock lock = new ReentrantLock();
    private final Object[] initialElements;
    private Object[] elements;
    private int index;

    public TaskQueue(int initialSize) {
        this.initialElements = this.elements = new Object[initialSize];
        this.index = 0;
    }

    /**
     *   Submit a task to current task queue
     */
    public void offer(T element) {
        lock.lock();
        try{
            if(index == elements.length) {
                int newCapacity = elements.length + (elements.length >> 1);
                if(newCapacity < 0) {
                    throw new FrameworkException(ExceptionType.CONTEXT, "Size overflow");
                }
                elements = Arrays.copyOf(elements, newCapacity);
            }
            elements[index++] = element;
        }finally {
            lock.unlock();
        }
    }

    private static class Itr<E> implements Iterator<E> {
        private final Object[] array;
        private int currentIndex = 0;

        Itr(Object[] array) {
            this.array = array;
        }
        @Override
        public boolean hasNext() {
            return currentIndex < array.length;
        }
        @SuppressWarnings("unchecked")
        @Override
        public E next() {
            return (E) array[currentIndex++];
        }
    }

    /**
     *   Poll all the tasks from current task queue
     */
    public Iterable<T> elements() {
        lock.lock();
        try{
            if(index > 0) {
                Itr<T> itr = new Itr<>(Arrays.copyOfRange(elements, 0, index));
                elements = initialElements;
                index = 0;
                return () -> itr;
            }else {
                return () -> EMPTY_ITR;
            }
        }finally {
            lock.unlock();
        }
    }
}
