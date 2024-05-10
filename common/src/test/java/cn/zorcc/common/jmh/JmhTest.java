package cn.zorcc.common.jmh;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 *   Jmh Test base class, all the JMH Tests should be put here
 */
@BenchmarkMode(value = Mode.AverageTime)
@Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 2000, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
abstract class JmhTest {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     *   Recommended jvm args:
     *   --enable-native-access=ALL-UNNAMED
     *   -DTENET_LIBRARY_PATH=C:/workspace/tenet-lib/lib
     *   -Xmx2G
     *   -XX:+UseZGC
     *   -XX:+ZGenerational
     *   --add-modules
     *   jdk.incubator.vector
     */
    public static void runTest(Class<?> launchClass) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(launchClass.getSimpleName())
                .detectJvmArgs()
                .resultFormat(ResultFormatType.TEXT)
                .result("%s_%s.txt".formatted(launchClass.getSimpleName(), LocalDateTime.now().format(FORMATTER)))
                .build();
        new Runner(options).run();
    }
}
