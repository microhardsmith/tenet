package cc.zorcc.tenet.core.test.jmh;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(value = Mode.AverageTime)
@Warmup(iterations = 3, time = 400, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1200, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public abstract class AbstractJmhTest {
    /**
     *   Jmh test result file formatter
     */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     *   Recommended jvm args:
     *   --enable-native-access=tenet.core
     *   -DTENET_LIBRARY_PATH=<Put your own library path here>
     *   -XX:+UseZGC
     *   -XX:+ZGenerational
     *   --add-modules
     *   jdk.incubator.vector
     */
    protected static void run(Class<?> launchClass) {
        Options options = new OptionsBuilder()
                .include(launchClass.getSimpleName())
                .detectJvmArgs()
                .resultFormat(ResultFormatType.TEXT)
                .result("%s_%s.txt".formatted(launchClass.getSimpleName(), LocalDateTime.now().format(FORMATTER)))
                .build();
        try {
            new Runner(options).run();
        }catch (RunnerException e) {
            e.printStackTrace(System.err);
        }
    }
}
