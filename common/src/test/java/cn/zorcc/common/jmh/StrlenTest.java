package cn.zorcc.common.jmh;

import cn.zorcc.common.Constants;
import cn.zorcc.common.bindings.SystemBinding;
import cn.zorcc.common.structure.ReadBuffer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.RunnerException;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This test is aimed at finding '\0' at a sequence,
 * Swar is preferred than Linear when searching for arbitrary byte at a sequence
 * The benchmark shows when data is smaller, swar might be slower, however the benchmark could be cheating because branch predictor could be learning from the benchmark data
 * when targeted at '\0', using strlen() would be the fastest approach
 * <p>
 * StrlenTest.testJdk        100  avgt   25    2739.272 ±   74.245  ns/op
 * StrlenTest.testJdk      10000  avgt   25   92919.992 ± 3505.743  ns/op
 * StrlenTest.testLinear     100  avgt   25    2906.915 ±   90.710  ns/op
 * StrlenTest.testLinear   10000  avgt   25  164464.310 ± 5648.656  ns/op
 * StrlenTest.testStrlen     100  avgt   25    2482.472 ±  112.805  ns/op
 * StrlenTest.testStrlen   10000  avgt   25   87673.961 ± 2559.505  ns/op
 * StrlenTest.testSwar       100  avgt   25    3211.847 ±   90.444  ns/op
 * StrlenTest.testSwar     10000  avgt   25  117472.792 ± 4458.383  ns/op
 */
public class StrlenTest extends JmhTest {
    private static final int BATCH = 64;
    @Param({"100", "10000"})
    private int size;
    private Arena arena;
    private List<MemorySegment> segments;

    private static MemorySegment init(Arena arena, long size) {
        ThreadLocalRandom t = ThreadLocalRandom.current();
        MemorySegment m = arena.allocate(ValueLayout.JAVA_BYTE, size + 1);
        for(int i = 0; i < size; i++) {
            byte b = (byte) t.nextInt((byte) 'a', (byte) 'z');
            m.set(ValueLayout.JAVA_BYTE, i, b);
        }
        m.set(ValueLayout.JAVA_BYTE, size, Constants.NUT);
        return m;
    }

    @Setup
    public void setup() {
        arena = Arena.ofConfined();
        segments = new ArrayList<>();
        for(int i = 0; i < BATCH; i++) {
            segments.add(init(arena, size));
        }
    }

    private static String newStr(MemorySegment segment, int len) {
        byte[] bytes = new byte[len];
        MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, 0, bytes, 0, len);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Benchmark
    public void testStrlen(Blackhole blackhole) {
        for(MemorySegment segment : segments) {
            long size = SystemBinding.strlen(segment, segment.byteSize());
            String s = newStr(segment, (int) size);
            blackhole.consume(s);
        }
    }

    @Benchmark
    public void testJdk(Blackhole blackhole) {
        for(MemorySegment segment : segments) {
            blackhole.consume(segment.getString(0L, StandardCharsets.UTF_8));
        }
    }

    private static final long PATTERN = ReadBuffer.compilePattern(Constants.NUT);
    @Benchmark
    public void testSwar(Blackhole blackhole) {
        for(MemorySegment segment : segments) {
            MemorySegment r = Objects.requireNonNull(new ReadBuffer(segment).swarReadUntil(PATTERN, Constants.NUT));
            String s = newStr(r, (int) r.byteSize());
            blackhole.consume(s);
        }
    }

    @Benchmark
    public void testLinear(Blackhole blackhole) {
        for(MemorySegment segment : segments) {
            MemorySegment r = Objects.requireNonNull(new ReadBuffer(segment).readUntil(Constants.NUT));
            String s = newStr(r, (int) r.byteSize());
            blackhole.consume(s);
        }
    }

    @TearDown
    public void tearDown() {
        arena.close();
    }

    public static void main(String[] args) throws RunnerException {
        runTest(StrlenTest.class);
    }
}
