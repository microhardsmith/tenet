package cn.zorcc.common.jmh;

import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.structure.Allocator;
import cn.zorcc.common.util.CompressUtil;
import cn.zorcc.common.util.NativeUtil;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.RunnerException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;


public class CompressionJmhTest extends JmhTest {
    @Param({"/small.json", "medium.json", "/large.json"})
    private String jsonFile;
    @Param({"1", "5", "9"})
    private int level;
    private byte[] original;
    private MemorySegment originalSegment;

    @Setup
    public void setup() {
        try(InputStream stream = CompressionJmhTest.class.getResourceAsStream(jsonFile)) {
            assert stream != null;
            this.original = stream.readAllBytes();
            this.originalSegment = NativeUtil.globalArena.allocate(ValueLayout.JAVA_BYTE, original.length);
            for(int i = 0; i < original.length; i++) {
                originalSegment.set(ValueLayout.JAVA_BYTE, i, original[i]);
            }
        } catch (IOException e) {
            throw new FrameworkException(ExceptionType.JSON, "Json file not found", e);
        }
    }

    @Benchmark
    public void jdkDeflateTest(Blackhole bh) {
        byte[] bytes = CompressUtil.compressUsingJdkDeflate(original, level);
        bh.consume(CompressUtil.decompressUsingJdkDeflate(bytes));
    }

    @Benchmark
    public void libDeflateTest(Blackhole bh) {
        try(Allocator allocator = Allocator.newDirectAllocator()) {
            MemorySegment m1 = CompressUtil.compressUsingDeflate(originalSegment, level, allocator);
            bh.consume(CompressUtil.decompressUsingDeflate(m1, allocator));
        }
    }

    @Benchmark
    public void jdkGzipTest(Blackhole bh) {
        byte[] bytes = CompressUtil.compressUsingJdkGzip(original, level);
        bh.consume(CompressUtil.decompressUsingJdkGzip(bytes));
    }

    @Benchmark
    public void libGzipTest(Blackhole bh) {
        try(Allocator allocator = Allocator.newDirectAllocator()) {
            MemorySegment m1 = CompressUtil.compressUsingGzip(originalSegment, level, allocator);
            bh.consume(CompressUtil.decompressUsingGzip(m1, allocator));
        }
    }

    public static void main(String[] args) throws RunnerException {
        runTest(CompressionJmhTest.class);
    }

}
