package cn.zorcc.common.jmh;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.runner.RunnerException;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

/**
 *   Conclusion : MemorySegment.copy() is a lot faster than invoking memcpy(), that should be some magic there
 */
public class MemcpyTest extends JmhTest {
    private static final int SIZE = 1000;
    private Arena arena;
    private MemorySegment source;
    private MemorySegment dest;
    private MemorySegment sourceHeap;
    private MemorySegment destHeap;
    private static final MethodHandle memcpyHandle;

    static {
        memcpyHandle = NativeUtil.nativeMethodHandle("memcpy", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), Linker.Option.critical(true));
    }

    private static MemorySegment memcpy(MemorySegment dest, MemorySegment src, long count) {
        try{
            return (MemorySegment) memcpyHandle.invokeExact(dest, src, count);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED);
        }
    }

    @Setup
    public void setup() {
        arena = Arena.ofConfined();
        source = arena.allocate(ValueLayout.JAVA_BYTE, SIZE);
        dest = arena.allocate(ValueLayout.JAVA_BYTE, SIZE);
        source.fill((byte) '1');
        dest.fill((byte) '3');
        sourceHeap = MemorySegment.ofArray(new byte[SIZE]);
        destHeap = MemorySegment.ofArray(new byte[SIZE]);
        sourceHeap.fill((byte) '2');
        destHeap.fill((byte) '4');
    }

    @Benchmark
    public void testMemNativeCopy() {
        for(int i = 0; i < SIZE; i++) {
            memcpy(dest, source, i);
        }
    }

    @Benchmark
    public void testJdkNativeCopy() {
        for(int i = 0; i < SIZE; i++) {
            MemorySegment.copy(source, 0L, dest, 0L, i);
        }
    }

    @Benchmark
    public void testMemHeapToNativeNativeCopy() {
        for(int i = 0; i < SIZE; i++) {
            memcpy(dest, sourceHeap, i);
        }
    }

    @Benchmark
    public void testJdkHeapToNativeNativeCopy() {
        for(int i = 0; i < SIZE; i++) {
            MemorySegment.copy(sourceHeap, 0L, dest, 0L, i);
        }
    }

    @Benchmark
    public void testMemNativeToHeapCopy() {
        for(int i = 0; i < SIZE; i++) {
            memcpy(destHeap, source, i);
        }
    }

    @Benchmark
    public void testJdkNativeToHeapCopy() {
        for(int i = 0; i < SIZE; i++) {
            MemorySegment.copy(source, 0L, destHeap, 0L, i);
        }
    }

    @Benchmark
    public void testMemHeapCopy() {
        for(int i = 0; i < SIZE; i++) {
            memcpy(destHeap, sourceHeap, i);
        }
    }

    @Benchmark
    public void testJdkHeapCopy() {
        for(int i = 0; i < SIZE; i++) {
            MemorySegment.copy(sourceHeap, 0L, destHeap, 0L, i);
        }
    }

    @Benchmark
    public void testArrayCopy() {
        byte[] b1 = (byte[]) sourceHeap.heapBase().orElseThrow();
        byte[] b2 = (byte[]) destHeap.heapBase().orElseThrow();
        for(int i = 0; i < SIZE; i++) {
            System.arraycopy(b1, 0, b2, 0, i);
        }
    }



    @TearDown
    public void tearDown() {
        arena.close();
    }

    public static void main(String[] args) throws RunnerException {
        runTest(MemcpyTest.class);
    }
}
