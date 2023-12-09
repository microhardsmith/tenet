package cn.zorcc.common.jmh;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.RunnerException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AtomicTest extends JmhTest {
    private static final AtomicInteger a = new AtomicInteger(0);
    private static final Lock lock = new ReentrantLock();
    private int i = 0;
    private Map<Object, String> map = new HashMap<>();

    @Benchmark
    public void testPlain(Blackhole bh) {
        bh.consume(++i);
    }

    @Benchmark
    public void testAtomic(Blackhole bh) {
        bh.consume(a.getAndIncrement());
    }

    @Benchmark
    public void testMap(Blackhole bh) {
        bh.consume(map.put(a, ""));
    }

    @Benchmark
    public void testLock(Blackhole bh) {
        lock.lock();
        try{
            bh.consume(++i);
        }finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) throws RunnerException {
        runTest(AtomicTest.class);
    }
}
