package cn.zorcc.common.metrics;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.OsType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

public final class Metrics {
    /**
     *  Note that load average will not be supported on windows, thus always returning -1d
     */
    private static final MethodHandle loadAverage;

    static {
        OsType ostype = NativeUtil.ostype();
        if(ostype == OsType.Windows) {
            loadAverage = null;
        }else if(ostype == OsType.Linux || ostype == OsType.MacOS) {
            loadAverage = NativeUtil.nativeMethodHandle("getloadavg", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        }else {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED);
        }
    }

    private static int getLoadAverage(MemorySegment ptr) {
        try{
            return (int) loadAverage.invokeExact(ptr, 3);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED);
        }
    }

    public static CpuLoadAverage getLoadAverage(SegmentAllocator allocator) {
        if(loadAverage == null) {
            return CpuLoadAverage.UNSUPPORTED;
        }
        MemorySegment ptr = allocator.allocate(ValueLayout.JAVA_DOUBLE, 3);
        int n = Metrics.getLoadAverage(ptr);
        if(n < 3) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED);
        }
        double l1 = ptr.get(ValueLayout.JAVA_DOUBLE_UNALIGNED, 0L);
        double l2 = ptr.get(ValueLayout.JAVA_DOUBLE_UNALIGNED, Double.BYTES);
        double l3 = ptr.get(ValueLayout.JAVA_DOUBLE_UNALIGNED, 2 * Double.BYTES);
        return new CpuLoadAverage(l1, l2, l3);
    }
}
