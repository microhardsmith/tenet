package cn.zorcc.common.bindings;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

public final class TenetMacosBinding {
    private static final MethodHandle connectBlockCodeMethodHandle;
    private static final MethodHandle sendBlockCodeMethodHandle;
    private static final MethodHandle interruptCodeMethodHandle;
    private static final MethodHandle ipv4AddressLenMethodHandle;
    private static final MethodHandle ipv6AddressLenMethodHandle;
    private static final MethodHandle ipv4AddressSizeMethodHandle;
    private static final MethodHandle ipv6AddressSizeMethodHandle;
    private static final MethodHandle ipv4AddressAlignMethodHandle;
    private static final MethodHandle ipv6AddressAlignMethodHandle;
    private static final MethodHandle kqueueMethodHandle;
    private static final MethodHandle keventCtlMethodHandle;
    private static final MethodHandle keventWaitMethodHandle;
    private static final MethodHandle getIpv4AddressMethodHandle;
    private static final MethodHandle getIpv6AddressMethodHandle;
    private static final MethodHandle ipv4PortMethodHandle;
    private static final MethodHandle ipv6PortMethodHandle;
    private static final MethodHandle ipv4SocketCreateMethodHandle;
    private static final MethodHandle ipv6SocketCreateMethodHandle;
    private static final MethodHandle setIpv4SockAddrMethodHandle;
    private static final MethodHandle setIpv6SockAddrMethodHandle;
    private static final MethodHandle setReuseAddrMethodHandle;
    private static final MethodHandle setKeepAliveMethodHandle;
    private static final MethodHandle setTcpNoDelayMethodHandle;
    private static final MethodHandle setIpv6OnlyMethodHandle;
    private static final MethodHandle getErrOptMethodHandle;
    private static final MethodHandle setNonBlockingMethodHandle;
    private static final MethodHandle bindMethodHandle;
    private static final MethodHandle listenMethodHandle;
    private static final MethodHandle connectMethodHandle;
    private static final MethodHandle acceptMethodHandle;
    private static final MethodHandle recvMethodHandle;
    private static final MethodHandle sendMethodHandle;
    private static final MethodHandle closeMethodHandle;
    private static final MethodHandle shutdownWriteMethodHandle;

