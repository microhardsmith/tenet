package cn.zorcc.common.network;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentScope;
import java.lang.foreign.ValueLayout;
import java.util.concurrent.TimeUnit;

/**
 *   Timeout abstraction for different operating system
 */
public record Timeout(
        int val,
        MemorySegment ptr
) {
    private static final MemoryLayout timespecLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_LONG.withName("tv_sec"),
            ValueLayout.JAVA_LONG.withName("tv_nsec")
    );
    private static final long secOffset = timespecLayout.byteOffset(MemoryLayout.PathElement.groupElement("tv_sec"));
    private static final long nsecOffset = timespecLayout.byteOffset(MemoryLayout.PathElement.groupElement("tv_nsec"));

    public static Timeout of(int milliseconds) {
        if(NativeUtil.isWindows() || NativeUtil.isLinux()) {
            return new Timeout(milliseconds, null);
        }else if(NativeUtil.isMacos()) {
            MemorySegment ptr = MemorySegment.allocateNative(timespecLayout, SegmentScope.global());
            NativeUtil.setLong(ptr, secOffset, milliseconds / 1000);
            NativeUtil.setLong(ptr, nsecOffset, TimeUnit.MILLISECONDS.toNanos(milliseconds % 1000));
            return new Timeout(Integer.MIN_VALUE, ptr);
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, "Unrecognized operating system");
        }
    }
}
