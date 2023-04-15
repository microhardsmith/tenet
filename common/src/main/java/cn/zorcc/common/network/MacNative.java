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
import java.util.concurrent.locks.LockSupport;

/**
 *   Native implementation under MacOS, using kqueue
 *   Note that the .dylib library is only suitable for ARM-based chips since I only tested on M1 MacBook
 *   If developer needs to run it on X86 processors, recompile a new .dylib would be fine
 */
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
    private static final long keventSize = keventLayout.byteSize();
    private static final long identOffset = keventLayout.byteOffset(MemoryLayout.PathElement.groupElement("ident"));
    private static final long filterOffset = keventLayout.byteOffset(MemoryLayout.PathElement.groupElement("filter"));
    private static final long flagsOffset = keventLayout.byteOffset(MemoryLayout.PathElement.groupElement("flags"));


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
    private static final MethodHandle keventCtlMethodHandle;
    private static final MethodHandle keventWaitMethodHandle;
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
        int kqfd = check(kqueue(), "kqueue create");
        return Mux.mac(kqfd);
    }

    @Override
    public MemorySegment createEventsArray(NetworkConfig config) {
        MemoryLayout eventsArrayLayout = MemoryLayout.sequenceLayout(config.getMaxEvents(), keventLayout);
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
        int kqfd = mux.kqfd();
        int fd = socket.intValue();
        check(keventCtl(kqfd, fd, Constants.EVFILT_READ, Constants.EV_ADD), "kevent register read");
    }

    @Override
    public void registerWrite(Mux mux, Socket socket) {
        int kqfd = mux.kqfd();
        int fd = socket.intValue();
        check(keventCtl(kqfd, fd, Constants.EVFILT_WRITE, (short) (Constants.EV_ADD | Constants.EV_ONESHOT)), "kevent register write");
    }

    @Override
    public void unregister(Mux mux, Socket socket) {
        // when socket is closed, the events will be automatically deleted from the mux
    }

    @Override
    public void waitForAccept(Net net, NetworkState state) {
        MemorySegment events = state.events();
        int serverSocket = state.socket().intValue();
        int count = keventWait(state.mux().kqfd(), events, net.config().getMaxEvents());
        if(count == -1) {
            if(Thread.currentThread().isInterrupted()) {
                // already shutdown
                return ;
            }else {
                // kqueue wait failed
                log.error("kevent failed with errno : {}", errno());
            }
        }
        for(int i = 0; i < count; i++) {
            int ident = (int) NativeUtil.getLong(events, i * keventSize + identOffset);
            short filter = NativeUtil.getShort(events, i * keventSize + filterOffset);
            if((filter & Constants.EVFILT_READ) != 0 && ident == serverSocket) {
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
            }else if((filter & Constants.EVFILT_WRITE) != 0) {
                // some client connections has been established, validate if there is a socket err
                int errOpt = getErrOpt(ident);
                if (errOpt == 0) {
                    Channel channel = state.intMap().get(ident);
                    channel.init();
                }else {
                    log.error("Establishing connection failed with socket err : {}", errOpt);
                    state.intMap().remove(ident);
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
        int count = keventWait(state.mux().kqfd(), events, buffers.length);
        if(count == -1) {
            if(Thread.currentThread().isInterrupted()) {
                // already shutdown
                return ;
            }else {
                // kqueue wait failed
                log.error("kevent failed with errno : {}", errno());
            }
        }
        for(int i = 0; i < count; i++) {
            long baseOffset = i * keventSize;
            int ident = (int) NativeUtil.getLong(events, baseOffset + identOffset);
            short filter = NativeUtil.getShort(events, baseOffset + filterOffset);
            short flags = NativeUtil.getShort(events, baseOffset + flagsOffset);
            Channel channel = channelMap.get(ident);
            if(channel != null) {
                if((flags & Constants.EV_EOF) != 0) {
                    // remote current socket
                    channel.close();
                }else if((filter & Constants.EVFILT_READ) != 0) {
                    // read event
                    ReadBuffer readBuffer = buffers[i];
                    int readableBytes = recv(ident, readBuffer.segment(), readBuffer.len());
                    if(readableBytes > 0) {
                        // recv data from remote peer
                        readBuffer.setWriteIndex(readableBytes);
                        channel.onReadBuffer(readBuffer);
                    }else {
                        // shouldn't happen in kqueue implementation, we already checked whether channel was closed
                        throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
                    }
                }else if((filter & Constants.EVFILT_WRITE) != 0) {
                    // write event
                    LockSupport.unpark(channel.writerThread());
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
    public void shutdownWrite(Socket socket) {
        check(shutdownWrite(socket.intValue()), "shutdown write");
    }

    @Override
    public int send(Socket socket, MemorySegment data, int len) {
        return send(socket.intValue(), data, len);
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
        keventCtlMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_kevent_ctl", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_SHORT, ValueLayout.JAVA_SHORT));
        keventWaitMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_kevent_wait", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
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
        getErrOptMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_get_err_opt", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        setNonBlockingMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_set_nonblocking", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        bindMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_bind", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        connectMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_connect", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        listenMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_listen", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        recvMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_recv", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        sendMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_send", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        closeMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_close", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        shutdownWriteMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_shutdown_write", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
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
     *  corresponding to `int m_kevent_ctl(int kq, int fd, int16_t filter, uint16_t flags)`
     */
    public int keventCtl(int kq, int fd, short filter, short flags) {
        try{
            return (int) keventCtlMethodHandle.invokeExact(kq, fd, filter, flags);
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
     *  corresponding to `int m_get_err_opt(int socket)`
     */
    public int getErrOpt(int socket) {
        try{
            return (int) getErrOptMethodHandle.invokeExact(socket);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking getErrOpt()", throwable);
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
     *  corresponding to `int m_bind(int socket, struct sockaddr_in* sockAddr, socklen_t size)`
     */
    public int bind(int socket, MemorySegment sockAddr, int size) {
        try{
            return (int) bindMethodHandle.invokeExact(socket, sockAddr, size);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking bind()", throwable);
        }
    }

    /**
     *  corresponding to `int m_connect(int socket, struct sockaddr_in* sockAddr, socklen_t size)`
     */
    public int connect(int socket, MemorySegment sockAddr, int size) {
        try{
            return (int) connectMethodHandle.invokeExact(sockAddr, socket, size);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking connect()", throwable);
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
     *  corresponding to `ssize_t m_recv(int socket, void* buf, size_t len)`
     */
    public int recv(int socket, MemorySegment buf, int len) {
        try{
            return (int) recvMethodHandle.invokeExact(socket, buf, len);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking recv()", throwable);
        }
    }

    /**
     *  corresponding to `ssize_t m_send(int socket, void* buf, size_t len)`
     */
    public int send(int socket, MemorySegment buf, int len) {
        try{
            return (int) sendMethodHandle.invokeExact(socket, buf, len);
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
     *  corresponding to `int m_shutdown_write(int fd)`
     */
    public int shutdownWrite(int fd) {
        try{
            return (int) shutdownWriteMethodHandle.invokeExact(fd);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking shutdownWrite()", throwable);
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
