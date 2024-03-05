package cn.zorcc.common.context;

import cn.zorcc.common.structure.IntHolder;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

public class LockTest {
    private static final int ROUNDS = 100000;

    @Test
    public void testIntHolderSpin() throws InterruptedException {
        IntHolder holder = new IntHolder(0);
        Thread t1 = Thread.ofPlatform().name("Thread1").unstarted(() -> {
            IntStream.range(0, ROUNDS).forEach(_ -> {
                holder.transform(v -> v + 1, Thread::onSpinWait);
            });
        });
        Thread t2 = Thread.ofPlatform().name("Thread2").unstarted(() -> {
            IntStream.range(0, ROUNDS).forEach(_ -> {
                holder.transform(v -> v + 1, Thread::onSpinWait);
            });
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println(holder.getVolatileValue());
    }

}
