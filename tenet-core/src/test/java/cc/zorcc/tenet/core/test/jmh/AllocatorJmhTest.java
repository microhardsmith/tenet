package cc.zorcc.tenet.core.test.jmh;

import cc.zorcc.tenet.core.Allocator;
import cc.zorcc.tenet.core.Mem;
import cc.zorcc.tenet.core.RpMem;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.foreign.Arena;
import java.lang.foreign.ValueLayout;
import java.util.concurrent.ThreadLocalRandom;

/**
 *   This test focus on the performance gap between arena and custom allocators
 */
public class AllocatorJmhTest extends AbstractJmhTest {

    private static final int MIN_SIZE = 1024;

    private static final int MAX_SIZE = 4 * 1024;

    @Param({"10", "100", "1000"})
    private int count;

    private int[] arr;
    private Mem rpMem;

    @Setup(Level.Iteration)
    public void setup() {
        arr = new int[count];
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for(int i = 0; i < count; i++) {
            arr[i] = random.nextInt(MIN_SIZE, MAX_SIZE);
        }
        rpMem = RpMem.acquire();
    }

    @Benchmark
    public void testArena(Blackhole bh) {
        try(Arena arena = Arena.ofConfined()) {
            for (int i : arr) {
                bh.consume(arena.allocate(ValueLayout.JAVA_BYTE, i));
            }
        }
    }

    @Benchmark
    public void testMalloc(Blackhole bh) {
        try(Allocator allocator = Allocator.newDirectAllocator(Mem.DEFAULT)) {
            for (int i : arr) {
                bh.consume(allocator.allocate(ValueLayout.JAVA_BYTE, i));
            }
        }
    }

    @Benchmark
    public void testRpMalloc(Blackhole bh) {
        for(int i : arr) {
            try(Allocator allocator = Allocator.newDirectAllocator(rpMem)) {
                bh.consume(allocator.allocate(ValueLayout.JAVA_BYTE, i));
            }
        }
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        RpMem.release();
    }

    void main() {
        run(AllocatorJmhTest.class);
    }
}
