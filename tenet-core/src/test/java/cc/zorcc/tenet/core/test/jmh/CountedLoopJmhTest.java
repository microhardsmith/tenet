package cc.zorcc.tenet.core.test.jmh;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.infra.Blackhole;

/**
 *   This test focus on the performance gap between counted loops and uncounted loops
 */
public class CountedLoopJmhTest extends AbstractJmhTest {

    @FunctionalInterface
    interface Looper {
        void loop(Blackhole bh, long start, long end);
    }

    @Param({"0", "1000", "1000000"})
    private long offset;

    @Param({"32", "256", "1024"})
    private long len;

    private static final Looper countedLooper = (bh, start, end) -> {
        int iStart = Math.toIntExact(start);
        int iEnd = Math.toIntExact(end);
        for(int i = iStart; i <= iEnd; i++) {
            bh.consume(i);
        }
    };

    private static final Looper unCountedLooper = (bh, start, end) -> {
        for(long i = start; i <= end; i++) {
            bh.consume(i);
        }
    };

    @Benchmark
    public void testCountedLoop(Blackhole bh) {
        countedLooper.loop(bh, offset, offset + len);
    }

    @Benchmark
    public void testUnCountedLoop(Blackhole bh) {
        unCountedLooper.loop(bh, offset, offset + len);
    }

    void main() {
        run(CountedLoopJmhTest.class);
    }
}
