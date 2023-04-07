package cn.zorcc.log;

import cn.zorcc.common.util.NativeUtil;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 1, time = 5, batchSize = 1000)
@Measurement(iterations = 3, time = 5, batchSize = 1000)
@Timeout(time = 3, timeUnit = TimeUnit.MINUTES)
@Fork(value = 1)
public class NativeTest {
    public static void main(String[] args) throws Exception {
        Options opts = new OptionsBuilder().include(NativeTest.class.getSimpleName()).resultFormat(ResultFormatType.TEXT).build();
        new Runner(opts).run();
    }
    private final long count = 100000;
    private final MemoryLayout layout = MemoryLayout.sequenceLayout(count, ValueLayout.JAVA_BYTE);
    private final MethodHandle memcpy = NativeUtil.getNativeMethodHandle("memcpy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    @Benchmark
    public void testNative(Blackhole blackhole) {
        try(Arena arena = Arena.openConfined()) {
            final MemorySegment memorySegment = arena.allocate(layout);
            memorySegment.fill((byte)  1);
            for(long i = 0; i < count; i++) {
                blackhole.consume(memorySegment.get(ValueLayout.JAVA_BYTE, i));
            }
        }
    }

    @Benchmark
    public void testArray(Blackhole blackhole) {
        final byte[] a = new byte[(int) count];
        Arrays.fill(a, (byte) 1);
        for (byte i : a) {
            blackhole.consume(i);
        }
    }

    @Benchmark
    public void testCopy() {
        try(Arena arena = Arena.openConfined()) {
            final MemorySegment a = arena.allocate(layout);
            final MemorySegment b = arena.allocate(layout);
            a.fill((byte)  1);
            MemorySegment.copy(a, 0L, b, 0L, count);
        }
    }

    @Benchmark
    public void testMemCpy() {
        try(Arena arena = Arena.openConfined()) {
            final MemorySegment a = arena.allocate(layout);
            final MemorySegment b = arena.allocate(layout);
            a.fill((byte)  1);
            memcpy.invokeExact(a, b, count);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
