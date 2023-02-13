package cn.zorcc.log;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 1, time = 5, batchSize = 1000)
@Measurement(iterations = 5, time = 10, batchSize = 1000)
@Timeout(time = 3, timeUnit = TimeUnit.MINUTES)
@Fork(value = 1)
public class ForloopTest {
    public static void main(String[] args) throws Exception {
        Options opts = new OptionsBuilder().include(ForloopTest.class.getSimpleName()).resultFormat(ResultFormatType.TEXT).build();
        new Runner(opts).run();
    }

    private static final List<Integer> list = new ArrayList<>();
    static
    {
        for(int i=0; i < 1_000_0; i++)
        {
            list.add(i);
        }
    }

    @Benchmark
    public void usingStream(Blackhole blackhole) {
        list.forEach(blackhole::consume);
    }

    @Benchmark
    public void usingIterator(Blackhole blackhole) {
        list.listIterator().forEachRemaining(blackhole::consume);
    }

    @Benchmark
    public void usingForEachLoop(Blackhole blackhole) {
        for(Integer i : list)
        {
            blackhole.consume(i);
        }
    }

    @Benchmark
    public void usingSimpleForLoop(Blackhole blackhole) {
        int size = list.size();
        for(int i = 0; i < size; i++)
        {
            blackhole.consume(list.get(i));
        }
    }
}
