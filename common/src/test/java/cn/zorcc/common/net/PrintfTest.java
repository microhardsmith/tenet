package cn.zorcc.common.net;

import cn.zorcc.common.util.NativeUtil;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.channels.FileChannel;
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
    private final MethodHandle println;
    private final MethodHandle flushStdout;
    private final String sys = "\033[34msystem\033[0m";
    private final String nat = "\033[34mnative\033[0m";
    private final MemorySegment memorySegment;
    public PrintfTest() {
        SymbolLookup symbolLookup = NativeUtil.loadLibraryFromResource("/lib/test.dll");
        this.println = NativeUtil.methodHandle(symbolLookup, "println", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        this.flushStdout = NativeUtil.methodHandle(symbolLookup, "flushStdout", FunctionDescriptor.ofVoid());
        this.memorySegment = MemorySegment.allocateNative(1024, SegmentScope.global());
    }
    public static void main(String[] args) throws Throwable {
        Options opts = new OptionsBuilder().include(PrintfTest.class.getSimpleName()).resultFormat(ResultFormatType.TEXT).build();
        new Runner(opts).run();
//        PrintfTest printfTest = new PrintfTest();
//        for(int i = 0;i < 10000;i++) {
//            printfTest.testNative(i);
//        }
//        printfTest.testFlush();
    }

    @Benchmark
    public void testNative() throws Throwable {
        memorySegment.setUtf8String(0, nat);
        println.invokeExact(memorySegment);
        flushStdout.invokeExact();
    }

    public void testFlush() throws Throwable {
        flushStdout.invokeExact();
    }

    @Benchmark
    public void testSystem() {
        System.out.println(sys);
    }

    public void testMap() throws IOException {
        FileChannel fc = FileChannel.open(Path.of("C:/workspace/files/log.txt"), Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.SPARSE, StandardOpenOption.READ, StandardOpenOption.WRITE));
        MemorySegment map = fc.map(FileChannel.MapMode.READ_WRITE, 0, 4 * 1024, SegmentScope.auto());
    }
}
