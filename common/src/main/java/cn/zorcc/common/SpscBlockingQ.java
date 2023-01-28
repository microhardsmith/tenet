package cn.zorcc.common;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.jctools.queues.atomic.SpscUnboundedAtomicArrayQueue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

/**
 *  Single Producer Single Consumer BlockingQueue
 */
@Slf4j
public class SpscBlockingQ<T> implements BlockingQ<T> {
    /**
     *  默认队列块大小,队列会按块无限扩容
     */
    private static final int DEFAULT_QUEUE_SIZE = 256;
    /**
     *  单生产者单消费者基于数组的队列
     */
    private final SpscUnboundedAtomicArrayQueue<T> queue;
    /**
     *  当前消费者线程是否阻塞
     */
    private volatile boolean block = false;
    /**
     *  消费者线程
     */
    private final Thread consumerThread;
    /**
     *  启动标识
     */
    private final AtomicBoolean startFlag = new AtomicBoolean(false);

    public SpscBlockingQ(String name, Consumer<T> consumer, int size) {
        if(name.isEmpty()) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Consumer thread name shouldn't be blank");
        }
        this.queue = new SpscUnboundedAtomicArrayQueue<>(size);
        this.consumerThread = ThreadUtil.virtual(name, () -> {
            Thread currentThread = Thread.currentThread();
            while (!currentThread.isInterrupted()) {
                T element = queue.poll();
                if(element == null && !block) {
                    block = true;
                    LockSupport.park();
                    element = queue.poll();
                }
                consumer.accept(element);
            }
        });
    }

    public SpscBlockingQ(String name, Consumer<T> consumer) {
        this(name, consumer, DEFAULT_QUEUE_SIZE);
    }

    @Override
    public void start() {
        if (startFlag.compareAndSet(false, true)) {
            consumerThread.start();
        }
    }

    @Override
    public Thread thread() {
        return consumerThread;
    }

    @Override
    public void put(T t) {
        if (!queue.offer(t)) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
        }
        if(block) {
            block = false;
            LockSupport.unpark(consumerThread);
        }
    }

    @Override
    public void shutdown() {
        consumerThread.interrupt();
    }
}