    static {
        SymbolLookup symbolLookup = NativeUtil.loadLibrary(Constants.TENET);
        connectBlockCodeMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_connect_block_code",
                FunctionDescriptor.of(ValueLayout.JAVA_INT), Linker.Option.isTrivial());
        sendBlockCodeMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_send_block_code",
                FunctionDescriptor.of(ValueLayout.JAVA_INT), Linker.Option.isTrivial());
        interruptCodeMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_interrupt_code",
                FunctionDescriptor.of(ValueLayout.JAVA_INT), Linker.Option.isTrivial());
        ipv4AddressLenMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_ipv4_address_len",
                FunctionDescriptor.of(ValueLayout.JAVA_INT), Linker.Option.isTrivial());
        ipv6AddressLenMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_ipv6_address_len",
                FunctionDescriptor.of(ValueLayout.JAVA_INT), Linker.Option.isTrivial());
        ipv4AddressSizeMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_ipv4_address_size",
                FunctionDescriptor.of(ValueLayout.JAVA_INT), Linker.Option.isTrivial());
        ipv6AddressSizeMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_ipv6_address_size",
                FunctionDescriptor.of(ValueLayout.JAVA_INT), Linker.Option.isTrivial());
        ipv4AddressAlignMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_ipv4_address_align",
                FunctionDescriptor.of(ValueLayout.JAVA_INT), Linker.Option.isTrivial());
        ipv6AddressAlignMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_ipv6_address_align",
                FunctionDescriptor.of(ValueLayout.JAVA_INT), Linker.Option.isTrivial());
        kqueueMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_kqueue",
                FunctionDescriptor.of(ValueLayout.JAVA_INT));
        keventCtlMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_kevent_ctl",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        keventWaitMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_kevent_wait",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        getIpv4AddressMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_get_ipv4_address",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        getIpv6AddressMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_get_ipv6_address",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        ipv4PortMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_ipv4_port",
                FunctionDescriptor.of(ValueLayout.JAVA_SHORT, ValueLayout.ADDRESS));
        ipv6PortMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_ipv6_port",
                FunctionDescriptor.of(ValueLayout.JAVA_SHORT, ValueLayout.ADDRESS));
        ipv4SocketCreateMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_ipv4_socket_create",
                FunctionDescriptor.of(ValueLayout.JAVA_INT));
        ipv6SocketCreateMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_ipv6_socket_create",
                FunctionDescriptor.of(ValueLayout.JAVA_INT));
        setIpv4SockAddrMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_set_ipv4_sock_addr",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_SHORT));
        setIpv6SockAddrMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_set_ipv6_sock_addr",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_SHORT));
        setReuseAddrMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_set_reuse_addr",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        setKeepAliveMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_set_keep_alive",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        setTcpNoDelayMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_set_tcp_no_delay",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        setIpv6OnlyMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_set_ipv6_only",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        getErrOptMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_get_err_opt",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        setNonBlockingMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_set_nonblocking",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        bindMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_bind",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        listenMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_listen",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        connectMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_connect",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        acceptMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_accept",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        recvMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_recv",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        sendMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_send",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        shutdownWriteMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_shutdown_write",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        closeMethodHandle = NativeUtil.methodHandle(symbolLookup, "m_close",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
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

    public static int interruptCode() {
        try{
            return (int) interruptCodeMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int ipv4AddressLen() {
        try{
            return (int) ipv4AddressLenMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int ipv6AddressLen() {
        try{
            return (int) ipv6AddressLenMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int ipv4AddressSize() {
        try{
            return (int) ipv4AddressSizeMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int ipv6AddressSize() {
        try{
            return (int) ipv6AddressSizeMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int ipv4AddressAlign() {
        try{
            return (int) ipv4AddressAlignMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int ipv6AddressAlign() {
        try{
            return (int) ipv6AddressAlignMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int kqueue() {
        try{
            return (int) kqueueMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int keventCtl(int kq, MemorySegment changeList, int nChanges) {
        try{
            return (int) keventCtlMethodHandle.invokeExact(kq, changeList, nChanges);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int keventWait(int kq, MemorySegment eventList, int nEvents, MemorySegment timeout) {
        try{
            return (int) keventWaitMethodHandle.invokeExact(kq, eventList, nEvents, timeout);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int getIpv4Address(MemorySegment clientAddr, MemorySegment addrStr, int len) {
        try{
            return (int) getIpv4AddressMethodHandle.invokeExact(clientAddr, addrStr, len);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int getIpv6Address(MemorySegment clientAddr, MemorySegment addrStr, int len) {
        try{
            return (int) getIpv6AddressMethodHandle.invokeExact(clientAddr, addrStr, len);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static short ipv4Port(MemorySegment clientAddr) {
        try{
            return (short) ipv4PortMethodHandle.invokeExact(clientAddr);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static short ipv6Port(MemorySegment clientAddr) {
        try{
            return (short) ipv6PortMethodHandle.invokeExact(clientAddr);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int ipv4SocketCreate() {
        try{
            return (int) ipv4SocketCreateMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int ipv6SocketCreate() {
        try{
            return (int) ipv6SocketCreateMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int setIpv4SockAddr(MemorySegment sockAddr, MemorySegment address, short port) {
        try{
            return (int) setIpv4SockAddrMethodHandle.invokeExact(sockAddr, address, port);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int setIpv6SockAddr(MemorySegment sockAddr, MemorySegment address, short port) {
        try{
            return (int) setIpv6SockAddrMethodHandle.invokeExact(sockAddr, address, port);
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

    public static int setIpv6Only(int socket, int value) {
        try{
            return (int) setIpv6OnlyMethodHandle.invokeExact(socket, value);
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

    public static int listen(int socket, int backlog) {
        try{
            return (int) listenMethodHandle.invokeExact(socket, backlog);
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

    public static int accept(int socket, MemorySegment clientAddr, int clientAddrSize) {
        try{
            return (int) acceptMethodHandle.invokeExact(socket, clientAddr, clientAddrSize);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int recv(int socket, MemorySegment buf, int len) {
        try{
            return (int) recvMethodHandle.invokeExact(socket, buf, len);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int send(int socket, MemorySegment buf, int len) {
        try{
            return (int) sendMethodHandle.invokeExact(socket, buf, len);
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

    public static int close(int fd) {
        try{
            return (int) closeMethodHandle.invokeExact(fd);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

}
