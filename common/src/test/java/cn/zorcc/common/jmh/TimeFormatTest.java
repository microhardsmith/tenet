package cn.zorcc.common.jmh;

import cn.zorcc.common.Constants;
import cn.zorcc.common.log.DefaultTimeResolver;
import cn.zorcc.common.log.TimeResolver;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.RunnerException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 *   May get twice the boost using timeResolver
 *   Benchmark                   Mode  Cnt   Score   Error  Units
 *   TimeFormatTest.testDefault  avgt   50  33.715 ± 0.212  ns/op
 *   TimeFormatTest.testFormat   avgt   50  63.067 ± 0.562  ns/op
 */
public class TimeFormatTest extends JmhTest {
    private static final TimeResolver timeResolver = new DefaultTimeResolver();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DEFAULT_TIME_FORMAT);
    @Benchmark
    public void testFormat(Blackhole bh) {
        bh.consume(formatter.format(LocalDateTime.now()));
    }

    @Benchmark
    public void testDefault(Blackhole bh) {
        bh.consume(timeResolver.format(LocalDateTime.now()));
    }

    public static void main(String[] args) throws RunnerException {
        runTest(TimeFormatTest.class);
    }
}
