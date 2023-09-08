package cn.zorcc.common.jmh;

import cn.zorcc.common.util.CompressUtil;
import cn.zorcc.common.util.NativeUtil;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.RunnerException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;


public class CompressionJmhTest extends JmhTest {
    private static final Arena globalArena = Arena.global();
    private byte[] original;
    private MemorySegment originalSegment;

    @Setup
    public void setup() {
        try(InputStream stream = CompressionJmhTest.class.getResourceAsStream("/large.json")){
            assert stream != null;
            this.original = stream.readAllBytes();
            this.originalSegment = globalArena.allocateArray(ValueLayout.JAVA_BYTE, original.length);
            for(int i = 0; i < original.length; i++) {
                NativeUtil.setByte(originalSegment, i, original[i]);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public CompressionJmhTest() {

    }

    @Benchmark
    public void jdkDeflateTest(Blackhole bh) {
        byte[] bytes = CompressUtil.compressUsingJdkDeflate(original, 9);
        bh.consume(CompressUtil.decompressUsingJdkDeflate(bytes));
    }

    @Benchmark
    public void libDeflateTest(Blackhole bh) {
        MemorySegment m1 = CompressUtil.compressUsingDeflate(originalSegment, 9);
        bh.consume(CompressUtil.decompressUsingDeflate(m1));
    }

    @Benchmark
    public void jdkGzipTest(Blackhole bh) {
        byte[] bytes = CompressUtil.compressUsingJdkGzip(original, 9);
        bh.consume(CompressUtil.decompressUsingJdkGzip(bytes));
    }

    @Benchmark
    public void libGzipTest(Blackhole bh) {
        MemorySegment m1 = CompressUtil.compressUsingGzip(originalSegment, 9);
        bh.consume(CompressUtil.decompressUsingGzip(m1));
    }

    public static void main(String[] args) throws RunnerException {
        runTest(CompressionJmhTest.class);
    }

}
