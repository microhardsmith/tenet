package cn.zorcc.common.metrics;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.enums.OsType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

public final class Metrics {
    /**
     *  Note that load average will not be supported on windows
     */
    private static final MethodHandle loadAverage;
    private static final int loadAverageCount = 3;

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

    private static int getLoadAverage(MemorySegment ptr, int size) {
        try{
            return (int) loadAverage.invokeExact(ptr, size);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED);
        }
    }

    public static CpuLoadAverage getLoadAverage() {
        if(loadAverage == null) {
            return CpuLoadAverage.UNSUPPORTED;
        }
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment ptr = arena.allocateArray(ValueLayout.JAVA_DOUBLE, loadAverageCount);
            int n = Metrics.getLoadAverage(ptr, loadAverageCount);
            if(n < 0) {
                throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED);
            }
            double l1 = n < 1 ? 0.0d : NativeUtil.getDouble(ptr, 0);
            System.out.println(l1);
            double l2 = n < 2 ? 0.0d : NativeUtil.getDouble(ptr, NativeUtil.getDoubleSize());
            System.out.println(l2);
            double l3 = n < 3 ? 0.0d : NativeUtil.getDouble(ptr, 2 * NativeUtil.getDoubleSize());
            System.out.println(l3);
            return new CpuLoadAverage(l1, l2, l3);
        }
    }
}
