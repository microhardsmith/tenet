package cn.zorcc.common.binding;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

public final class TenetLinuxBinding {
    private static final MethodHandle addressLenMethodHandle;
    private static final MethodHandle connectBlockCodeMethodHandle;
    private static final MethodHandle sendBlockCodeMethodHandle;
    private static final MethodHandle epollCreateMethodHandle;
    private static final MethodHandle epollCtlMethodHandle;
    private static final MethodHandle epollWaitMethodHandle;
    private static final MethodHandle addressMethodHandle;
    private static final MethodHandle portMethodHandle;
    private static final MethodHandle socketCreateMethodHandle;
    private static final MethodHandle acceptMethodHandle;
    private static final MethodHandle setSockAddrMethodHandle;
    private static final MethodHandle setReuseAddrMethodHandle;
    private static final MethodHandle setKeepAliveMethodHandle;
    private static final MethodHandle setTcpNoDelayMethodHandle;
    private static final MethodHandle getErrOptMethodHandle;
    private static final MethodHandle setNonBlockingMethodHandle;
    private static final MethodHandle bindMethodHandle;
    private static final MethodHandle connectMethodHandle;
    private static final MethodHandle listenMethodHandle;
    private static final MethodHandle recvMethodHandle;
    private static final MethodHandle sendMethodHandle;
    private static final MethodHandle closeMethodHandle;
    private static final MethodHandle shutdownWriteMethodHandle;
    private static final MethodHandle errnoMethodHandle;

    static {
        SymbolLookup symbolLookup = NativeUtil.loadLibrary(Constants.TENET);
        addressLenMethodHandle = NativeUtil.methodHandle(symbolLookup, "l_address_len", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        connectBlockCodeMethodHandle = NativeUtil.methodHandle(symbolLookup, "l_connect_block_code", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        sendBlockCodeMethodHandle = NativeUtil.methodHandle(symbolLookup, "l_send_block_code", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        epollCreateMethodHandle = NativeUtil.methodHandle(symbolLookup, "l_epoll_create", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        epollCtlMethodHandle = NativeUtil.methodHandle(symbolLookup, "l_epoll_ctl", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        epollWaitMethodHandle = NativeUtil.methodHandle(symbolLookup, "l_epoll_wait", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        addressMethodHandle = NativeUtil.methodHandle(symbolLookup, "l_address", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        portMethodHandle = NativeUtil.methodHandle(symbolLookup, "l_port", FunctionDescriptor.of(ValueLayout.JAVA_SHORT, ValueLayout.ADDRESS));
        socketCreateMethodHandle = NativeUtil.methodHandle(symbolLookup, "l_socket_create", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        acceptMethodHandle = NativeUtil.methodHandle(symbolLookup, "l_accept", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        setSockAddrMethodHandle = NativeUtil.methodHandle(symbolLookup, "l_set_sock_addr", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_SHORT));
        setReuseAddrMethodHandle = NativeUtil.methodHandle(symbolLookup, "l_set_reuse_addr", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        setKeepAliveMethodHandle = NativeUtil.methodHandle(symbolLookup, "l_set_keep_alive", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        setTcpNoDelayMethodHandle = NativeUtil.methodHandle(symbolLookup, "l_set_tcp_no_delay", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        getErrOptMethodHandle = NativeUtil.methodHandle(symbolLookup, "l_get_err_opt", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        setNonBlockingMethodHandle = NativeUtil.methodHandle(symbolLookup, "l_set_nonblocking", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        bindMethodHandle = NativeUtil.methodHandle(symbolLookup, "l_bind", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        connectMethodHandle = NativeUtil.methodHandle(symbolLookup, "l_connect", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        listenMethodHandle = NativeUtil.methodHandle(symbolLookup, "l_listen", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        recvMethodHandle = NativeUtil.methodHandle(symbolLookup, "l_recv", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        sendMethodHandle = NativeUtil.methodHandle(symbolLookup, "l_send", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        closeMethodHandle = NativeUtil.methodHandle(symbolLookup, "l_close", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        shutdownWriteMethodHandle = NativeUtil.methodHandle(symbolLookup, "l_shutdown_write", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        errnoMethodHandle = NativeUtil.methodHandle(symbolLookup, "l_errno", FunctionDescriptor.of(ValueLayout.JAVA_INT));
    }

    public static int addressLen() {
        try{
            return (int) addressLenMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int connectBlockCode() {
        try{
            return (int) connectBlockCodeMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int sendBlockCode() {
        try{
            return (int) sendBlockCodeMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int epollCreate() {
        try{
            return (int) epollCreateMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int epollCtl(int epfd, int op, int socket, MemorySegment ev) {
        try{
            return (int) epollCtlMethodHandle.invokeExact(epfd, op, socket, ev);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int epollWait(int epfd, MemorySegment events, int maxEvents, int timeout) {
        try{
            return (int) epollWaitMethodHandle.invokeExact(epfd, events, maxEvents, timeout);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int address(MemorySegment sockAddr, MemorySegment addrStr, int len) {
        try{
            return (int) addressMethodHandle.invokeExact(sockAddr, addrStr, len);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static short port(MemorySegment sockAddr) {
        try{
            return (short) portMethodHandle.invokeExact(sockAddr);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int socketCreate() {
        try{
            return (int) socketCreateMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int accept(int socket, MemorySegment clientAddr, int clientAddrSize) {
        try{
            return (int) acceptMethodHandle.invokeExact(socket, clientAddr, clientAddrSize);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int setSockAddr(MemorySegment sockAddr, MemorySegment address, short port) {
        try{
            return (int) setSockAddrMethodHandle.invokeExact(sockAddr, address, port);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int setReuseAddr(int socket, int value) {
        try{
            return (int) setReuseAddrMethodHandle.invokeExact(socket, value);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int setKeepAlive(int socket, int value) {
        try{
            return (int) setKeepAliveMethodHandle.invokeExact(socket, value);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int setTcpNoDelay(int socket, int value) {
        try{
            return (int) setTcpNoDelayMethodHandle.invokeExact(socket, value);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int getErrOpt(int socket, MemorySegment ptr) {
        try{
            return (int) getErrOptMethodHandle.invokeExact(socket, ptr);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int setNonBlocking(int socket) {
        try{
            return (int) setNonBlockingMethodHandle.invokeExact(socket);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int bind(int socket, MemorySegment sockAddr, int size) {
        try{
            return (int) bindMethodHandle.invokeExact(socket, sockAddr, size);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int connect(int socket, MemorySegment sockAddr, int size) {
        try{
            return (int) connectMethodHandle.invokeExact(socket, sockAddr, size);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int listen(int socket, int backlog) {
        try{
            return (int) listenMethodHandle.invokeExact(socket, backlog);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static long recv(int socket, MemorySegment buf, long len) {
        try{
            return (long) recvMethodHandle.invokeExact(socket, buf, len);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static long send(int socket, MemorySegment buf, long len) {
        try{
            return (long) sendMethodHandle.invokeExact(socket, buf, len);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int close(int fd) {
        try{
            return (int) closeMethodHandle.invokeExact(fd);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int shutdownWrite(int fd) {
        try{
            return (int) shutdownWriteMethodHandle.invokeExact(fd);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int errno() {
        try{
            return (int) errnoMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }
}
