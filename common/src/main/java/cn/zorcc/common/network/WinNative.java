package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.pojo.Loc;
import cn.zorcc.common.util.NativeUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

/**
 *  Native implementation under Windows, using wepoll
 */
@Slf4j
public final class WinNative implements Native {
    /**
     *  corresponding to union epoll_data in wepoll.h
     */
    private static final MemoryLayout epollDataLayout = MemoryLayout.unionLayout(
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
    private static final MemoryLayout epollEventLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("events"),
            MemoryLayout.paddingLayout(4 * Constants.BYTE_SIZE),
            epollDataLayout.withName("data")
    );
    private static final long eventSize = epollEventLayout.byteSize();
    private static final long eventsOffset = epollEventLayout.byteOffset(MemoryLayout.PathElement.groupElement("events"));
    private static final long dataOffset = epollEventLayout.byteOffset(MemoryLayout.PathElement.groupElement("data"));
    private static final long sockOffset = epollDataLayout.byteOffset(MemoryLayout.PathElement.groupElement("sock"));

    /**
     *  corresponding to struct sockaddr_in
     */
    private static final MemoryLayout sockAddrLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_SHORT.withName("sin_family"),
            ValueLayout.JAVA_SHORT.withName("sin_port"),
            ValueLayout.JAVA_INT.withName("sin_addr"),
            MemoryLayout.paddingLayout(8 * Constants.BYTE_SIZE)
    );
    private static final int sockAddrSize = (int) sockAddrLayout.byteSize();
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
    private static final int addressLen;
    private static final int connectBlockCode;
    private static final int sendBlockCode;
    private static final long invalidSocket;

    static {
        SymbolLookup symbolLookup = NativeUtil.loadLibraryFromResource(NativeUtil.netLib());
        epollCreateMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "w_epoll_create", FunctionDescriptor.of(ValueLayout.ADDRESS));
        epollCtlMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "w_epoll_ctl_add", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
        epollWaitMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "w_epoll_wait", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        epollCloseMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "w_epoll_close", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        addressMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "w_address", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        portMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "w_port", FunctionDescriptor.of(ValueLayout.JAVA_SHORT, ValueLayout.ADDRESS));
        socketCreateMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "w_socket_create", FunctionDescriptor.of(ValueLayout.JAVA_LONG));
        acceptMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "w_accept", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        setSockAddrMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "w_set_sock_addr", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_SHORT));
        setReuseAddrMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "w_set_reuse_addr", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));
        setKeepAliveMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "w_set_keep_alive", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));
        setTcpNoDelayMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "w_set_tcp_no_delay", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));
        getErrOptMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "w_get_err_opt", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
        setNonBlockingMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "w_set_nonblocking", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));
        bindMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "w_bind", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        connectMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "w_connect", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        listenMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "w_listen", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));
        recvMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "w_recv", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        sendMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "w_send", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        closeSocketMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "w_close_socket", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));
        shutdownWriteMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "w_shutdown_write", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));
        wsaGetLastErrorMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "w_get_last_error", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        wsaCleanUpMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "w_clean_up", FunctionDescriptor.of(ValueLayout.JAVA_INT));

        // initialize constants
        try{
            addressLen = (int) NativeUtil.methodHandle(symbolLookup,
                    "w_address_len", FunctionDescriptor.of(ValueLayout.JAVA_INT)).invokeExact();
            connectBlockCode = (int) NativeUtil.methodHandle(symbolLookup,
                    "w_connect_block_code", FunctionDescriptor.of(ValueLayout.JAVA_INT)).invokeExact();
            sendBlockCode = (int) NativeUtil.methodHandle(symbolLookup,
                    "w_send_block_code", FunctionDescriptor.of(ValueLayout.JAVA_INT)).invokeExact();
            invalidSocket = (long) NativeUtil.methodHandle(symbolLookup,
                    "w_invalid_socket", FunctionDescriptor.of(ValueLayout.JAVA_LONG)).invokeExact();
        }catch (Throwable throwable) {
            // should never happen
            throw new FrameworkException(ExceptionType.NATIVE, "Failed to initialize constants", throwable);
        }
    }

    /**
     *   corresponding to `int w_connect_block_code()`
     */
    @Override
    public int connectBlockCode() {
        return connectBlockCode;
    }

    /**
     *   corresponding to `int w_send_block_code()`
     */
    @Override
    public int sendBlockCode() {
        return sendBlockCode;
    }

    @Override
    public MemorySegment createSockAddr(Loc loc, Arena arena) {
        MemorySegment r = arena.allocate(sockAddrLayout);
        MemorySegment ip = NativeUtil.allocateStr(arena, loc.ip(), addressLen);
        short port = shortPort(loc.port());
        if(check(setSockAddr(r, ip, port), "setSockAddr") == 0) {
            throw new FrameworkException(ExceptionType.NETWORK, "Loc is not valid");
        }
        return r;
    }

    @Override
    public Mux createMux() {
        MemorySegment winHandle = epollCreate();
        if(NativeUtil.checkNullPointer(winHandle)) {
            throw new FrameworkException(ExceptionType.NETWORK, "Failed to create wepoll with NULL pointer exception");
        }
        return Mux.win(winHandle);
    }

    @Override
    public MemorySegment createEventsArray(NetworkConfig config) {
        MemoryLayout eventsArrayLayout = MemoryLayout.sequenceLayout(config.getMaxEvents(), epollEventLayout);
        return MemorySegment.allocateNative(eventsArrayLayout, SegmentScope.global());
    }

    @Override
    public Socket createSocket(NetworkConfig config) {
        long socketFd = check(socketCreate(), "create socket");
        check(setReuseAddr(socketFd, config.getReuseAddr() ? 1 : 0), "set SO_REUSE_ADDR");
        check(setKeepAlive(socketFd, config.getKeepAlive() ? 1 : 0), "set SO_KEEPALIVE");
        check(setTcpNoDelay(socketFd, config.getTcpNoDelay() ? 1 : 0), "set TCP_NODELAY");
        check(setNonBlocking(socketFd), "set NON_BLOCKING");
        return new Socket(socketFd);
    }

    @Override
    public void bindAndListen(NetworkConfig config, Socket socket) {
        try(Arena arena = Arena.openConfined()) {
            MemorySegment addr = arena.allocate(sockAddrLayout);
            MemorySegment ip = NativeUtil.allocateStr(arena, config.getIp(), addressLen);
            int setSockAddr = check(setSockAddr(addr, ip, config.getPort()), "set SockAddr");
            if(setSockAddr == 0) {
                throw new FrameworkException(ExceptionType.NETWORK, "Network address is not valid");
            }
            check(bind(socket.longValue(), addr, sockAddrSize), "bind");
            check(listen(socket.longValue(), config.getBacklog()), "listen");
        }
    }

    @Override
    public void register(Mux mux, Socket socket, int from, int to) {
        int target = switch (to) {
            case Native.REGISTER_READ -> Constants.EPOLL_IN | Constants.EPOLL_RDHUP;
            case Native.REGISTER_WRITE -> Constants.EPOLL_OUT;
            case Native.REGISTER_READ_WRITE -> Constants.EPOLL_IN | Constants.EPOLL_RDHUP | Constants.EPOLL_OUT;
            default -> throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        };
        try(Arena arena = Arena.openConfined()) {
            MemorySegment winHandle = mux.winHandle();
            long fd = socket.longValue();
            MemorySegment ev = arena.allocate(epollEventLayout);
            NativeUtil.setInt(ev, eventsOffset, target);
            NativeUtil.setLong(ev, dataOffset + sockOffset, fd);
            check(epollCtl(winHandle, from == Native.REGISTER_NONE ? Constants.EPOLL_CTL_ADD : Constants.EPOLL_CTL_MOD, fd, ev), "epollCtl");
        }
    }

    @Override
    public void unregister(Mux mux, Socket socket, int current) {
        if(current > 0) {
            MemorySegment winHandle = mux.winHandle();
            long fd = socket.longValue();
            check(epollCtl(winHandle, Constants.EPOLL_CTL_DEL, fd, NativeUtil.NULL_POINTER), "epollCtl");
        }
    }

    @Override
    public int multiplexingWait(NetworkState state, int maxEvents) {
        return epollWait(state.mux().winHandle(), state.events(), maxEvents, -1);
    }

    @Override
    public void waitForAccept(NetworkState state, int index, Net net) {
        MemorySegment events = state.events();
        Socket serverSocket = net.master().socket();
        int event = NativeUtil.getInt(events, index * eventSize + eventsOffset);
        long socket = NativeUtil.getLong(events, index * eventSize + dataOffset + sockOffset);
        if(socket == serverSocket.longValue() && (event & Constants.EPOLL_IN) != 0) {
            // current server socket receive connection
            ClientSocket clientSocket = accept(net.config(), serverSocket);
            net.distribute(clientSocket);
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public void waitForData(NetworkState state, int index, ReadBuffer readBuffer) {
        MemorySegment events = state.events();
        int event = NativeUtil.getInt(events, index * eventSize + eventsOffset);
        Socket socket = new Socket(NativeUtil.getLong(events, index * eventSize + dataOffset + sockOffset));
        if((event & Constants.EPOLL_REMOTE) != 0) {
            state.shouldRead(socket, readBuffer);
        }else if((event & Constants.EPOLL_OUT) != 0) {
            state.shouldWrite(socket);
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public int connect(Socket socket, MemorySegment sockAddr) {
        return connect(socket.longValue(), sockAddr, addressLen);
    }

    @Override
    public ClientSocket accept(NetworkConfig config, Socket socket) {
        try(Arena arena = Arena.openConfined()) {
            MemorySegment clientAddr = arena.allocate(sockAddrLayout);
            MemorySegment address = arena.allocateArray(ValueLayout.JAVA_BYTE, addressLen);
            long socketFd = accept(socket.longValue(), clientAddr, sockAddrSize);
            if(socketFd == invalidSocket) {
                throw new FrameworkException(ExceptionType.NETWORK, "Failed to accept client socket, errno : {}", errno());
            }
            Socket clientSocket = new Socket(socketFd);
            check(setReuseAddr(socketFd, config.getReuseAddr() ? 1 : 0), "set SO_REUSE_ADDR");
            check(setKeepAlive(socketFd, config.getKeepAlive() ? 1 : 0), "set SO_KEEPALIVE");
            check(setTcpNoDelay(socketFd, config.getTcpNoDelay() ? 1 : 0), "set TCP_NODELAY");
            check(setNonBlocking(socketFd), "set NON_BLOCKING");
            if(address(clientAddr, address, addressLen) == -1) {
                throw new FrameworkException(ExceptionType.NETWORK, "Failed to get client socket's remote address, errno : {}", errno());
            }
            Loc loc = new Loc(NativeUtil.getStr(address), intPort(port(clientAddr)));
            return new ClientSocket(clientSocket, loc);
        }
    }

    @Override
    public int recv(Socket socket, MemorySegment data, int len) {
        return recv(socket.longValue(), data, len);
    }

    @Override
    public int send(Socket socket, MemorySegment data, int len) {
        return send(socket.longValue(), data, len);
    }

    @Override
    public int getErrOpt(Socket socket) {
        try(Arena arena = Arena.openConfined()) {
            MemorySegment ptr = arena.allocate(ValueLayout.JAVA_INT, -1);
            if (getErrOpt(socket.longValue(), ptr) == -1) {
                throw new FrameworkException(ExceptionType.NETWORK, "Failed to get Socket's err opt");
            }
            return NativeUtil.getInt(ptr, 0);
        }
    }

    @Override
    public void closeSocket(Socket socket) {
        check(closeSocket(socket.longValue()), "close socket");
    }

    @Override
    public void shutdownWrite(Socket socket) {
        check(shutdownWrite(socket.longValue()), "shutdown write");
    }

    @Override
    public void exitMux(Mux mux) {
        check(epollClose(mux.winHandle()), "close wepoll fd");
    }

    @Override
    public void exit() {
        check(cleanUp(), "wsa_clean_up");
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
     *  corresponding to `int w_epoll_ctl(void* handle, int op, SOCKET socket, struct epoll_event* event)`
     */
    public int epollCtl(MemorySegment handle, int op, long socket, MemorySegment event) {
        try{
            return (int) epollCtlMethodHandle.invokeExact(handle, op, socket, event);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking epollCtl()", throwable);
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
     *  corresponding to `u_short w_port(struct sockaddr_in* clientAddr)`
     */
    public short port(MemorySegment clientAddr) {
        try{
            return (short) portMethodHandle.invokeExact(clientAddr);
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
     *  corresponding to `SOCKET w_accept(SOCKET socket, struct sockaddr_in* clientAddr, int clientAddrSize)`
     */
    public long accept(long socket, MemorySegment clientAddr, int clientAddrSize) {
        try{
            return (long) acceptMethodHandle.invokeExact(socket, clientAddr, clientAddrSize);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking accept()", throwable);
        }
    }

    /**
     *  corresponding to `int w_set_sock_addr(struct sockaddr_in* sockAddr, char* address, u_short port)`
     */
    public int setSockAddr(MemorySegment sockAddr, MemorySegment address, short port) {
        try{
            return (int) setSockAddrMethodHandle.invokeExact(sockAddr, address, port);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setSockAddr()", throwable);
        }
    }

    /**
     *  corresponding to `int w_set_reuse_addr(SOCKET socket, int value)`
     */
    public int setReuseAddr(long socket, int value) {
        try{
            return (int) setReuseAddrMethodHandle.invokeExact(socket, value);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setReuseAddr()", throwable);
        }
    }

    /**
     *  corresponding to `int w_set_keep_alive(SOCKET socket, int value)`
     */
    public int setKeepAlive(long socket, int value) {
        try{
            return (int) setKeepAliveMethodHandle.invokeExact(socket, value);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setKeepAlive()", throwable);
        }
    }

    /**
     *  corresponding to `int w_set_tcp_no_delay(SOCKET socket, int value)`
     */
    public int setTcpNoDelay(long socket, int value) {
        try{
            return (int) setTcpNoDelayMethodHandle.invokeExact(socket, value);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setTcpNoDelay()", throwable);
        }
    }

    /**
     *  corresponding to `int w_get_err_opt(SOCKET socket, int* ptr)`
     */
    public int getErrOpt(long socket, MemorySegment ptr) {
        try{
            return (int) getErrOptMethodHandle.invokeExact(socket, ptr);
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
     *  corresponding to `int w_bind(SOCKET socket, struct sockaddr_in* sockAddr, int size)`
     */
    public int bind(long socket, MemorySegment sockAddr, int size) {
        try{
            return (int) bindMethodHandle.invokeExact(socket, sockAddr, size);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking bind()", throwable);
        }
    }

    /**
     *  corresponding to `int w_connect(SOCKET socket, struct sockaddr_in* sockAddr, int size)`
     */
    public int connect(long socket, MemorySegment sockAddr, int size) {
        try{
            return (int) connectMethodHandle.invokeExact(sockAddr, socket, size);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking connect()", throwable);
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
     *  corresponding to `int w_send(SOCKET socket, char* buf, int len)`
     */
    public int send(long socket, MemorySegment buf, int len) {
        try{
            return (int) sendMethodHandle.invokeExact(socket, buf, len);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking send()", throwable);
        }
    }

    /**
     *  corresponding to `int w_close_socket(SOCKET socket)`
     */
    public int closeSocket(long socket) {
        try{
            return (int) closeSocketMethodHandle.invokeExact(socket);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking closeSocket()", throwable);
        }
    }

    /**
     *  corresponding to `int w_shutdown_write(SOCKET socket)`
     */
    public int shutdownWrite(long socket) {
        try{
            return (int) shutdownWriteMethodHandle.invokeExact(socket);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking shutdownWrite()", throwable);
        }
    }

    /**
     *  corresponding to `int w_get_last_error()`, make it errno() to be consistent with macOS and Linux
     */
    @Override
    public int errno() {
        try{
            return (int) wsaGetLastErrorMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking wsaGetLastError()", throwable);
        }
    }

    /**
     *  corresponding to `int w_clean_up()`
     */
    public int cleanUp() {
        try{
            return (int) wsaCleanUpMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking wsaCleanUp()", throwable);
        }
    }

}