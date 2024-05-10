package cn.zorcc.common.context;

import cn.zorcc.common.structure.IntHolder;
import cn.zorcc.common.structure.Mutex;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class LockTest {
    private static final int ROUNDS = 100000;

    @Test
    public void testIntHolderSpin() throws InterruptedException {
        IntHolder holder = new IntHolder(0);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Thread t1 = Thread.ofPlatform().name("Thread1").unstarted(() -> {
            try {
                countDownLatch.await();
                for(int i = 0; i < ROUNDS; i++) {
                    holder.transform(v -> v + 1, Thread::onSpinWait);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        Thread t2 = Thread.ofPlatform().name("Thread2").unstarted(() -> {
            try {
                countDownLatch.await();
                for(int i = 0; i < ROUNDS; i++) {
                    holder.transform(v -> v + 1, Thread::onSpinWait);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        t1.start();
        t2.start();
        countDownLatch.countDown();
        t1.join();
        t2.join();
        Assertions.assertEquals(holder.getVolatileValue(), ROUNDS * 2);
    }

    @Test
    public void testSpin() throws InterruptedException {
        AtomicReference<Mutex> ref = new AtomicReference<>();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Thread t1 = Thread.ofPlatform().name("Thread1").unstarted(() -> {
            try{
                countDownLatch.await();
                Mutex mutex = ref.get();
                for(int i = 0; i < ROUNDS; i++) {
                    int current = mutex.pLock();
                    mutex.pUnlock(current + 1);
                }
            } catch (InterruptedException e){
                throw new RuntimeException(e);
            }
        });
        Thread t2 = Thread.ofPlatform().name("Thread2").unstarted(() -> {
            try{
                countDownLatch.await();
                Mutex mutex = ref.get();
                for(int i = 0; i < ROUNDS; i++){
                    int current = mutex.wLock();
                    mutex.wUnlock(current + 1);
                }
            } catch (InterruptedException e){
                throw new RuntimeException(e);
            }
        });
        Mutex mutex = new Mutex(t1, t2, 0);
        ref.set(mutex);
        t1.start();
        t2.start();
        countDownLatch.countDown();
        t1.join();
        t2.join();
        Assertions.assertEquals(mutex.getVolatileState(), ROUNDS * 2);
    }

}
