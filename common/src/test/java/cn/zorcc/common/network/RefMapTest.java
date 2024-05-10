package cn.zorcc.common.network;

import cn.zorcc.common.Ref;
import cn.zorcc.common.structure.Allocator;
import cn.zorcc.common.util.NativeUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class RefMapTest {
    private static final int BATCH = 100000;
    private static final int ROUND = 100;

    private static MemorySegment generateData(int first, int second) {
        MemorySegment m = Allocator.HEAP.allocate(ValueLayout.JAVA_INT, 2);
        NativeUtil.setInt(m, 0, first);
        NativeUtil.setInt(m, 4, second);
        return m;
    }

    @Test
    public void test() throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        for(int cpu = 0; cpu < Runtime.getRuntime().availableProcessors(); cpu++) {
            Thread thread = Thread.ofPlatform().start(() -> {
                for (int t = 0; t < ROUND; t++) {
                    PollerNode.RefMap refMap = PollerNode.RefMap.newInstance(16);
                    List<MemorySegment> segments = new ArrayList<>(BATCH);
                    List<Ref> refs = new ArrayList<>(BATCH);
                    ThreadLocalRandom random = ThreadLocalRandom.current();
                    Set<Integer> set = new HashSet<>(BATCH);
                    for (int i = 0; i < BATCH; i++) {
                        int first = random.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE); // same prefix could provide more collision
                        int second;
                        do {
                            second = random.nextInt();
                        } while (!set.add(second)); // ensure each seg is unique
                        MemorySegment seg = generateData(first, second);
                        Ref ref = new Ref();
                        refMap.put(seg, ref);
                        segments.add(seg);
                        refs.add(ref);
                    }
                    for (int i = 0; i < BATCH; i++) {
                        MemorySegment seg = segments.get(i);
                        Ref ref = refs.get(i);
                        Ref r = refMap.get(seg);
                        Assertions.assertSame(r, ref);
                        Assertions.assertTrue(refMap.remove(seg, ref));
                        Assertions.assertEquals(refMap.count(), BATCH - i - 1);
                    }
                }
            });
            threads.add(thread);
        }
        for (Thread t : threads) {
            t.join();
        }
    }
}
