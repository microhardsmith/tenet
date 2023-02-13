package cn.zorcc.common.net;

import cn.zorcc.common.NativeHelper;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *  windows wepoll implementation
 */
public class WinNative {
    /**
     *  corresponding to enum EPOLL_EVENTS in wepoll.h
     */
    public static final int EPOLL_IN = 1;
    public static final int EPOLL_OUT = 1 << 2;
    public static final int EPOLL_ERR = 1 << 3;
    public static final int EPOLL_HUP = 1 << 4;
    /**
     *  动态链接库路径
     */
    private static final String LIB_PATH = "/lib_win.dll";
    /**
     *  corresponding to union epoll_data in wepoll.h
     */
    public static final MemoryLayout epollDataLayout = MemoryLayout.unionLayout(
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
    public static final MemoryLayout epollEventLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("events"),
            MemoryLayout.paddingLayout(32),
            epollDataLayout.withName("data")
    );
    public static final VarHandle eventsHandle = epollEventLayout.varHandle(MemoryLayout.PathElement.groupElement("events"));
    public static final VarHandle fdHandle = epollEventLayout.varHandle(MemoryLayout.PathElement.groupElement("data"), MemoryLayout.PathElement.groupElement("fd"));
    /**
     *  corresponding to struct sockaddr_in
     */
    public static final MemoryLayout sockAddrLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_SHORT.withName("sin_family"),
            ValueLayout.JAVA_SHORT.withName("sin_port"),
            ValueLayout.JAVA_INT.withName("sin_addr"),
            MemoryLayout.sequenceLayout(8, ValueLayout.JAVA_BYTE)
    );
    public static final int sockAddrSize = (int) sockAddrLayout.byteSize();
    private static final AtomicBoolean instanceFlag = new AtomicBoolean(false);
    private final MethodHandle epollCreateMethodHandle;
    private final MethodHandle epollCtlAddMethodHandle;
    private final MethodHandle epollCtlDelMethodHandle;
    private final MethodHandle epollWaitMethodHandle;
    private final MethodHandle epollCloseMethodHandle;
    private final MethodHandle acceptMethodHandle;
    private final MethodHandle addressLenMethodHandle;
    private final MethodHandle addressMethodHandle;
    private final MethodHandle portMethodHandle;
    private final MethodHandle socketCreateMethodHandle;
    private final MethodHandle setReuseAddrMethodHandle;
    private final MethodHandle setKeepAliveMethodHandle;
    private final MethodHandle setTcpNoDelayMethodHandle;
    private final MethodHandle setNonBlockingMethodHandle;
    private final MethodHandle setSockAddrMethodHandle;
    private final MethodHandle bindMethodHandle;
    private final MethodHandle listenMethodHandle;
    private final MethodHandle recvMethodHandle;
    private final MethodHandle wsaGetLastErrorMethodHandle;
    private final MethodHandle wsaCleanUpMethodHandle;

    public WinNative() {
        if(!instanceFlag.compareAndSet(false, true)) {
            throw new FrameworkException(ExceptionType.NATIVE, "WinNative has been initialized");
        }
        SymbolLookup symbolLookup = NativeHelper.loadLibraryFromResource(LIB_PATH);
        this.epollCreateMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "w_epoll_create", FunctionDescriptor.of(ValueLayout.ADDRESS));
        this.epollCtlAddMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "w_epoll_ctl_add", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
        this.epollCtlDelMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "w_epoll_ctl_del", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        this.epollWaitMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "w_epoll_wait", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        this.epollCloseMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "w_epoll_close", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        this.acceptMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "w_accept", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        this.addressLenMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "w_address_len", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        this.addressMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "w_address", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        this.portMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "w_port", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        this.socketCreateMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "w_socket_create", FunctionDescriptor.of(ValueLayout.JAVA_LONG));
        this.setReuseAddrMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "w_set_reuse_addr", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_BOOLEAN));
        this.setKeepAliveMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "w_set_keep_alive", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_BOOLEAN));
        this.setTcpNoDelayMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "w_set_tcp_no_delay", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_BOOLEAN));
        this.setNonBlockingMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "w_set_nonblocking", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));
        this.setSockAddrMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "w_set_sock_addr", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        this.bindMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "w_bind", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));
        this.listenMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "w_listen", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));
        this.recvMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "w_recv", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        this.wsaGetLastErrorMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "wsa_get_last_error", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        this.wsaCleanUpMethodHandle = NativeHelper.methodHandle(symbolLookup,
                "wsa_clean_up", FunctionDescriptor.of(ValueLayout.JAVA_INT));
    }

    /**
     *  corresponding to `void* w_epoll_create()`
     */
    public MemorySegment epollCreate() {
        try{
            return (MemorySegment) epollCreateMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking epollCreate()", throwable);
        }
    }

    /**
     *  corresponding to `int w_epoll_ctl_add(void* handle, SOCKET socket, struct epoll_event* event)`
     */
    public int epollCtlAdd(MemorySegment handle, long socket, MemorySegment event) {
        try{
            return (int) epollCtlAddMethodHandle.invokeExact(handle, socket, event);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking epollCtlAdd()", throwable);
        }
    }

    /**
     *  corresponding to `int w_epoll_ctl_del(void* handle, SOCKET socket)`
     */
    public int epollCtlDel(MemorySegment handle, long socket) {
        try{
            return (int) epollCtlDelMethodHandle.invokeExact(handle, socket);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking epollCtlDel()", throwable);
        }
    }

    /**
     *  corresponding to `int w_epoll_wait(void* handle, struct epoll_event* events, int maxevents, int timeout)`
     */
    public int epollWait(MemorySegment handle, MemorySegment events, int maxEvents, int timeout) {
        try{
            return (int) epollWaitMethodHandle.invokeExact(handle, events, maxEvents, timeout);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking epollWait()", throwable);
        }
    }

    /**
     *  corresponding to `int w_epoll_close(void* handle)`
     */
    public int epollClose(MemorySegment handle) {
        try{
            return (int) epollCloseMethodHandle.invokeExact(handle);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking epollClose()", throwable);
        }
    }

    /**
     *  corresponding to `int w_accept(SOCKET socket, struct sockaddr_in* clientAddr, int clientAddrSize)`
     */
    public int accept(long socket, MemorySegment clientAddr, int clientAddrSize) {
        try{
            return (int) acceptMethodHandle.invokeExact(socket, clientAddr, clientAddrSize);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking accept()", throwable);
        }
    }

    /**
     *  corresponding to `int w_address_len()`
     */
    public int addressLen() {
        try{
            return (int) addressLenMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking address()", throwable);
        }
    }

    /**
     *  corresponding to `int w_address(struct sockaddr_in* clientAddr, char* addrStr, int len)`
     */
    public int address(MemorySegment clientAddr, MemorySegment addrStr, int len) {
        try{
            return (int) addressMethodHandle.invokeExact(clientAddr, addrStr, len);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking address()", throwable);
        }
    }

    /**
     *  corresponding to `int w_port(struct sockaddr_in* clientAddr)`
     */
    public int port(MemorySegment clientAddr) {
        try{
            return (int) portMethodHandle.invokeExact(clientAddr);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking port()", throwable);
        }
    }

    /**
     *  corresponding to `SOCKET w_socket_create()`
     */
    public long socketCreate() {
        try{
            return (long) socketCreateMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking socketCreate()", throwable);
        }
    }

    /**
     *  corresponding to `int w_set_reuse_addr(SOCKET socket, boolean value)`
     */
    public int setReuseAddr(long socket, boolean value) {
        try{
            return (int) setReuseAddrMethodHandle.invokeExact(socket, value);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setReuseAddr()", throwable);
        }
    }

    /**
     *  corresponding to `int w_set_keep_alive(SOCKET socket, boolean value)`
     */
    public int setKeepAlive(long socket, boolean value) {
        try{
            return (int) setKeepAliveMethodHandle.invokeExact(socket, value);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setKeepAlive()", throwable);
        }
    }

    /**
     *  corresponding to `int w_set_tcp_no_delay(SOCKET socket, boolean value)`
     */
    public int setTcpNoDelay(long socket, boolean value) {
        try{
            return (int) setTcpNoDelayMethodHandle.invokeExact(socket, value);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setTcpNoDelay()", throwable);
        }
    }

    /**
     *  corresponding to `int w_set_nonblocking(SOCKET socket)`
     */
    public int setNonBlocking(long socket) {
        try{
            return (int) setNonBlockingMethodHandle.invokeExact(socket);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setNonBlocking()", throwable);
        }
    }

    /**
     *  corresponding to `int w_set_sock_addr(struct sockaddr_in* sockAddr, char* address, int port)`
     */
    public int setSockAddr(MemorySegment sockAddr, MemorySegment address, int port) {
        try{
            return (int) setSockAddrMethodHandle.invokeExact(sockAddr, address, port);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setSockAddr()", throwable);
        }
    }

    /**
     *  corresponding to `int w_bind(struct sockaddr_in* sockAddr, SOCKET socket, int size)`
     */
    public int bind(MemorySegment sockAddr, long socket, int size) {
        try{
            return (int) bindMethodHandle.invokeExact(sockAddr, socket, size);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking bind()", throwable);
        }
    }

    /**
     *  corresponding to `int w_listen(SOCKET socket, int backlog)`
     */
    public int listen(long socket, int backlog) {
        try{
            return (int) listenMethodHandle.invokeExact(socket, backlog);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking listen()", throwable);
        }
    }

    /**
     *  corresponding to `int w_recv(SOCKET socket, char* buf, int len)`
     */
    public int recv(long socket, MemorySegment buf, int len) {
        try{
            return (int) recvMethodHandle.invokeExact(socket, buf, len);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking recv()", throwable);
        }
    }

    /**
     *  corresponding to `int wsa_get_last_error()`
     */
    public int getLastError() {
        try{
            return (int) wsaGetLastErrorMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking wsaGetLastError()", throwable);
        }
    }

    /**
     *  corresponding to `int wsa_clean_up()`
     */
    public int cleanUp() {
        try{
            return (int) wsaCleanUpMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking wsaCleanUp()", throwable);
        }
    }

    /**
     *  win错误处理,关闭epoll句柄与wsa库
     */
    public void clean(MemorySegment handle) {
        int closeResult = epollClose(handle);
        if(closeResult == -1) {
            int err = getLastError();
            throw new FrameworkException(ExceptionType.NET, "Closing epoll failed with error code : %d", err);
        }
        int cleanUpResult = cleanUp();
        if(cleanUpResult == -1) {
            int err = getLastError();
            throw new FrameworkException(ExceptionType.NET, "Wsa clean up failed with error code : %d", err);
        }
    }

}
