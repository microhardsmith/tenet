package cn.zorcc.common.net.linux;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicBoolean;

public class LinuxNative {
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
    private final MethodHandle closeMethodHandle;
    private final MethodHandle errnoMethodHandle;

    public LinuxNative() {
        if(!instanceFlag.compareAndSet(false, true)) {
            throw new FrameworkException(ExceptionType.NATIVE, "LinuxNative has been initialized");
        }
        SymbolLookup symbolLookup = NativeUtil.loadLibraryFromResource(NativeUtil.commonLib());
        this.epollCreateMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_epoll_create", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        this.epollCtlAddMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_epoll_ctl_add", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        this.epollCtlDelMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_epoll_ctl_del", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        this.epollWaitMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_epoll_wait", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        this.addressLenMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_address_len", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        this.addressMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_address", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        this.portMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_port", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        this.socketCreateMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_socket_create", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        this.acceptMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_accept", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        this.setSockAddrMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_set_sock_addr", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        this.setReuseAddrMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_set_reuse_addr", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        this.setKeepAliveMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_set_keep_alive", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        this.setTcpNoDelayMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_set_tcp_no_delay", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        this.setNonBlockingMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_set_nonblocking", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        this.bindMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_bind", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        this.listenMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_listen", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        this.recvMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_recv", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        this.closeMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_close", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        this.errnoMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_errno", FunctionDescriptor.of(ValueLayout.JAVA_INT));
    }

    /**
     *  corresponding to `int l_epoll_create()`
     */
    public int epollCreate() {
        try{
            return (int) epollCreateMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking epollCreate()", throwable);
        }
    }

    /**
     *  corresponding to `int l_epoll_ctl_add(int epfd, int socket, struct epoll_event* ev)`
     */
    public int epollCtlAdd(int epfd, int socket, MemorySegment ev) {
        try{
            return (int) epollCtlAddMethodHandle.invokeExact(epfd, socket, ev);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking epollCtlAdd()", throwable);
        }
    }

    /**
     *  corresponding to `int l_epoll_ctl_del(int epfd, int socket)`
     */
    public int epollCtlDel(int epfd, int socket) {
        try{
            return (int) epollCtlDelMethodHandle.invokeExact(epfd, socket);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking epollCtlDel()", throwable);
        }
    }

    /**
     *  corresponding to `int l_epoll_wait(int epfd, struct epoll_event* events, int maxEvents, int timeout)`
     */
    public int epollWait(int epfd, MemorySegment events, int maxEvents, int timeout) {
        try{
            return (int) epollWaitMethodHandle.invokeExact(epfd, events, maxEvents, timeout);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking epollWait()", throwable);
        }
    }

    /**
     *  corresponding to `socklen_t l_address_len()`
     */
    public int addressLen() {
        try{
            return (int) addressLenMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking addressLen()", throwable);
        }
    }

    /**
     *  corresponding to `int l_address(struct sockaddr_in* sockAddr, char* addrStr, socklen_t len)`
     */
    public int address(MemorySegment sockAddr, MemorySegment addrStr, int len) {
        try{
            return (int) addressMethodHandle.invokeExact(sockAddr, addrStr, len);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking address()", throwable);
        }
    }

    /**
     *  corresponding to `int l_port(struct sockaddr_in* sockAddr)`
     */
    public int port(MemorySegment sockAddr) {
        try{
            return (int) portMethodHandle.invokeExact(sockAddr);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking port()", throwable);
        }
    }

    /**
     *  corresponding to `int l_socket_create()`
     */
    public int socketCreate() {
        try{
            return (int) socketCreateMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking socketCreate()", throwable);
        }
    }

    /**
     *  corresponding to `int l_accept(int socket, struct sockaddr_in* clientAddr, socklen_t clientAddrSize)`
     */
    public int accept(int socket, MemorySegment clientAddr, int clientAddrSize) {
        try{
            return (int) acceptMethodHandle.invokeExact(socket, clientAddr, clientAddrSize);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking accept()", throwable);
        }
    }

    /**
     *  corresponding to `int l_set_sock_addr(struct sockaddr_in* sockAddr, char* address, int port)`
     */
    public int setSockAddr(MemorySegment sockAddr, MemorySegment address, int port) {
        try{
            return (int) setSockAddrMethodHandle.invokeExact(sockAddr, address, port);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setSockAddr()", throwable);
        }
    }

    /**
     *  corresponding to `int l_set_reuse_addr(int socket, int value)`
     */
    public int setReuseAddr(int socket, int value) {
        try{
            return (int) setReuseAddrMethodHandle.invokeExact(socket, value);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setReuseAddr()", throwable);
        }
    }

    /**
     *  corresponding to `int l_set_keep_alive(int socket, int value)`
     */
    public int setKeepAlive(int socket, int value) {
        try{
            return (int) setKeepAliveMethodHandle.invokeExact(socket, value);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setKeepAlive()", throwable);
        }
    }

    /**
     *  corresponding to `int l_set_tcp_no_delay(int socket, int value)`
     */
    public int setTcpNoDelay(int socket, int value) {
        try{
            return (int) setTcpNoDelayMethodHandle.invokeExact(socket, value);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setTcpNoDelay()", throwable);
        }
    }

    /**
     *  corresponding to `int l_set_nonblocking(int socket)`
     */
    public int setNonBlocking(int socket) {
        try{
            return (int) setNonBlockingMethodHandle.invokeExact(socket);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setNonBlocking()", throwable);
        }
    }

    /**
     *  corresponding to `int l_bind(struct sockaddr_in* sockAddr, int socket, socklen_t size)`
     */
    public int bind(MemorySegment sockAddr, int socket, int size) {
        try{
            return (int) bindMethodHandle.invokeExact(sockAddr, socket, size);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking bind()", throwable);
        }
    }

    /**
     *  corresponding to `int l_listen(int socket, int backlog)`
     */
    public int listen(int socket, int backlog) {
        try{
            return (int) listenMethodHandle.invokeExact(socket, backlog);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking listen()", throwable);
        }
    }

    /**
     *  corresponding to `ssize_t l_recv(int socket, void* buf, size_t len)`
     */
    public int recv(int socket, MemorySegment buf, int len) {
        try{
            return (int) recvMethodHandle.invokeExact(socket, buf, len);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking recv()", throwable);
        }
    }

    /**
     *  corresponding to `int l_close(int fd)`
     */
    public int close(int socket) {
        try{
            return (int) closeMethodHandle.invokeExact(socket);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking close()", throwable);
        }
    }

    /**
     *  corresponding to `int l_errno()`
     */
    public int errno() {
        try{
            return (int) errnoMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking errno()", throwable);
        }
    }

}
