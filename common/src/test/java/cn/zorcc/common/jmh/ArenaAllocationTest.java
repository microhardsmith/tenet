package cn.zorcc.common.jmh;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.RunnerException;

import java.lang.foreign.Arena;
import java.lang.foreign.ValueLayout;

/**
 * Benchmark                    Mode  Cnt    Score    Error  Units
 * AllocationTest.testAuto      avgt   50  155.663 ± 37.894  ns/op
 * AllocationTest.testConfined  avgt   50  130.021 ±  4.483  ns/op
 */
public class ArenaAllocationTest extends JmhTest {
    private static final Arena autoArena = Arena.ofAuto();

    @Benchmark
    public void testConfined(Blackhole bh) {
        try(Arena arena = Arena.ofConfined()) {
            bh.consume(arena.allocate(ValueLayout.JAVA_BYTE));
        }
    }

    @Benchmark
    public void testAuto(Blackhole bh) {
        bh.consume(autoArena.allocate(ValueLayout.JAVA_BYTE));
    }

    public static void main(String[] args) throws RunnerException {
        runTest(ArenaAllocationTest.class);
    }
}
