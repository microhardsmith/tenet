package cn.zorcc.common.net;

import cn.zorcc.common.NativeHelper;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 1, time = 5, batchSize = 3000)
@Measurement(iterations = 5, time = 10, batchSize = 3000)
@Timeout(time = 3, timeUnit = TimeUnit.MINUTES)
@Fork(value = 1)
public class PrintfTest {
    private final MethodHandle methodHandle;
    private final String hello = "hello";
    private final MemorySegment memorySegment;
    public PrintfTest() {
        SymbolLookup symbolLookup = NativeHelper.loadLibraryFromResource("/test.dll");
        this.methodHandle = NativeHelper.methodHandle(symbolLookup, "pr", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        this.memorySegment = MemorySegment.allocateNative(6, SegmentScope.global());
        memorySegment.setUtf8String(0, hello);
    }
    public static void main(String[] args) throws Throwable {
//        Options opts = new OptionsBuilder().include(PrintfTest.class.getSimpleName()).resultFormat(ResultFormatType.TEXT).build();
//        new Runner(opts).run();
    }

    @Benchmark
    public void testNative() throws Throwable {
        methodHandle.invokeExact(memorySegment);
    }

    @Benchmark
    public void testSystem() {
        System.out.println(hello);
    }

    public void testMap() throws IOException {
        FileChannel fc = FileChannel.open(Path.of("C:/workspace/files/log.txt"), Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.SPARSE, StandardOpenOption.READ, StandardOpenOption.WRITE));
        MemorySegment map = fc.map(FileChannel.MapMode.READ_WRITE, 0, 4 * 1024, SegmentScope.auto());
        map.
    }
}
