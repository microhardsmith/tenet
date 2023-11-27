package cn.zorcc.common.metrics;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.OsType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

public final class Metrics {
    /**
     *  Note that load average will not be supported on windows, thus always returning -1d
     */
    private static final MethodHandle loadAverage;
    private static final int DIMENSION = 3;

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
            return (int) loadAverage.invokeExact(ptr, DIMENSION);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED);
        }
    }

    public static CpuLoadAverage getLoadAverage() {
        if(loadAverage == null) {
            return CpuLoadAverage.UNSUPPORTED;
        }
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment ptr = arena.allocateArray(ValueLayout.JAVA_DOUBLE);
            int n = Metrics.getLoadAverage(ptr);
            if(n < DIMENSION) {
                throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED);
            }
            double l1 = NativeUtil.getDouble(ptr, 0);
            double l2 = NativeUtil.getDouble(ptr, NativeUtil.getDoubleSize());
            double l3 = NativeUtil.getDouble(ptr, 2 * NativeUtil.getDoubleSize());
            return new CpuLoadAverage(l1, l2, l3);
        }
    }
}
