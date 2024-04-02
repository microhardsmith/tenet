package cn.zorcc.common.jmh;

import cn.zorcc.common.util.NativeUtil;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.RunnerException;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * MemTest.testAlignedAccess           avgt   25  75.096 ± 0.833  ns/op
 * MemTest.testDefaultAccess           avgt   25  74.359 ± 0.712  ns/op
 * MemTest.testDefaultUnalignedAccess  avgt   25  74.812 ± 1.313  ns/op
 * MemTest.testUnalignedAccess         avgt   25  91.217 ± 0.948  ns/op
 * Conclusion : Unaligned access would be slower than Aligned, but not quite much
 */
public class MemAccessTest extends JmhTest {
    private static final int COUNT = 1000;
    private Arena arena;
    private MemorySegment memorySegment;
    @Setup
    public void setup() {
        arena = Arena.ofConfined();
        memorySegment = arena.allocate(ValueLayout.JAVA_LONG, COUNT);
    }

    @Benchmark
    public void testDefaultAccess(Blackhole blackhole) {
        for(int i = 0; i < COUNT - 1; i++) {
            blackhole.consume(memorySegment.get(ValueLayout.JAVA_LONG, (long) Long.BYTES * i));
        }
    }

    @Benchmark
    public void testDefaultUnalignedAccess(Blackhole blackhole) {
        for(int i = 0; i < COUNT - 1; i++) {
            blackhole.consume(memorySegment.get(ValueLayout.JAVA_LONG, (long) Long.BYTES * i));
        }
    }


    @Benchmark
    public void testUnalignedAccess(Blackhole blackhole) {
        for(int i = 0; i < COUNT - 1; i++) {
            blackhole.consume(NativeUtil.getLong(memorySegment, (long) Long.BYTES * i + 1L));
        }
    }

    @Benchmark
    public void testAlignedAccess(Blackhole blackhole) {
        for(int i = 0; i < COUNT - 1; i++) {
            blackhole.consume(NativeUtil.getLong(memorySegment, (long) Long.BYTES * i));
        }
    }

    @TearDown
    public void tearDown() {
        arena.close();
    }

    public static void main(String[] args) throws RunnerException {
        runTest(MemAccessTest.class);
    }
}
