package cn.zorcc.common.net;

import cn.zorcc.common.NativeHelper;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.PlatformUtil;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 *  windows wepoll implementation
 */
public class WinNative {
    /**
     *  动态链接库路径
     */
    private static final String LIB_PATH = "/lib_win.dll";

    private final Arena arena;
    /**
     *  corresponding to union epoll_data in wepoll.h
     */
    private final MemoryLayout wepollDataLayout = MemoryLayout.unionLayout(
            ValueLayout.ADDRESS.withName("ptr"),
            ValueLayout.JAVA_INT.withName("fd"),
            ValueLayout.JAVA_INT.withName("u32"),
            ValueLayout.JAVA_LONG.withName("u64"),
            ValueLayout.JAVA_INT.withName("sock"),
            ValueLayout.ADDRESS.withName("hnd")
    );
    /**
     *  corresponding to struct epoll_event in wepoll.h
     */
    private final MemoryLayout wepollEventLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("fd"),
            wepollDataLayout.withName("data")
    );
    /**
     *  corresponding to struct sockaddr_in
     */
    private final MemoryLayout sockAddrLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_SHORT.withName("sin_family"),
            ValueLayout.JAVA_SHORT.withName("sin_port"),
            ValueLayout.JAVA_INT.withName("sin_addr"),
            MemoryLayout.sequenceLayout(8, ValueLayout.JAVA_BYTE)
    );

    public WinNative(Arena arena) {
        this.arena = arena;
        NativeHelper.loadLibraryFromResource(LIB_PATH);

    }

    public static void main(String[] args) {
        SymbolLookup symbolLookup = NativeHelper.loadLibraryFromResource("/simple.dll");
        MemorySegment printStr = symbolLookup.find("printStr").orElseThrow();
        Linker linker = Linker.nativeLinker();
        MethodHandle methodHandle = linker.downcallHandle(printStr, FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        try(Arena a = Arena.openConfined()) {
            MemorySegment m = a.allocateArray(ValueLayout.JAVA_BYTE, "hello".getBytes(StandardCharsets.UTF_8));
            methodHandle.invokeExact(m);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public int epollCreate() {
        return 0;
    }

    public void clean() {

    }

}
