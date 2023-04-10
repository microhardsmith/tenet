package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

@Slf4j
public class MacNative implements Native {
    /**
     *  corresponding to struct kevent in event.h
     */
    private static final MemoryLayout keventLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_LONG.withName("ident"),
            ValueLayout.JAVA_SHORT.withName("filter"),
            ValueLayout.JAVA_SHORT.withName("flags"),
            ValueLayout.JAVA_INT.withName("fflags"),
            ValueLayout.JAVA_LONG.withName("data"),
            ValueLayout.ADDRESS.withName("udata")
    );

    /**
     *  corresponding to struct sockaddr_in in in.h
     */
    private static final MemoryLayout sockAddrLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_BYTE.withName("sin_len"),
            ValueLayout.JAVA_BYTE.withName("sin_family"),
            ValueLayout.JAVA_SHORT.withName("sin_port"),
            ValueLayout.JAVA_INT.withName("sin_addr"),
            MemoryLayout.paddingLayout(8 * Constants.BYTE_SIZE)
    );
    private static final int sockAddrSize = (int) sockAddrLayout.byteSize();
    private static final MethodHandle kqueueMethodHandle;
    private static final MethodHandle keventErrMethodHandle;
    private static final MethodHandle keventEofMethodHandle;
    private static final MethodHandle ewouldblockMethodHandle;
    private static final MethodHandle einprogressMethodHandle;
    private static final MethodHandle keventAddMethodHandle;
    private static final MethodHandle keventWaitMethodHandle;
    private static final MethodHandle addressLenMethodHandle;
    private static final MethodHandle addressMethodHandle;
    private static final MethodHandle portMethodHandle;
    private static final MethodHandle socketCreateMethodHandle;
    private static final MethodHandle acceptMethodHandle;
    private static final MethodHandle setSockAddrMethodHandle;
    private static final MethodHandle setReuseAddrMethodHandle;
    private static final MethodHandle setKeepAliveMethodHandle;
    private static final MethodHandle setTcpNoDelayMethodHandle;
    private static final MethodHandle setNonBlockingMethodHandle;
    private static final MethodHandle bindMethodHandle;
    private static final MethodHandle listenMethodHandle;
    private static final MethodHandle recvMethodHandle;
    private static final MethodHandle closeMethodHandle;
    private static final MethodHandle errnoMethodHandle;
    private static final int addressLen;
    private static final int connectBlockCode;
    private static final int sendBlockCode;

    @Override
    public int connectBlockCode() {
        return connectBlockCode;
    }

    @Override
    public int sendBlockCode() {
        return sendBlockCode;
    }

    @Override
    public Mux createMux() {
        return null;
    }

    @Override
    public MemorySegment createEventsArray(NetworkConfig config) {
        return null;
    }

    @Override
    public Socket createSocket(NetworkConfig config, boolean isServer) {
        return null;
    }

    @Override
    public void bindAndListen(NetworkConfig config, Socket socket) {

    }

    @Override
    public void registerRead(Mux mux, Socket socket) {

    }

    @Override
    public void registerWrite(Mux mux, Socket socket) {

    }

    @Override
    public void unregister(Mux mux, Socket socket) {

    }

    @Override
    public void waitForAccept(Net net, NetworkState state) {

    }

    @Override
    public void waitForData(ReadBuffer[] buffers, NetworkState state) {

    }

    @Override
    public void connect(Net net, Remote remote, Codec codec) {

    }

    @Override
    public void closeSocket(Socket socket) {
        check(close(socket.intValue()), "close socket");
    }

    @Override
    public int send(Socket socket, MemorySegment data, int len) {
        return 0;
    }

    @Override
    public void exitMux(Mux mux) {
        check(close(mux.kqfd()), "close kqueue fd");
    }

    @Override
    public void exit() {
        // no action, kqueue doesn't need external operations for clean up
    }

    static {
        SymbolLookup symbolLookup = NativeUtil.loadLibraryFromResource(NativeUtil.commonLib());
        kqueueMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_kqueue", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        keventErrMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_kevent_err", FunctionDescriptor.of(ValueLayout.JAVA_SHORT));
        keventEofMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_kevent_eof", FunctionDescriptor.of(ValueLayout.JAVA_SHORT));
        ewouldblockMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_ewouldblock", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        einprogressMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_einprogress", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        keventAddMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_kevent_add", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_SHORT));
        keventWaitMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_kevent_wait", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        addressLenMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_address_len", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        addressMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_address", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        portMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_port", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        socketCreateMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_socket_create", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        acceptMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_accept", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        setSockAddrMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_set_sock_addr", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        setReuseAddrMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_set_reuse_addr", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        setKeepAliveMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_set_keep_alive", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        setTcpNoDelayMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_set_tcp_no_delay", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        setNonBlockingMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_set_nonblocking", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        bindMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_bind", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        listenMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_listen", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        recvMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_recv", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        closeMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_close", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        errnoMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_errno", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        // initialize constants
        try{
            addressLen = (int) NativeUtil.methodHandle(symbolLookup,
                    "m_address_len", FunctionDescriptor.of(ValueLayout.JAVA_INT)).invokeExact();
            connectBlockCode = (int) NativeUtil.methodHandle(symbolLookup,
                    "m_connect_block_code", FunctionDescriptor.of(ValueLayout.JAVA_INT)).invokeExact();
            sendBlockCode = (int) NativeUtil.methodHandle(symbolLookup,
                    "m_send_block_code", FunctionDescriptor.of(ValueLayout.JAVA_INT)).invokeExact();
        }catch (Throwable throwable) {
            // should never happen
            throw new FrameworkException(ExceptionType.NATIVE, "Failed to initialize constants", throwable);
        }
    }

    /**
     *  corresponding to `int m_kqueue()`
     */
    public int kqueue() {
        try{
            return (int) kqueueMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking kqueue()", throwable);
        }
    }

    /**
     *  corresponding to `u_int16_t m_kevent_err()`
     */
    public short keventErr() {
        try{
            return (short) keventErrMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking keventErr()", throwable);
        }
    }

    /**
     *  corresponding to `u_int16_t m_kevent_eof()`
     */
    public short keventEof() {
        try{
            return (short) keventEofMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking keventEof()", throwable);
        }
    }



    /**
     *  corresponding to `u_int16_t m_kevent_err()`
     */
    public int ewouldblock() {
        try{
            return (int) ewouldblockMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking ewouldblock()", throwable);
        }
    }

    /**
     *  corresponding to `u_int16_t m_kevent_err()`
     */
    public int einprogress() {
        try{
            return (int) einprogressMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking einprogress()", throwable);
        }
    }

    /**
     *  corresponding to `int m_kevent_add(int kq, struct kevent* changelist, int fd, uint16_t flag)`
     */
    public int keventAdd(int kq, MemorySegment changelist, int fd, short flag) {
        try{
            return (int) keventAddMethodHandle.invokeExact(kq, changelist, fd, flag);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking keventAdd()", throwable);
        }
    }

    /**
     *  corresponding to `int m_kevent_wait(int kq, struct kevent* eventlist, int nevents)`
     */
    public int keventWait(int kq, MemorySegment eventlist, int nevents) {
        try{
            return (int) keventWaitMethodHandle.invokeExact(kq, eventlist, nevents);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking keventWait()", throwable);
        }
    }

    /**
     *  corresponding to `socklen_t m_address_len()`
     */
    public int addressLen() {
        try{
            return (int) addressLenMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking addressLen()", throwable);
        }
    }

    /**
     *  corresponding to `int m_address(struct sockaddr_in* sockAddr, char* addrStr, socklen_t len)`
     */
    public int address(MemorySegment sockAddr, MemorySegment addrStr, int len) {
        try{
            return (int) addressMethodHandle.invokeExact(sockAddr, addrStr, len);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking address()", throwable);
        }
    }

    /**
     *  corresponding to `int m_port(struct sockaddr_in* sockAddr)`
     */
    public int port(MemorySegment sockAddr) {
        try{
            return (int) portMethodHandle.invokeExact(sockAddr);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking port()", throwable);
        }
    }

    /**
     *  corresponding to `int m_socket_create()`
     */
    public int socketCreate() {
        try{
            return (int) socketCreateMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking socketCreate()", throwable);
        }
    }

    /**
     *  corresponding to `int m_accept(int socket, struct sockaddr_in* clientAddr, socklen_t clientAddrSize)`
     */
    public int accept(int socket, MemorySegment clientAddr, int clientAddrSize) {
        try{
            return (int) acceptMethodHandle.invokeExact(socket, clientAddr, clientAddrSize);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking accept()", throwable);
        }
    }

    /**
     *  corresponding to `int m_set_sock_addr(struct sockaddr_in* sockAddr, char* address, int port)`
     */
    public int setSockAddr(MemorySegment sockAddr, MemorySegment address, int port) {
        try{
            return (int) setSockAddrMethodHandle.invokeExact(sockAddr, address, port);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setSockAddr()", throwable);
        }
    }

    /**
     *  corresponding to `int m_set_reuse_addr(int socket, int value)`
     */
    public int setReuseAddr(int socket, int value) {
        try{
            return (int) setReuseAddrMethodHandle.invokeExact(socket, value);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setReuseAddr()", throwable);
        }
    }

    /**
     *  corresponding to `int m_set_keep_alive(int socket, int value)`
     */
    public int setKeepAlive(int socket, int value) {
        try{
            return (int) setKeepAliveMethodHandle.invokeExact(socket, value);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setKeepAlive()", throwable);
        }
    }

    /**
     *  corresponding to `int m_set_tcp_no_delay(int socket, int value)`
     */
    public int setTcpNoDelay(int socket, int value) {
        try{
            return (int) setTcpNoDelayMethodHandle.invokeExact(socket, value);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setTcpNoDelay()", throwable);
        }
    }

    /**
     *  corresponding to `int m_set_nonblocking(int socket)`
     */
    public int setNonBlocking(int socket) {
        try{
            return (int) setNonBlockingMethodHandle.invokeExact(socket);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setNonBlocking()", throwable);
        }
    }

    /**
     *  corresponding to `int m_bind(struct sockaddr_in* sockAddr, int socket, socklen_t size)`
     */
    public int bind(MemorySegment sockAddr, int socket, int size) {
        try{
            return (int) bindMethodHandle.invokeExact(sockAddr, socket, size);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking bind()", throwable);
        }
    }

    /**
     *  corresponding to `int m_listen(int socket, int backlog)`
     */
    public int listen(int socket, int backlog) {
        try{
            return (int) listenMethodHandle.invokeExact(socket, backlog);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking listen()", throwable);
        }
    }

    /**
     *  corresponding to `ssize_t m_recv(int socket, void* buf, socklen_t len)`
     */
    public int recv(int socket, MemorySegment buf, int len) {
        try{
            return (int) recvMethodHandle.invokeExact(socket, buf, len);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking recv()", throwable);
        }
    }

    /**
     *  corresponding to `int m_close(int fd)`
     */
    public int close(int fd) {
        try{
            return (int) closeMethodHandle.invokeExact(fd);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking close()", throwable);
        }
    }

    /**
     *  corresponding to `int m_errno()`
     */
    public int errno() {
        try{
            return (int) errnoMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking errno()", throwable);
        }
    }
}
