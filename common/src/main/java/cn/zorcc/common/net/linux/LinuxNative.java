package cn.zorcc.common.net.linux;

import cn.zorcc.common.Constants;
import cn.zorcc.common.NativeHelper;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.net.NetConfig;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicBoolean;

public class LinuxNative {
    /**
     *  动态链接库路径
     */
    private static final String LIB_PATH = "/lib_win.dll";
    /**
     *  corresponding to union epoll_data in epoll.h
     */
    public static final MemoryLayout epollDataLayout = MemoryLayout.unionLayout(
            ValueLayout.ADDRESS.withName("ptr"),
            ValueLayout.JAVA_INT.withName("fd"),
            ValueLayout.JAVA_INT.withName("u32"),
            ValueLayout.JAVA_LONG.withName("u64")
    );
    /**
     *  corresponding to struct epoll_event in epoll.h
     */
    public static final MemoryLayout epollEventLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("events"),
            MemoryLayout.paddingLayout(4 * Constants.BYTE_SIZE),
            epollDataLayout.withName("data")
    );
    public static final VarHandle eventsHandle = epollEventLayout.varHandle(MemoryLayout.PathElement.groupElement("events"));
    public static final VarHandle fdHandle = epollEventLayout.varHandle(MemoryLayout.PathElement.groupElement("data"), MemoryLayout.PathElement.groupElement("fd"));
    /**
     *  corresponding to struct sockaddr_in in in.h
     */
    public static final MemoryLayout sockAddrLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_SHORT.withName("sin_family"),
            ValueLayout.JAVA_SHORT.withName("sin_port"),
            ValueLayout.JAVA_INT.withName("sin_addr"),
            // MemoryLayout.sequenceLayout(8, ValueLayout.JAVA_BYTE)
            MemoryLayout.paddingLayout(8 * Constants.BYTE_SIZE)
    );
    public static final int sockAddrSize = (int) sockAddrLayout.byteSize();
    private static final AtomicBoolean instanceFlag = new AtomicBoolean(false);
    private final MethodHandle epollCreateMethodHandle;
    private final MethodHandle epollCtlAddMethodHandle;
    private final MethodHandle epollCtlDelMethodHandle;
    private final MethodHandle epollWaitMethodHandle;
    private final MethodHandle addressLenMethodHandle;
    private final MethodHandle addressMethodHandle;
    private final MethodHandle portMethodHandle;
    private final MethodHandle socketCreateMethodHandle;
    private final MethodHandle acceptMethodHandle;
    private final MethodHandle setSockAddrMethodHandle;
    private final MethodHandle setReuseAddrMethodHandle;
    private final MethodHandle setKeepAliveMethodHandle;
    private final MethodHandle setTcpNoDelayMethodHandle;
    private final MethodHandle setNonBlockingMethodHandle;

    private final MethodHandle bindMethodHandle;
    private final MethodHandle listenMethodHandle;
    private final MethodHandle recvMethodHandle;
    private final MethodHandle closeSocketMethodHandle;
    private final MethodHandle errnoMethodHandle;

    public LinuxNative() {
        if(!instanceFlag.compareAndSet(false, true)) {
            throw new FrameworkException(ExceptionType.NATIVE, "LinuxNative has been initialized");
        }
        SymbolLookup symbolLookup = NativeHelper.loadLibraryFromResource(LIB_PATH);
        this.epollCreateMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "l_epoll_create", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        this.epollCtlAddMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "l_epoll_ctl_add", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        this.epollCtlDelMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "l_epoll_ctl_del", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        this.epollWaitMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "l_epoll_wait", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        this.addressLenMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "l_address_len", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        this.addressMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "l_address", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        this.portMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "l_port", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        this.socketCreateMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "l_socket_create", FunctionDescriptor.of(ValueLayout.JAVA_LONG));
        this.acceptMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "l_accept", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        this.setSockAddrMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "l_set_sock_addr", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        this.setReuseAddrMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "l_set_reuse_addr", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_BOOLEAN));
        this.setKeepAliveMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "l_set_keep_alive", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_BOOLEAN));
        this.setTcpNoDelayMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "l_set_tcp_no_delay", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_BOOLEAN));
        this.setNonBlockingMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "l_set_nonblocking", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));
        this.bindMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "l_bind", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));
        this.listenMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "l_listen", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));
        this.recvMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "l_recv", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        this.closeSocketMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "l_close_socket", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));
        this.errnoMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "l_errno", FunctionDescriptor.of(ValueLayout.JAVA_INT));
    }

}
