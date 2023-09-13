package cn.zorcc.common.jmh;

import cn.zorcc.common.Constants;
import cn.zorcc.common.util.NativeUtil;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.RunnerException;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * Benchmark                                          (size)  Mode  Cnt     Score   Error  Units
 * MemorySegmentTest.testHeapAccess                        5  avgt   50     5.492 ± 0.022  ns/op
 * MemorySegmentTest.testHeapAccess                       20  avgt   50     8.459 ± 0.050  ns/op
 * MemorySegmentTest.testHeapAccess                      100  avgt   50    21.713 ± 0.105  ns/op
 * MemorySegmentTest.testHeapAccess                     1000  avgt   50   149.108 ± 2.673  ns/op
 * MemorySegmentTest.testHeapAccess                    10000  avgt   50  1360.910 ± 5.343  ns/op
 * MemorySegmentTest.testHeapMemorySegmentAccess           5  avgt   50     9.249 ± 0.083  ns/op
 * MemorySegmentTest.testHeapMemorySegmentAccess          20  avgt   50    12.960 ± 0.098  ns/op
 * MemorySegmentTest.testHeapMemorySegmentAccess         100  avgt   50    27.549 ± 0.170  ns/op
 * MemorySegmentTest.testHeapMemorySegmentAccess        1000  avgt   50   154.213 ± 2.204  ns/op
 * MemorySegmentTest.testHeapMemorySegmentAccess       10000  avgt   50  1381.166 ± 5.544  ns/op
 * MemorySegmentTest.testNativeMemorySegmentAccess         5  avgt   50     9.802 ± 0.045  ns/op
 * MemorySegmentTest.testNativeMemorySegmentAccess        20  avgt   50    14.268 ± 0.072  ns/op
 * MemorySegmentTest.testNativeMemorySegmentAccess       100  avgt   50    28.333 ± 0.148  ns/op
 * MemorySegmentTest.testNativeMemorySegmentAccess      1000  avgt   50   152.456 ± 0.596  ns/op
 * MemorySegmentTest.testNativeMemorySegmentAccess     10000  avgt   50  1330.936 ± 5.131  ns/op
 * MemorySegmentTest.testPureHeapMemorySegmentAccess       5  avgt   50    10.195 ± 0.042  ns/op
 * MemorySegmentTest.testPureHeapMemorySegmentAccess      20  avgt   50    14.712 ± 0.064  ns/op
 * MemorySegmentTest.testPureHeapMemorySegmentAccess     100  avgt   50    30.767 ± 0.134  ns/op
 * MemorySegmentTest.testPureHeapMemorySegmentAccess    1000  avgt   50   149.147 ± 3.205  ns/op
 * MemorySegmentTest.testPureHeapMemorySegmentAccess   10000  avgt   50  1320.839 ± 4.188  ns/op
 */
@SuppressWarnings("unused")
public class MemoryAccessTest extends JmhTest {
    private static final Arena autoArena = Arena.ofAuto();
    @Param({"5", "20", "100", "1000", "10000"})
    private int size;
    private byte[] b1;
    private byte[] b2;
    private MemorySegment b3;
    private MemorySegment b4;

    @Setup
    public void setup() {
        this.b1 = new byte[size];
        this.b2 = new byte[size];
        this.b3 = MemorySegment.ofArray(b2);
        this.b4 = autoArena.allocateArray(ValueLayout.JAVA_BYTE, size);
    }
    @Benchmark
    public void testHeapAccess(Blackhole bh) {
        for(int i = 0; i < size; i++) {
            b1[i] = Constants.B_ZERO;
        }
        for(int i = 0; i < size; i++) {
            bh.consume(b1[i]);
        }
    }

    @Benchmark
    public void testHeapMemorySegmentAccess(Blackhole bh) {
        for(int i = 0; i < size; i++) {
            NativeUtil.setByte(b3, i, Constants.B_ZERO);
        }
        for(int i = 0; i < size; i++) {
            bh.consume(b2[i]);
        }
    }

    @Benchmark
    public void testPureHeapMemorySegmentAccess(Blackhole bh) {
        for(int i = 0; i < size; i++) {
            NativeUtil.setByte(b3, i, Constants.B_ZERO);
        }
        for(int i = 0; i < size; i++) {
            bh.consume(NativeUtil.getByte(b3, i));
        }
    }

    @Benchmark
    public void testNativeMemorySegmentAccess(Blackhole bh) {
        for(int i = 0; i < size; i++) {
            NativeUtil.setByte(b4, i, Constants.B_ZERO);
        }
        for(int i = 0; i < size; i++) {
            bh.consume(NativeUtil.getByte(b4, i));
        }
    }

    public static void main(String[] args) throws RunnerException {
        runTest(MemoryAccessTest.class);
    }

}
