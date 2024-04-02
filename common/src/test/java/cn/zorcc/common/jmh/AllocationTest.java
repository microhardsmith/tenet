package cn.zorcc.common.jmh;

import cn.zorcc.common.RpMalloc;
import cn.zorcc.common.structure.Allocator;
import cn.zorcc.common.structure.MemApi;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.RunnerException;

import java.lang.foreign.Arena;
import java.lang.foreign.ValueLayout;
import java.util.concurrent.ThreadLocalRandom;

/**
 * AllocationTest.testAuto      avgt   25  952123.665 ± 87650.750  ns/op
 * AllocationTest.testConfined  avgt   25  110382.002 ±  9231.961  ns/op
 * AllocationTest.testHeap      avgt   25   84734.509 ±  1134.490  ns/op
 * AllocationTest.testRp        avgt   25   13987.693 ±   624.108  ns/op
 * AllocationTest.testSys       avgt   25   51766.527 ±  4461.971  ns/op
 */
public class AllocationTest extends JmhTest {
    private static final int COUNT = 1000;
    private static final long SIZE = 4096L;
    private long[] arr;

    @Setup
    public void setup() {
        arr = new long[COUNT];
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for(int i = 0; i < COUNT; i++) {
            arr[i] = random.nextLong(1L, SIZE);
        }
        RpMalloc.initialize();
    }

    @Benchmark
    public void testConfined(Blackhole bh) {
        for (long i : arr) {
            try(Arena arena = Arena.ofConfined()) {
                bh.consume(arena.allocate(ValueLayout.JAVA_BYTE, i));
            }
        }
    }

    @Benchmark
    public void testAuto(Blackhole bh) {
        for(long i : arr) {
            bh.consume(Arena.ofAuto().allocate(ValueLayout.JAVA_BYTE, i));
        }
    }

    @Benchmark
    public void testSys(Blackhole bh) {
        for(long i : arr) {
            try(Allocator allocator = Allocator.newDirectAllocator(MemApi.DEFAULT)) {
                bh.consume(allocator.allocate(ValueLayout.JAVA_BYTE, i));
            }
        }
    }

    @Benchmark
    public void testHeap(Blackhole bh) {
        for(long i : arr) {
            try(Allocator allocator = Allocator.HEAP) {
                bh.consume(allocator.allocate(ValueLayout.JAVA_BYTE, i));
            }
        }
    }

    @Benchmark
    public void testRp(Blackhole bh) {
        MemApi memApi = RpMalloc.tInitialize();
        for(long i : arr) {
            try(Allocator allocator = Allocator.newDirectAllocator(memApi)) {
                bh.consume(allocator.allocate(ValueLayout.JAVA_BYTE, i));
            }
        }
        RpMalloc.tRelease();
    }

    @TearDown
    public void tearDown() {
        RpMalloc.release();
    }

    public static void main(String[] args) throws RunnerException {
        runTest(AllocationTest.class);
    }
}
