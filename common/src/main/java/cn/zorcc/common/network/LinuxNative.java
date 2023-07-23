package cn.zorcc.common.network;

import cn.zorcc.common.Clock;
import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.pojo.Loc;
import cn.zorcc.common.util.NativeUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 *   Native implementation under Linux, using epoll
 */
@Slf4j
public final class LinuxNative implements Native {
    /**
     *  Corresponding to union epoll_data in epoll.h
     */
    private static final MemoryLayout epollDataLayout = MemoryLayout.unionLayout(
            ValueLayout.ADDRESS.withName("ptr"),
            ValueLayout.JAVA_INT.withName("fd"),
            ValueLayout.JAVA_INT.withName("u32"),
            ValueLayout.JAVA_LONG.withName("u64")
    );
    /**
     *  Corresponding to struct epoll_event in epoll.h
     *  Note that epoll_event struct is defined as __attribute__ ((__packed__))
     *  so there is no need for a padding layout
     */
    private static final MemoryLayout epollEventLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("events"),
            epollDataLayout.withName("data")
    );
    private static final long eventSize = epollEventLayout.byteSize();
    private static final long eventsOffset = epollEventLayout.byteOffset(MemoryLayout.PathElement.groupElement("events"));
    private static final long dataOffset = epollEventLayout.byteOffset(MemoryLayout.PathElement.groupElement("data"));
    private static final long fdOffset = epollDataLayout.byteOffset(MemoryLayout.PathElement.groupElement("fd"));
    /**
     *  Corresponding to struct sockaddr_in in in.h
     */
    private static final MemoryLayout sockAddrLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_SHORT.withName("sin_family"),
            ValueLayout.JAVA_SHORT.withName("sin_port"),
            ValueLayout.JAVA_INT.withName("sin_addr"),
            MemoryLayout.paddingLayout(8 * ValueLayout.JAVA_BYTE.byteSize())
    );
    private static final int sockAddrSize = (int) sockAddrLayout.byteSize();
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
    private static final int addressLen;
    private static final int connectBlockCode;
    private static final int sendBlockCode;

    /**
     *   Corresponding to `int l_connect_block_code()`
     */
    @Override
    public int connectBlockCode() {
        return connectBlockCode;
    }

    /**
     *   Corresponding to `int l_send_block_code()`
     */
    @Override
    public int sendBlockCode() {
        return sendBlockCode;
    }

    @Override
    public MemorySegment createSockAddr(Loc loc, Arena arena) {
        MemorySegment r = arena.allocate(sockAddrLayout);
        MemorySegment ip = NativeUtil.allocateStr(arena, loc.ip(), addressLen);
        if(check(setSockAddr(r, ip, loc.shortPort()), "setSockAddr") == 0) {
            throw new FrameworkException(ExceptionType.NETWORK, "Loc is not valid");
        }
        return r;
    }

    @Override
    public Mux createMux() {
        int epfd = check(epollCreate(), "epoll create");
        return Mux.linux(epfd);
    }

    @Override
    public MemorySegment createEventsArray(MuxConfig config, Arena arena) {
        MemoryLayout eventsArrayLayout = MemoryLayout.sequenceLayout(config.getMaxEvents(), epollEventLayout);
        return arena.allocate(eventsArrayLayout);
    }

    @Override
    public Socket createSocket() {
        int socketFd = check(socketCreate(), "create socket");
        return new Socket(socketFd);
    }

    @Override
    public void configureSocket(NetworkConfig config, Socket socket) {
        int socketFd = socket.intValue();
        check(setReuseAddr(socketFd, config.getReuseAddr() > 0 ? 1 : 0), "set SO_REUSE_ADDR");
        check(setKeepAlive(socketFd, config.getKeepAlive() > 0 ? 1 : 0), "set SO_KEEPALIVE");
        check(setTcpNoDelay(socketFd, config.getTcpNoDelay() > 0 ? 1 : 0), "set TCP_NODELAY");
        check(setNonBlocking(socketFd), "set NON_BLOCKING");
    }

    @Override
    public void bindAndListen(Loc loc, MuxConfig config, Socket socket) {
        try(Arena arena = Arena.openConfined()) {
            MemorySegment addr = arena.allocate(sockAddrLayout);
            MemorySegment ip = NativeUtil.allocateStr(arena, loc.ip(), addressLen);
            int setSockAddr = check(setSockAddr(addr, ip, loc.shortPort()), "set SockAddr");
            if(setSockAddr == 0) {
                throw new FrameworkException(ExceptionType.NETWORK, "Network address is not valid");
            }
            check(bind(socket.intValue(), addr, sockAddrSize), "bind");
            check(listen(socket.intValue(), config.getBacklog()), "listen");
        }
    }

    @Override
    public void ctl(Mux mux, Socket socket, int from, int to) {
        if(from == to) {
            return ;
        }
        int epfd = mux.epfd();
        int fd = socket.intValue();
        if(to == Native.REGISTER_NONE) {
            check(epollCtl(epfd, Constants.EPOLL_CTL_DEL, fd, NativeUtil.NULL_POINTER), "epoll_ctl");
        }else {
            int target = ((to & Native.REGISTER_READ) != 0 ? Constants.EPOLL_IN | Constants.EPOLL_RDHUP : 0) |
                    ((to & Native.REGISTER_WRITE) != 0 ? Constants.EPOLL_OUT : 0);
            try(Arena arena = Arena.openConfined()) {
                MemorySegment ev = arena.allocate(epollEventLayout);
                NativeUtil.setInt(ev, eventsOffset, target);
                NativeUtil.setInt(ev, dataOffset + fdOffset, fd);
                check(epollCtl(epfd, from == Native.REGISTER_NONE ? Constants.EPOLL_CTL_ADD : Constants.EPOLL_CTL_MOD, fd, ev), "epoll_ctl");
            }
        }
    }

    @Override
    public int multiplexingWait(Mux mux, MemorySegment events, int maxEvents, Timeout timeout) {
        return epollWait(mux.epfd(), events, maxEvents, timeout.val());
    }

    @Override
    public ClientSocket waitForAccept(NetworkConfig config, Socket serverSocket, MemorySegment events, int index) {
        int event = NativeUtil.getInt(events, index * eventSize + eventsOffset);
        int socket = NativeUtil.getInt(events, index * eventSize + dataOffset + fdOffset);
        if(socket == serverSocket.intValue() && (event & Constants.EPOLL_IN) != 0) {
            return accept(config, serverSocket);
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public void waitForData(Map<Socket, Object> socketMap, MemorySegment buffer, MemorySegment events, int index) {
        int event = NativeUtil.getInt(events, index * eventSize + eventsOffset);
        Socket socket = new Socket(NativeUtil.getInt(events, index * eventSize + dataOffset + fdOffset));
        if((event & (Constants.EPOLL_IN | Constants.EPOLL_ERR | Constants.EPOLL_HUP | Constants.EPOLL_RDHUP)) != 0) {
            Native.shouldRead(socketMap, socket, buffer);
        }else if((event & Constants.EPOLL_OUT) != 0) {
            Native.shouldWrite(socketMap, socket);
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public int connect(Socket socket, MemorySegment sockAddr) {
        return connect(socket.intValue(), sockAddr, addressLen);
    }

    @Override
    public ClientSocket accept(NetworkConfig config, Socket socket) {
        try(Arena arena = Arena.openConfined()) {
            MemorySegment clientAddr = arena.allocate(sockAddrLayout);
            MemorySegment address = arena.allocateArray(ValueLayout.JAVA_BYTE, addressLen);
            int socketFd = accept(socket.intValue(), clientAddr, sockAddrSize);
            if(socketFd == -1) {
                throw new FrameworkException(ExceptionType.NETWORK, "Failed to accept client socket, errno : %d".formatted(errno()));
            }
            Socket clientSocket = new Socket(socketFd);
            configureSocket(config, clientSocket);
            if(address(clientAddr, address, addressLen) == -1) {
                throw new FrameworkException(ExceptionType.NETWORK, "Failed to get client socket's remote address, errno : %d".formatted(errno()));
            }
            String ip = NativeUtil.getStr(address, addressLen);
            int port = Loc.toIntPort(port(clientAddr));
            Loc loc = new Loc(ip, port);
            return new ClientSocket(clientSocket, loc);
        }
    }

    @Override
    public long recv(Socket socket, MemorySegment data, long len) {
        return recv(socket.intValue(), data, len);
    }

    @Override
    public long send(Socket socket, MemorySegment data, long len) {
        return send(socket.intValue(), data, len);
    }

    @Override
    public int getErrOpt(Socket socket) {
        try(Arena arena = Arena.openConfined()) {
            MemorySegment ptr = arena.allocate(ValueLayout.JAVA_INT, -1);
            if (getErrOpt(socket.intValue(), ptr) == -1) {
                throw new FrameworkException(ExceptionType.NETWORK, "Failed to get Socket's err opt");
            }
            return NativeUtil.getInt(ptr, 0);
        }
    }

    @Override
    public void closeSocket(Socket socket) {
        check(close(socket.intValue()), "close socket");
    }

    @Override
    public void shutdownWrite(Socket socket) {
        check(shutdownWrite(socket.intValue()), "shutdown write");
    }

    @Override
    public void exitMux(Mux mux) {
        check(close(mux.epfd()), "close epoll fd");
    }

    @Override
    public void exit() {
        // No action, epoll doesn't need external operations for clean up
    }

    static  {
        long nano = Clock.nano();
        SymbolLookup symbolLookup = NativeUtil.loadLibrary(Native.LIB);
        epollCreateMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_epoll_create", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        epollCtlMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_epoll_ctl", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        epollWaitMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_epoll_wait", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        addressMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_address", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        portMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_port", FunctionDescriptor.of(ValueLayout.JAVA_SHORT, ValueLayout.ADDRESS));
        socketCreateMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_socket_create", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        acceptMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_accept", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        setSockAddrMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_set_sock_addr", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_SHORT));
        setReuseAddrMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_set_reuse_addr", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        setKeepAliveMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_set_keep_alive", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        setTcpNoDelayMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_set_tcp_no_delay", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        getErrOptMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_get_err_opt", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        setNonBlockingMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_set_nonblocking", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        bindMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_bind", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        connectMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_connect", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        listenMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_listen", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        recvMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_recv", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        sendMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_send", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        closeMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_close", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        shutdownWriteMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_shutdown_write", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        errnoMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_errno", FunctionDescriptor.of(ValueLayout.JAVA_INT));

        // initialize constants
        try{
            addressLen = (int) NativeUtil.methodHandle(symbolLookup,
                    "l_address_len", FunctionDescriptor.of(ValueLayout.JAVA_INT)).invokeExact();
            connectBlockCode = (int) NativeUtil.methodHandle(symbolLookup,
                    "l_connect_block_code", FunctionDescriptor.of(ValueLayout.JAVA_INT)).invokeExact();
            sendBlockCode = (int) NativeUtil.methodHandle(symbolLookup,
                    "l_send_block_code", FunctionDescriptor.of(ValueLayout.JAVA_INT)).invokeExact();
        }catch (Throwable throwable) {
            // should never happen
            throw new FrameworkException(ExceptionType.NATIVE, "Failed to initialize constants", throwable);
        }
        log.info("Initializing Native successfully, platform : {}, time consuming : {} ms", NativeUtil.osName(), TimeUnit.NANOSECONDS.toMillis(Clock.elapsed(nano)));
    }

    /**
     *  Corresponding to `int l_epoll_create()`
     */
    public int epollCreate() {
        try{
            return (int) epollCreateMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking epollCreate()", throwable);
        }
    }

    /**
     *  Corresponding to `int l_epoll_ctl(int epfd, int op, int socket, struct epoll_event* ev)`
     */
    public int epollCtl(int epfd, int op, int socket, MemorySegment ev) {
        try{
            return (int) epollCtlMethodHandle.invokeExact(epfd, op, socket, ev);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking epollCtlAdd()", throwable);
        }
    }

    /**
     *  Corresponding to `int l_epoll_wait(int epfd, struct epoll_event* events, int maxEvents, int timeout)`
     */
    public int epollWait(int epfd, MemorySegment events, int maxEvents, int timeout) {
        try{
            return (int) epollWaitMethodHandle.invokeExact(epfd, events, maxEvents, timeout);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking epollWait()", throwable);
        }
    }

    /**
     *  Corresponding to `int l_address(struct sockaddr_in* sockAddr, char* addrStr, socklen_t len)`
     */
    public int address(MemorySegment sockAddr, MemorySegment addrStr, int len) {
        try{
            return (int) addressMethodHandle.invokeExact(sockAddr, addrStr, len);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking address()", throwable);
        }
    }

    /**
     *  Corresponding to `uint16_t l_port(struct sockaddr_in* sockAddr)`
     */
    public short port(MemorySegment sockAddr) {
        try{
            return (short) portMethodHandle.invokeExact(sockAddr);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking port()", throwable);
        }
    }

    /**
     *  Corresponding to `int l_socket_create()`
     */
    public int socketCreate() {
        try{
            return (int) socketCreateMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking socketCreate()", throwable);
        }
    }

    /**
     *  Corresponding to `int l_accept(int socket, struct sockaddr_in* clientAddr, socklen_t clientAddrSize)`
     */
    public int accept(int socket, MemorySegment clientAddr, int clientAddrSize) {
        try{
            return (int) acceptMethodHandle.invokeExact(socket, clientAddr, clientAddrSize);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking accept()", throwable);
        }
    }

    /**
     *  Corresponding to `int l_set_sock_addr(struct sockaddr_in* sockAddr, char* address, uint16_t port)`
     */
    public int setSockAddr(MemorySegment sockAddr, MemorySegment address, short port) {
        try{
            return (int) setSockAddrMethodHandle.invokeExact(sockAddr, address, port);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setSockAddr()", throwable);
        }
    }

    /**
     *  Corresponding to `int l_set_reuse_addr(int socket, int value)`
     */
    public int setReuseAddr(int socket, int value) {
        try{
            return (int) setReuseAddrMethodHandle.invokeExact(socket, value);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setReuseAddr()", throwable);
        }
    }

    /**
     *  Corresponding to `int l_set_keep_alive(int socket, int value)`
     */
    public int setKeepAlive(int socket, int value) {
        try{
            return (int) setKeepAliveMethodHandle.invokeExact(socket, value);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setKeepAlive()", throwable);
        }
    }

    /**
     *  Corresponding to `int l_set_tcp_no_delay(int socket, int value)`
     */
    public int setTcpNoDelay(int socket, int value) {
        try{
            return (int) setTcpNoDelayMethodHandle.invokeExact(socket, value);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setTcpNoDelay()", throwable);
        }
    }

    /**
     *  Corresponding to `int l_get_err_opt(int socket, int* ptr)`
     */
    public int getErrOpt(int socket, MemorySegment ptr) {
        try{
            return (int) getErrOptMethodHandle.invokeExact(socket, ptr);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking getErrOpt()", throwable);
        }
    }

    /**
     *  Corresponding to `int l_set_nonblocking(int socket)`
     */
    public int setNonBlocking(int socket) {
        try{
            return (int) setNonBlockingMethodHandle.invokeExact(socket);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setNonBlocking()", throwable);
        }
    }

    /**
     *  Corresponding to `int l_bind(int socket, struct sockaddr_in* sockAddr, socklen_t size)`
     */
    public int bind(int socket, MemorySegment sockAddr, int size) {
        try{
            return (int) bindMethodHandle.invokeExact(socket, sockAddr, size);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking bind()", throwable);
        }
    }

    /**
     *  Corresponding to `int l_connect(int socket, struct sockaddr_in* sockAddr, socklen_t size)`
     */
    public int connect(int socket, MemorySegment sockAddr, int size) {
        try{
            return (int) connectMethodHandle.invokeExact(sockAddr, socket, size);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking connect()", throwable);
        }
    }

    /**
     *  Corresponding to `int l_listen(int socket, int backlog)`
     */
    public int listen(int socket, int backlog) {
        try{
            return (int) listenMethodHandle.invokeExact(socket, backlog);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking listen()", throwable);
        }
    }

    /**
     *  Corresponding to `ssize_t l_recv(int socket, void* buf, size_t len)`
     */
    public long recv(int socket, MemorySegment buf, long len) {
        try{
            return (long) recvMethodHandle.invokeExact(socket, buf, len);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking recv()", throwable);
        }
    }

    /**
     *  Corresponding to `ssize_t l_send(int socket, void* buf, size_t len)`
     */
    public long send(int socket, MemorySegment buf, long len) {
        try{
            return (int) sendMethodHandle.invokeExact(socket, buf, len);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking recv()", throwable);
        }
    }

    /**
     *  Corresponding to `int l_close(int fd)`
     */
    public int close(int fd) {
        try{
            return (int) closeMethodHandle.invokeExact(fd);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking close()", throwable);
        }
    }

    /**
     *  Corresponding to `int l_shutdown_write(int fd)`
     */
    public int shutdownWrite(int fd) {
        try{
            return (int) shutdownWriteMethodHandle.invokeExact(fd);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking shutdownWrite()", throwable);
        }
    }

    /**
     *  Corresponding to `int l_errno()`
     */
    @Override
    public int errno() {
        try{
            return (int) errnoMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking errno()", throwable);
        }
    }

}
