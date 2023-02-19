package cn.zorcc.common.net;

import cn.zorcc.common.NativeHelper;
import cn.zorcc.common.util.ThreadUtil;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.TimeUnit;

//@BenchmarkMode(Mode.Throughput)
//@OutputTimeUnit(TimeUnit.SECONDS)
//@State(Scope.Benchmark)
//@Warmup(iterations = 1, time = 5, batchSize = 3000)
//@Measurement(iterations = 5, time = 10, batchSize = 3000)
//@Timeout(time = 3, timeUnit = TimeUnit.MINUTES)
//@Fork(value = 1)
public class PrintfTest {
    private final MethodHandle methodHandle;
    private final String sys = "\033[34msystem\033[0m";
    private final String nat = "\033[34mnative\033[0m";
    private final MemorySegment memorySegment;
    public PrintfTest() {
        SymbolLookup symbolLookup = NativeHelper.loadLibraryFromResource("/test.dll");
        this.methodHandle = NativeHelper.methodHandle(symbolLookup, "pr", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        this.memorySegment = MemorySegment.allocateNative(1024, SegmentScope.global());
    }
    public static void main(String[] args) throws Throwable {
        PrintfTest printfTest = new PrintfTest();
        for(int i = 0;i < 1000;i++) {
            printfTest.testNative(i);
        }
        Thread.sleep(5000);
    }

//    @Benchmark
    public void testNative(int i) throws Throwable {
        memorySegment.setUtf8String(0, nat + i);
        int r = (int) methodHandle.invokeExact(memorySegment);
    }

//    @Benchmark
    public void testSystem() {
        System.out.println(sys);
    }

    public void testMap() throws IOException {
        FileChannel fc = FileChannel.open(Path.of("C:/workspace/files/log.txt"), Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.SPARSE, StandardOpenOption.READ, StandardOpenOption.WRITE));
        MemorySegment map = fc.map(FileChannel.MapMode.READ_WRITE, 0, 4 * 1024, SegmentScope.auto());

    }
}
