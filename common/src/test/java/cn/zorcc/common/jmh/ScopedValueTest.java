package cn.zorcc.common.jmh;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.RunnerException;

public class ScopedValueTest extends JmhTest {
    private static final String s = "hello";
    @Param({"100", "1000", "10000"})
    private int BATCH;
    private static final ThreadLocal<String> threadLocal = new ThreadLocal<>();
    private static final ScopedValue<String> scopedValue = ScopedValue.newInstance();

    @Benchmark
    public void testScopedValue(Blackhole blackhole) {
        ScopedValue.runWhere(scopedValue, s, () -> {
            for(int i = 0; i < BATCH; i++) {
                blackhole.consume(scopedValue.get());
            }
        });
    }

    @Benchmark
    public void testThreadLocal(Blackhole blackhole) {
        threadLocal.set(s);
        for(int i = 0; i < BATCH; i++) {
            blackhole.consume(threadLocal.get());
        }
        threadLocal.remove();
    }

    @Benchmark
    public void testEmpty(Blackhole blackhole) {
        for(int i = 0; i < BATCH; i++) {
            blackhole.consume(s);
        }
    }

    public static void main(String[] args) throws RunnerException {
        runTest(ScopedValueTest.class);
    }
}
