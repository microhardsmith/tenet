package cn.zorcc.common;

import org.jctools.queues.atomic.SpscUnboundedAtomicArrayQueue;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public class Q {
    private static final int count = 1000000;
    private static volatile int m = 0;

    public static void main(String[] args) throws InterruptedException {
//        for(int i = 0;i < 10; i++) {
//            testQueue();
//            testSpsc();
//        }
        testPark();
    }

    public static void testPark() throws InterruptedException {
        Thread t = Thread.ofVirtual().start(() -> {
            Thread thread = Thread.currentThread();
            LockSupport.unpark(thread);
            LockSupport.park();
            System.out.println("finish");
        });
        t.join();
    }

    public static void testQueue() throws InterruptedException {
        BlockingQueue<Integer> integers = new ArrayBlockingQueue<>(1024);
        CountDownLatch countDownLatch = new CountDownLatch(2);
        long l = System.currentTimeMillis();
        Thread.ofVirtual().start(() -> {
            for(int i = 0; i < count; i++) {
                try {
                    integers.put(i);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            };
            countDownLatch.countDown();
        });
        Thread.ofVirtual().start(() -> {
            for(int i = 0; i < count; i++) {
                try {
                    integers.take();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            };
            countDownLatch.countDown();
        });
        countDownLatch.await();
        System.out.println("Array : " + (System.currentTimeMillis() - l));
    }

    public static void testSpsc() throws InterruptedException {
        Queue<Integer> q = new SpscUnboundedAtomicArrayQueue<>(256);
        CountDownLatch countDownLatch = new CountDownLatch(2);
        long l = System.currentTimeMillis();
        Thread consumer = Thread.ofVirtual().start(() -> {
            int time = 0;
            for (int i = 0; i < count; i++) {
                Integer poll = q.poll();
                // atomicBoolean.compareAndSet(true, false)
                if(poll == null && m == 0) {
                    m = 1;
                    LockSupport.park();
                }
                time++;
            }
            System.out.println("Consumer Times : " + time);
            countDownLatch.countDown();
        });
        Thread.ofVirtual().start(() -> {
            int time = 0;
            for(int i = 0; i < count; i++) {
                q.offer(i);
                time++;
                // !atomicBoolean.get() && atomicBoolean.compareAndSet(false, true)
                if(m == 1) {
                    LockSupport.unpark(consumer);
                    m = 0;
                }
            }
            System.out.println("Producer Times : " + time);
            countDownLatch.countDown();
        });
        countDownLatch.await();
        System.out.println("Spsc : " + (System.currentTimeMillis() - l));
    }
}
