package cn.zorcc.common.jmh;

import cn.zorcc.common.Constants;
import cn.zorcc.common.bindings.SystemBinding;
import cn.zorcc.common.structure.ReadBuffer;
import org.openjdk.jmh.annotations.Benchmark;
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

public class StrlenTest extends JmhTest {
    private static final int BATCH = 64;
    private static final long MINIMUM_SIZE = 100;
    private static final long MAXIMUM_SIZE = 10000;
    private Arena arena;
    private List<MemorySegment> segments;

    private static MemorySegment init(Arena arena, long size) {
        MemorySegment m = arena.allocate(ValueLayout.JAVA_BYTE, size + 1);
        for(int i = 0; i < size; i++) {
            m.set(ValueLayout.JAVA_BYTE, i, (byte) 'a');
        }
        m.set(ValueLayout.JAVA_BYTE, size, Constants.NUT);
        return m;
    }

    @Setup
    public void setup() {
        arena = Arena.ofConfined();
        segments = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for(int i = 0; i < BATCH; i++) {
            long size = random.nextLong(MINIMUM_SIZE, MAXIMUM_SIZE);
            segments.add(init(arena, size));
        }
    }

    @Benchmark
    public void testStrlen(Blackhole blackhole) {
        for(MemorySegment segment : segments) {
            long size = SystemBinding.strlen(segment, segment.byteSize());
            blackhole.consume(new String(segment.asSlice(0, size).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8));
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
            MemorySegment s = new ReadBuffer(segment).readPattern(PATTERN, Constants.NUT);
            blackhole.consume(new String(Objects.requireNonNull(s).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8));
        }
    }

    @Benchmark
    public void testLinear(Blackhole blackhole) {
        for(MemorySegment segment : segments) {
            MemorySegment r = new ReadBuffer(segment).readUntil(Constants.NUT);
            blackhole.consume(new String(Objects.requireNonNull(r).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8));
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
