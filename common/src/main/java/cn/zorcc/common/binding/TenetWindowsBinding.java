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

public final class TenetWindowsBinding {
    private static final MethodHandle addressLenMethodHandle;
    private static final MethodHandle connectBlockCodeMethodHandle;
    private static final MethodHandle sendBlockCodeMethodHandle;
    private static final MethodHandle invalidSocketMethodHandle;
    private static final MethodHandle epollCreateMethodHandle;
    private static final MethodHandle epollCtlMethodHandle;
    private static final MethodHandle epollWaitMethodHandle;
    private static final MethodHandle epollCloseMethodHandle;
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
    private static final MethodHandle listenMethodHandle;
    private static final MethodHandle recvMethodHandle;
    private static final MethodHandle sendMethodHandle;
    private static final MethodHandle connectMethodHandle;
    private static final MethodHandle closeSocketMethodHandle;
    private static final MethodHandle shutdownWriteMethodHandle;
    private static final MethodHandle wsaGetLastErrorMethodHandle;
    private static final MethodHandle wsaCleanUpMethodHandle;

    static {
        SymbolLookup symbolLookup = NativeUtil.loadLibrary(Constants.TENET);
        addressLenMethodHandle = NativeUtil.methodHandle(symbolLookup, "w_address_len", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        connectBlockCodeMethodHandle = NativeUtil.methodHandle(symbolLookup, "w_connect_block_code", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        sendBlockCodeMethodHandle = NativeUtil.methodHandle(symbolLookup, "w_send_block_code", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        invalidSocketMethodHandle = NativeUtil.methodHandle(symbolLookup, "w_invalid_socket", FunctionDescriptor.of(ValueLayout.JAVA_LONG));
        epollCreateMethodHandle = NativeUtil.methodHandle(symbolLookup, "w_epoll_create", FunctionDescriptor.of(ValueLayout.ADDRESS));
        epollCtlMethodHandle = NativeUtil.methodHandle(symbolLookup, "w_epoll_ctl", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
        epollWaitMethodHandle = NativeUtil.methodHandle(symbolLookup, "w_epoll_wait", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        epollCloseMethodHandle = NativeUtil.methodHandle(symbolLookup, "w_epoll_close", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        addressMethodHandle = NativeUtil.methodHandle(symbolLookup, "w_address", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        portMethodHandle = NativeUtil.methodHandle(symbolLookup, "w_port", FunctionDescriptor.of(ValueLayout.JAVA_SHORT, ValueLayout.ADDRESS));
        socketCreateMethodHandle = NativeUtil.methodHandle(symbolLookup, "w_socket_create", FunctionDescriptor.of(ValueLayout.JAVA_LONG));
        acceptMethodHandle = NativeUtil.methodHandle(symbolLookup, "w_accept", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        setSockAddrMethodHandle = NativeUtil.methodHandle(symbolLookup, "w_set_sock_addr", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_SHORT));
        setReuseAddrMethodHandle = NativeUtil.methodHandle(symbolLookup, "w_set_reuse_addr", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));
        setKeepAliveMethodHandle = NativeUtil.methodHandle(symbolLookup, "w_set_keep_alive", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));
        setTcpNoDelayMethodHandle = NativeUtil.methodHandle(symbolLookup, "w_set_tcp_no_delay", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));
        getErrOptMethodHandle = NativeUtil.methodHandle(symbolLookup, "w_get_err_opt", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
        setNonBlockingMethodHandle = NativeUtil.methodHandle(symbolLookup, "w_set_nonblocking", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));
        bindMethodHandle = NativeUtil.methodHandle(symbolLookup, "w_bind", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        connectMethodHandle = NativeUtil.methodHandle(symbolLookup, "w_connect", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        listenMethodHandle = NativeUtil.methodHandle(symbolLookup, "w_listen", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));
        recvMethodHandle = NativeUtil.methodHandle(symbolLookup, "w_recv", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        sendMethodHandle = NativeUtil.methodHandle(symbolLookup, "w_send", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        closeSocketMethodHandle = NativeUtil.methodHandle(symbolLookup, "w_close_socket", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));
        shutdownWriteMethodHandle = NativeUtil.methodHandle(symbolLookup, "w_shutdown_write", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));
        wsaGetLastErrorMethodHandle = NativeUtil.methodHandle(symbolLookup, "w_get_last_error", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        wsaCleanUpMethodHandle = NativeUtil.methodHandle(symbolLookup, "w_clean_up", FunctionDescriptor.of(ValueLayout.JAVA_INT));
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

    public static int invalidSocket() {
        try{
            return (int) invalidSocketMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static MemorySegment epollCreate() {
        try{
            return (MemorySegment) epollCreateMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int epollCtl(MemorySegment handle, int op, long socket, MemorySegment event) {
        try{
            return (int) epollCtlMethodHandle.invokeExact(handle, op, socket, event);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int epollWait(MemorySegment handle, MemorySegment events, int maxEvents, int timeout) {
        try{
            return (int) epollWaitMethodHandle.invokeExact(handle, events, maxEvents, timeout);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int epollClose(MemorySegment handle) {
        try{
            return (int) epollCloseMethodHandle.invokeExact(handle);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int address(MemorySegment clientAddr, MemorySegment addrStr, int len) {
        try{
            return (int) addressMethodHandle.invokeExact(clientAddr, addrStr, len);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static short port(MemorySegment clientAddr) {
        try{
            return (short) portMethodHandle.invokeExact(clientAddr);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static long socketCreate() {
        try{
            return (long) socketCreateMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static long accept(long socket, MemorySegment clientAddr, int clientAddrSize) {
        try{
            return (long) acceptMethodHandle.invokeExact(socket, clientAddr, clientAddrSize);
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

    public static int setReuseAddr(long socket, int value) {
        try{
            return (int) setReuseAddrMethodHandle.invokeExact(socket, value);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int setKeepAlive(long socket, int value) {
        try{
            return (int) setKeepAliveMethodHandle.invokeExact(socket, value);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int setTcpNoDelay(long socket, int value) {
        try{
            return (int) setTcpNoDelayMethodHandle.invokeExact(socket, value);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int getErrOpt(long socket, MemorySegment ptr) {
        try{
            return (int) getErrOptMethodHandle.invokeExact(socket, ptr);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int setNonBlocking(long socket) {
        try{
            return (int) setNonBlockingMethodHandle.invokeExact(socket);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int bind(long socket, MemorySegment sockAddr, int size) {
        try{
            return (int) bindMethodHandle.invokeExact(socket, sockAddr, size);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int connect(long socket, MemorySegment sockAddr, int size) {
        try{
            return (int) connectMethodHandle.invokeExact(socket, sockAddr, size);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int listen(long socket, int backlog) {
        try{
            return (int) listenMethodHandle.invokeExact(socket, backlog);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int recv(long socket, MemorySegment buf, int len) {
        try{
            return (int) recvMethodHandle.invokeExact(socket, buf, len);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int send(long socket, MemorySegment buf, int len) {
        try{
            return (int) sendMethodHandle.invokeExact(socket, buf, len);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int closeSocket(long socket) {
        try{
            return (int) closeSocketMethodHandle.invokeExact(socket);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int shutdownWrite(long socket) {
        try{
            return (int) shutdownWriteMethodHandle.invokeExact(socket);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int wsaGetLastError() {
        try{
            return (int) wsaGetLastErrorMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int wsaCleanUp() {
        try{
            return (int) wsaCleanUpMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }
}
