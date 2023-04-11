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
import java.util.Map;

/**
 *   Native implementation under Linux, using epoll
 */
@Slf4j
public class LinuxNative implements Native {
    /**
     *  corresponding to union epoll_data in epoll.h
     */
    private static final MemoryLayout epollDataLayout = MemoryLayout.unionLayout(
            ValueLayout.ADDRESS.withName("ptr"),
            ValueLayout.JAVA_INT.withName("fd"),
            ValueLayout.JAVA_INT.withName("u32"),
            ValueLayout.JAVA_LONG.withName("u64")
    );
    /**
     *  corresponding to struct epoll_event in epoll.h
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
     *  corresponding to struct sockaddr_in in in.h
     */
    private static final MemoryLayout sockAddrLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_SHORT.withName("sin_family"),
            ValueLayout.JAVA_SHORT.withName("sin_port"),
            ValueLayout.JAVA_INT.withName("sin_addr"),
            MemoryLayout.paddingLayout(8 * Constants.BYTE_SIZE)
    );
    private static final int sockAddrSize = (int) sockAddrLayout.byteSize();
    private static final MethodHandle epollCreateMethodHandle;
    private static final MethodHandle epollCtlAddMethodHandle;
    private static final MethodHandle epollCtlDelMethodHandle;
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
        int epfd = check(epollCreate(), "epoll create");
        return Mux.linux(epfd);
    }

    @Override
    public MemorySegment createEventsArray(NetworkConfig config) {
        MemoryLayout eventsArrayLayout = MemoryLayout.sequenceLayout(config.getMaxEvents(), epollEventLayout);
        return MemorySegment.allocateNative(eventsArrayLayout, SegmentScope.global());
    }

    @Override
    public Socket createSocket(NetworkConfig config, boolean isServer) {
        int socketFd = check(socketCreate(), "create socket");
        Socket socket = new Socket(socketFd);
        // if it's a client socket, this is no need to set the SO_REUSE_ADDR opt
        if(isServer) {
            check(setReuseAddr(socketFd, config.getReuseAddr() ? 1 : 0), "set SO_REUSE_ADDR");
        }
        check(setKeepAlive(socketFd, config.getKeepAlive() ? 1 : 0), "set SO_KEEPALIVE");
        check(setTcpNoDelay(socketFd, config.getTcpNoDelay() ? 1 : 0), "set TCP_NODELAY");
        // socket must be non_blocking
        check(setNonBlocking(socketFd), "set NON_BLOCKING");
        return socket;
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
            check(bind(socket.intValue(), addr, sockAddrSize), "bind");
            check(listen(socket.intValue(), config.getBacklog()), "listen");
        }
    }

    @Override
    public void registerRead(Mux mux, Socket socket) {
        try(Arena arena = Arena.openConfined()) {
            int epfd = mux.epfd();
            int fd = socket.intValue();
            MemorySegment ev = arena.allocate(epollEventLayout);
            NativeUtil.setInt(ev, eventsOffset, Constants.EPOLL_IN | Constants.EPOLL_RDHUP);
            NativeUtil.setInt(ev, dataOffset + fdOffset, fd);
            check(epollCtlAdd(epfd, fd, ev), "epoll_ctl_add read");
        }
    }

    @Override
    public void registerWrite(Mux mux, Socket socket) {
        try(Arena arena = Arena.openConfined()) {
            int epfd = mux.epfd();
            int fd = socket.intValue();
            MemorySegment ev = arena.allocate(epollEventLayout);
            NativeUtil.setInt(ev, eventsOffset, Constants.EPOLL_OUT | Constants.EPOLL_ONESHOT);
            NativeUtil.setInt(ev, dataOffset + fdOffset, fd);
            check(epollCtlAdd(epfd, fd, ev), "epoll_ctl_add write");
        }
    }

    @Override
    public void unregister(Mux mux, Socket socket) {
        int epFd = mux.epfd();
        int fd = socket.intValue();
        check(epollCtlDel(epFd, fd), "epoll_ctl_del");
    }

    @Override
    public void waitForAccept(Net net, NetworkState state) {
        MemorySegment events = state.events();
        int serverSocket = state.socket().intValue();
        int count = epollWait(state.mux().epfd(), events, net.config().getMaxEvents(), -1);
        if(count == -1) {
            if(Thread.currentThread().isInterrupted()) {
                // already shutdown
                return ;
            }else {
                // epoll wait failed
                log.error("epoll_wait failed with errno : {}", errno());
            }
        }
        for(int i = 0; i < count; i++) {
            int event = NativeUtil.getInt(events, i * eventSize + eventsOffset);
            int socket = NativeUtil.getInt(events, i * eventSize + dataOffset + fdOffset);
            if((event & Constants.EPOLL_IN) != 0 && socket == serverSocket) {
                // accept connection
                try(Arena arena = Arena.openConfined()) {
                    MemorySegment clientAddr = arena.allocate(sockAddrLayout);
                    MemorySegment address = arena.allocateArray(ValueLayout.JAVA_BYTE, addressLen);
                    int clientFd = accept(serverSocket, clientAddr, sockAddrSize);
                    if(clientFd == -1) {
                        log.error("Failed to accept client socket, errno : {}", errno());
                    }
                    Socket clientSocket = new Socket(clientFd);
                    if (setNonBlocking(clientFd) == -1) {
                        log.error("Failed to set client socket as non_blocking, errno : {}", errno());
                        closeSocket(clientSocket);
                    }
                    if(address(clientAddr, address, addressLen) == -1) {
                        log.error("Failed to get client socket's remote address, errno : {}", errno());
                        closeSocket(clientSocket);
                    }
                    Loc loc = new Loc(NativeUtil.getStr(address), port(clientAddr));
                    Worker worker = net.nextWorker();
                    Channel channel = Channel.forServer(net, clientSocket, loc, worker);
                    channel.init();
                }
            }else if((event & Constants.EPOLL_OUT) != 0){
                // some client connections has been established, validate if there is a socket err
                int errOpt = getErrOpt(socket);
                if (errOpt == 0) {
                    Channel channel = state.intMap().get(socket);
                    channel.init();
                }else {
                    log.error("Establishing connection failed with socket err : {}", errOpt);
                    state.intMap().remove(socket);
                }
            }else {
                // should never happen
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
        }
    }

    @Override
    public void waitForData(ReadBuffer[] buffers, NetworkState state) {
        MemorySegment events = state.events();
        Map<Integer, Channel> channelMap = state.intMap();
        int count = epollWait(state.mux().epfd(), events, buffers.length, -1);
        if(count == -1) {
            if(Thread.currentThread().isInterrupted()) {
                // already shutdown
                return ;
            }else {
                // epoll wait failed
                log.error("epoll_wait failed with errno : {}", errno());
            }
        }
        for(int i = 0; i < count; i++) {
            int event = NativeUtil.getInt(events, i * eventSize + eventsOffset);
            int socket = NativeUtil.getInt(events, i * eventSize + dataOffset + fdOffset);
            Channel channel = channelMap.get(socket);
            if(channel != null) {
                if((event & Constants.EPOLL_IN) != 0 || (event & Constants.EPOLL_RDHUP) != 0 || (event & Constants.EPOLL_ERR) != 0 || (event & Constants.EPOLL_HUP) != 0) {
                    // read event
                    ReadBuffer readBuffer = buffers[i];
                    int readableBytes = recv(socket, readBuffer.segment(), readBuffer.len());
                    if(readableBytes > 0) {
                        // recv data from remote peer
                        readBuffer.setWriteIndex(readableBytes);
                        channel.onReadBuffer(readBuffer);
                    }else {
                        // remove current socket
                        channel.shutdown();
                    }
                }else if((event & Constants.EPOLL_OUT) != 0) {
                    // write event
                    channel.becomeWritable();
                }else {
                    // should never happen
                    throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
                }
            }
        }
    }

    @Override
    public void connect(Net net, Remote remote, Codec codec) {
        Loc loc = remote.loc();
        try(Arena arena = Arena.openConfined()) {
            Socket socket = createSocket(net.config(), false);
            MemorySegment addr = arena.allocate(sockAddrLayout);
            MemorySegment ip = NativeUtil.allocateStr(arena, loc.ip(), addressLen);
            int setSockAddr = check(setSockAddr(addr, ip, loc.port()), "set SockAddr");
            if(setSockAddr == 0) {
                throw new FrameworkException(ExceptionType.NETWORK, "Network address is not valid");
            }
            int connect = connect(socket.intValue(), addr, addressLen);
            Channel channel = Channel.forClient(net, socket, codec, remote, net.nextWorker());
            if(connect == -1) {
                // we need to check if the connection is currently establishing
                int errno = errno();
                if(errno == connectBlockCode) {
                    // add it to current master's interest list
                    NetworkState masterState = net.master().state();
                    masterState.intMap().put(socket.intValue(), channel);
                    registerWrite(masterState.mux(), socket);
                }else {
                    throw new FrameworkException(ExceptionType.NETWORK, "Unable to connect, err : %d".formatted(errno));
                }
            }
        }
    }

    @Override
    public void closeSocket(Socket socket) {
        check(close(socket.intValue()), "close socket");
    }

    @Override
    public int send(Socket socket, MemorySegment data, int len) {
        return send(socket.intValue(), data, len);
    }

    @Override
    public void exitMux(Mux mux) {
        check(close(mux.epfd()), "close epoll fd");
    }

    @Override
    public void exit() {
        // no action, epoll doesn't need external operations for clean up
    }

    static  {
        SymbolLookup symbolLookup = NativeUtil.loadLibraryFromResource(NativeUtil.commonLib());
        epollCreateMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_epoll_create", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        epollCtlAddMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_epoll_ctl_add", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        epollCtlDelMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_epoll_ctl_del", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        epollWaitMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_epoll_wait", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        addressMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_address", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        portMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_port", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        socketCreateMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_socket_create", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        acceptMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_accept", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        setSockAddrMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_set_sock_addr", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        setReuseAddrMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_set_reuse_addr", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        setKeepAliveMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_set_keep_alive", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        setTcpNoDelayMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_set_tcp_no_delay", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        getErrOptMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_get_err_opt", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        setNonBlockingMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_set_nonblocking", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        bindMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_bind", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        connectMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_connect", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        listenMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_listen", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        recvMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_recv", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        sendMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_send", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        closeMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "l_close", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
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
     *  corresponding to `int l_get_err_opt(int socket)`
     */
    public int getErrOpt(int socket) {
        try{
            return (int) getErrOptMethodHandle.invokeExact(socket);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking getErrOpt()", throwable);
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
     *  corresponding to `int l_bind(int socket, struct sockaddr_in* sockAddr, socklen_t size)`
     */
    public int bind(int socket, MemorySegment sockAddr, int size) {
        try{
            return (int) bindMethodHandle.invokeExact(socket, sockAddr, size);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking bind()", throwable);
        }
    }

    /**
     *  corresponding to `int l_connect(int socket, struct sockaddr_in* sockAddr, socklen_t size)`
     */
    public int connect(int socket, MemorySegment sockAddr, int size) {
        try{
            return (int) connectMethodHandle.invokeExact(sockAddr, socket, size);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking connect()", throwable);
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
     *  corresponding to `ssize_t l_send(int socket, void* buf, size_t len)`
     */
    public int send(int socket, MemorySegment buf, int len) {
        try{
            return (int) sendMethodHandle.invokeExact(socket, buf, len);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking recv()", throwable);
        }
    }

    /**
     *  corresponding to `int l_close(int fd)`
     */
    public int close(int fd) {
        try{
            return (int) closeMethodHandle.invokeExact(fd);
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
