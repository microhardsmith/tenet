package cn.zorcc.common.structure;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Ref;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class RefTest {
    private static final int BATCH_SIZE = 100000;
    private static final AtomicReferenceArray<Integer> arr = new AtomicReferenceArray<>(BATCH_SIZE);

    @Test
    public void testRef() throws InterruptedException {
        for (int i = 0; i < BATCH_SIZE; i++) {
            arr.set(i, 0);
        }
        BlockingQueue<Ref> queue = new LinkedTransferQueue<>();
        Thread consumer = Thread.ofPlatform().start(() -> {
            for (int i = 0; i < BATCH_SIZE; i++) {
                try {
                    Ref ref = queue.take();
                    ref.assign(i);
                } catch (InterruptedException e) {
                    throw new RuntimeException(Constants.UNREACHED, e);
                }
            }
        });
        List<Thread> threads = new ArrayList<>(BATCH_SIZE);
        for(int i = 0; i < BATCH_SIZE; i++) {
            threads.add(Thread.ofVirtual().start(() -> {
                Ref ref = new Ref();
                if (queue.offer(ref)) {
                    Object r = ref.fetch();
                    if(r instanceof Integer index && index >= 0 && index < arr.length() && arr.get(index) == 0) {
                        arr.set(index, 1);
                        return ;
                    } else {
                        throw new RuntimeException("Illegal ref : %s".formatted(r));
                    }
                }
                throw new RuntimeException(Constants.UNREACHED);
            }));
        }
        consumer.join();
        for (Thread t : threads) {
            t.join();
        }
        for (int i = 0; i < BATCH_SIZE; i++) {
            Assertions.assertEquals(arr.get(i), 1);
        }
    }
}
