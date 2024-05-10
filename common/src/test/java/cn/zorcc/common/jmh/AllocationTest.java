package cn.zorcc.common.jmh;

import cn.zorcc.common.bindings.TenetBinding;
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
 * This test focus on different allocator behaviour
 * Just ignore Arena.auto() or Arena.confined(), using custom allocator with malloc would be much faster, use RpMalloc whenever we can
 * AllocationTest.testAuto      avgt   25  933926.575 ± 89362.926  ns/op
 * AllocationTest.testConfined  avgt   25  103721.868 ±  7026.512  ns/op
 * AllocationTest.testHeap      avgt   25   81796.236 ±   844.086  ns/op
 * AllocationTest.testRp        avgt   25   21533.046 ±   707.843  ns/op
 * AllocationTest.testSys       avgt   25   93472.459 ± 35532.708  ns/op
 */
public class AllocationTest extends JmhTest {
    private static final int COUNT = 1000;
    private static final int SIZE = 4096;
    private int[] arr;

    @Setup
    public void setup() {
        arr = new int[COUNT];
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for(int i = 0; i < COUNT; i++) {
            arr[i] = random.nextInt(1, SIZE);
        }
        TenetBinding.rpmallocInitialize();
    }

    @Benchmark
    public void testConfined(Blackhole bh) {
        for (int i : arr) {
            try(Arena arena = Arena.ofConfined()) {
                bh.consume(arena.allocate(ValueLayout.JAVA_BYTE, i));
            }
        }
    }

    @Benchmark
    public void testAuto(Blackhole bh) {
        for(int i : arr) {
            bh.consume(Arena.ofAuto().allocate(ValueLayout.JAVA_BYTE, i));
        }
    }

    @Benchmark
    public void testHeap(Blackhole bh) {
        for(int i : arr) {
            try(Allocator allocator = Allocator.HEAP) {
                bh.consume(allocator.allocate(ValueLayout.JAVA_BYTE, i));
            }
        }
    }

    @Benchmark
    public void testSys(Blackhole bh) {
        for(int i : arr) {
            try(Allocator allocator = Allocator.newDirectAllocator(MemApi.DEFAULT)) {
                bh.consume(allocator.allocate(ValueLayout.JAVA_BYTE, i));
            }
        }
    }

    @Benchmark
    public void testRp(Blackhole bh) {
        MemApi memApi = TenetBinding.rpMallocThreadInitialize();
        for(int i : arr) {
            try(Allocator allocator = Allocator.newDirectAllocator(memApi)) {
                bh.consume(allocator.allocate(ValueLayout.JAVA_BYTE, i));
            }
        }
        TenetBinding.rpMallocThreadFinalize();
    }

    @TearDown
    public void tearDown() {
        TenetBinding.rpMallocFinalize();
    }

    void main() throws RunnerException {
        runTest(AllocationTest.class);
    }
}
