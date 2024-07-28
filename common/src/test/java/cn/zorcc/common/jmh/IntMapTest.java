package cn.zorcc.common.jmh;

import cn.zorcc.common.structure.IntMap;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.RunnerException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

public class IntMapTest extends JmhTest {
    private static final int LENGTH = 64;
    @Param({"10", "1000", "100000"})
    private int size;
    private static final Object t = new Object();
    private static final Map<Integer, Object> m1 = new LinkedHashMap<>(LENGTH, Float.MAX_VALUE);
    private static final IntMap<Object> m2 = IntMap.newLinkedMap(LENGTH);
    private static final IntMap<Object> m3 = IntMap.newTreeMap(LENGTH);
    private static final Map<Integer, Object> m4 = new TreeMap<>(Integer::compareTo);
    private static final Map<IntHolder, Object> m5 = new TreeMap<>(IntHolder::compareTo);

    record IntHolder(int value) implements Comparable<IntHolder> {
        @Override
        public int compareTo(IntHolder o) {
            return Integer.compare(value, o.value);
        }
    }

    static {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for(int i = 0; i < 10000; i++) {
            int o = random.nextInt();
            m1.put(i, o);
            m2.put(i, o);
            m3.put(i, o);
            m4.put(i, o);
            m5.put(new IntHolder(i), o);
        }
    }

    @Benchmark
    public void testGetHashMap(Blackhole bh) {
        for(int i = 0; i < size; i++) {
            bh.consume(m1.get(i));
        }
    }

    @Benchmark
    public void testGetIntLinkedMap(Blackhole bh) {
        for(int i = 0; i < size; i++) {
            bh.consume(m2.get(i));
        }
    }

    @Benchmark
    public void testGetIntTreeMap(Blackhole bh) {
        for(int i = 0; i < size; i++) {
            bh.consume(m3.get(i));
        }
    }

    @Benchmark
    public void testGetTreeMap(Blackhole bh) {
        for(int i = 0; i < size; i++) {
            bh.consume(m4.get(i));
        }
    }

    @Benchmark
    public void testGetIntHolderTreeMap(Blackhole bh) {
        for(int i = 0; i < size; i++) {
            bh.consume(m5.get(new IntHolder(i)));
        }
    }

    @Benchmark
    public void testPutHashMap(Blackhole bh) {
        HashMap<Object, Object> m = new HashMap<>(LENGTH, Float.MAX_VALUE);
        for(int i = 0; i < size; i++) {
            m.put(i, t);
        }
        bh.consume(m);
    }

    @Benchmark
    public void testPutIntLinkedMap(Blackhole bh) {
        IntMap<Object> m = IntMap.newLinkedMap(LENGTH);
        for(int i = 0; i < size; i++) {
            m.put(i, t);
        }
        bh.consume(m);
    }

    @Benchmark
    public void testPutIntTreeMap(Blackhole bh) {
        IntMap<Object> m = IntMap.newTreeMap(LENGTH);
        for(int i = 0; i < size; i++) {
            m.put(i, t);
        }
        bh.consume(m);
    }

    @Benchmark
    public void testPutTreeMap(Blackhole bh) {
        TreeMap<Integer, Object> m = new TreeMap<>();
        for(int i = 0; i < size; i++) {
            m.put(i, t);
        }
        bh.consume(m);
    }

    @Benchmark
    public void testPutIntHolderTreeMap(Blackhole bh) {
        TreeMap<IntHolder, Object> m = new TreeMap<>();
        for(int i = 0; i < size; i++) {
            m.put(new IntHolder(i), t);
        }
        bh.consume(m);
    }

    public static void main(String[] args) throws RunnerException {
        runTest(IntMapTest.class);
    }
}
