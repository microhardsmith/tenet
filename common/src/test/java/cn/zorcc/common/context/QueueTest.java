package cn.zorcc.common.context;

import cn.zorcc.common.Clock;
import cn.zorcc.common.structure.MpscArrayQueue;
import cn.zorcc.common.structure.MpscLinkedQueue;
import org.jctools.queues.atomic.MpscLinkedAtomicQueue;
import org.jctools.queues.atomic.MpscUnboundedAtomicArrayQueue;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class QueueTest {
    private static final int RANGE = 1000;
    private static final int PRODUCERS = 10;
    private static final int SUM = IntStream.range(0, RANGE).sum();
    private static final int ROUND = 100;

    @Test
    public void testMpscArrayQueue() throws InterruptedException {
        List<Long> metrics = new ArrayList<>();
        for(int round = 0; round < ROUND; round++) {
            final int r = round;
            MpscArrayQueue<Integer> queue = MpscArrayQueue.create(16);
            List<Thread> list = new ArrayList<>();
            for(int i = 0; i < PRODUCERS; i++) {
                list.add(Thread.ofVirtual().unstarted(() -> {
                    for(int t = 0; t < RANGE; t++) {
                        queue.offer(t);
                    }
                }));
            }
            Thread consumer = Thread.ofPlatform().unstarted(() -> {
                int s = 0;
                int times = PRODUCERS;
                for (; ; ) {
                    Integer v = queue.poll();
                    if (v == null) {
                        continue;
                    }
                    s = s + v;
                    if (s >= SUM) {
                        s -= SUM;
                        if (--times == 0) {
                            System.out.println(STR."Successful : \{r}");
                            break;
                        }
                    }
                }
            });
            long current = Clock.nano();
            list.forEach(Thread::start);
            consumer.start();
            list.forEach(thread -> {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            consumer.join();
            metrics.add(Clock.elapsed(current));
        }
        System.out.println(STR."Average : \{metrics.stream().mapToDouble(Long::doubleValue).average().orElse(Double.NaN)}");
    }

    @Test
    public void testMpscLinkedQueue() throws InterruptedException {
        List<Long> metrics = new ArrayList<>();
        for(int round = 0; round < 100; round++) {
            final int r = round;
            MpscLinkedQueue<Integer> queue = new MpscLinkedQueue<>();
            List<Thread> list = new ArrayList<>();
            for(int i = 0; i < PRODUCERS; i++) {
                list.add(Thread.ofVirtual().unstarted(() -> {
                    for(int t = 0; t < RANGE; t++) {
                        queue.offer(t);
                    }
                }));
            }
            Thread consumer = Thread.ofPlatform().unstarted(() -> {
                int s = 0;
                int times = PRODUCERS;
                for (; ; ) {
                    Integer v = queue.poll();
                    if (v == null) {
                        continue;
                    }
                    s = s + v;
                    if (s >= SUM) {
                        s -= SUM;
                        if (--times == 0) {
                            System.out.println(STR."Successful : \{r}");
                            break;
                        }
                    }
                }
            });
            long current = Clock.nano();
            list.forEach(Thread::start);
            consumer.start();
            list.forEach(thread -> {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            consumer.join();
            metrics.add(Clock.elapsed(current));
        }
        System.out.println(STR."Average : \{metrics.stream().mapToDouble(Long::doubleValue).average().orElse(Double.NaN)}");
    }

    @Test
    public void testJctoolsMpscArrayQueue() throws InterruptedException {
        List<Long> metrics = new ArrayList<>();
        for(int round = 0; round < 100; round++) {
            final int r = round;
            MpscUnboundedAtomicArrayQueue<Integer> queue = new MpscUnboundedAtomicArrayQueue<>(16);
            List<Thread> list = new ArrayList<>();
            for(int i = 0; i < PRODUCERS; i++) {
                list.add(Thread.ofVirtual().unstarted(() -> {
                    for(int t = 0; t < RANGE; t++) {
                        queue.offer(t);
                    }
                }));
            }
            Thread consumer = Thread.ofPlatform().unstarted(() -> {
                int s = 0;
                int times = PRODUCERS;
                for (; ; ) {
                    Integer v = queue.poll();
                    if (v == null) {
                        continue;
                    }
                    s = s + v;
                    if (s >= SUM) {
                        s -= SUM;
                        if (--times == 0) {
                            System.out.println(STR."Successful : \{r}");
                            break;
                        }
                    }
                }
            });
            long current = Clock.nano();
            list.forEach(Thread::start);
            consumer.start();
            list.forEach(thread -> {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            consumer.join();
            metrics.add(Clock.elapsed(current));
        }
        System.out.println(STR."Average : \{metrics.stream().mapToDouble(Long::doubleValue).average().orElse(Double.NaN)}");
    }

    @Test
    public void testJctoolsMpscLinkedQueue() throws InterruptedException {
        List<Long> metrics = new ArrayList<>();
        for(int round = 0; round < 100; round++) {
            final int r = round;
            MpscLinkedAtomicQueue<Integer> queue = new MpscLinkedAtomicQueue<>();
            List<Thread> list = new ArrayList<>();
            for(int i = 0; i < PRODUCERS; i++) {
                list.add(Thread.ofVirtual().unstarted(() -> {
                    for(int t = 0; t < RANGE; t++) {
                        queue.offer(t);
                    }
                }));
            }
            Thread consumer = Thread.ofPlatform().unstarted(() -> {
                int s = 0;
                int times = PRODUCERS;
                for (; ; ) {
                    Integer v = queue.poll();
                    if (v == null) {
                        continue;
                    }
                    s = s + v;
                    if (s >= SUM) {
                        s -= SUM;
                        if (--times == 0) {
                            System.out.println(STR."Successful : \{r}");
                            break;
                        }
                    }
                }
            });
            long current = Clock.nano();
            list.forEach(Thread::start);
            consumer.start();
            list.forEach(thread -> {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            consumer.join();
            metrics.add(Clock.elapsed(current));
        }
        System.out.println(STR."Average : \{metrics.stream().mapToDouble(Long::doubleValue).average().orElse(Double.NaN)}");
    }
}
