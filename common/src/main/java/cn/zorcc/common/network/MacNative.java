package cn.zorcc.common.network;

import cn.zorcc.common.Clock;
import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.pojo.Loc;
import cn.zorcc.common.util.NativeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 *   Native implementation under MacOS, using kqueue
 *   Note that the .dylib library is only suitable for ARM-based chips since I only tested on M1 series MacBook
 *   If developer needs to run it on X86 processors, recompile a new .dylib would work, it should be working on freebsd or openbsd operating system too since they are quite similar to macos
 */
public final class MacNative implements Native {
    private static final Logger log = LoggerFactory.getLogger(MacNative.class);
    /**
     *  Corresponding to struct kevent in event.h
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


    /**
     *  Corresponding to struct sockaddr_in in in.h
     */
    private static final MemoryLayout sockAddrLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_BYTE.withName("sin_len"),
            ValueLayout.JAVA_BYTE.withName("sin_family"),
            ValueLayout.JAVA_SHORT.withName("sin_port"),
            ValueLayout.JAVA_INT.withName("sin_addr"),
            MemoryLayout.paddingLayout(8)
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
    public int interruptCode() {
        return Constants.EINTR;
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
        int kqfd = check(kqueue(), "kqueue create");
        return Mux.mac(kqfd);
    }

    @Override
    public MemorySegment createEventsArray(MuxConfig config, Arena arena) {
        MemoryLayout eventsArrayLayout = MemoryLayout.sequenceLayout(config.getMaxEvents(), keventLayout);
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
        try(Arena arena = Arena.ofConfined()) {
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
        int kqfd = mux.kqfd();
        int fd = socket.intValue();
        int r1 = from & Native.REGISTER_READ, r2 = to & Native.REGISTER_READ;
        if(r1 != r2) {
            check(keventCtl(kqfd, fd, Constants.EVFILT_READ, r1 > r2 ? Constants.EV_DELETE : Constants.EV_ADD), "kevent_ctl");
        }
        int w1 = from & Native.REGISTER_WRITE, w2 = to & Native.REGISTER_WRITE;
        if(w1 != w2) {
            check(keventCtl(kqfd, fd, Constants.EVFILT_WRITE, w1 > w2 ? Constants.EV_DELETE : Constants.EV_ADD), "kevent_ctl");
        }
    }

    @Override
    public int multiplexingWait(Mux mux, MemorySegment events, int maxEvents, Timeout timeout) {
        return keventWait(mux.kqfd(), events, maxEvents, timeout.ptr());
    }

    @Override
    public ClientSocket waitForAccept(NetworkConfig config, Socket serverSocket, MemorySegment events, int index) {
        short filter = NativeUtil.getShort(events, index * keventSize + filterOffset);
        int ident = (int) NativeUtil.getLong(events, index * keventSize + identOffset);
        if(ident == serverSocket.intValue() && filter == Constants.EVFILT_READ) {
            return accept(config, serverSocket);
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public void waitForData(Map<Socket, Actor> socketMap, MemorySegment buffer, MemorySegment events, int index) {
        short filter = NativeUtil.getShort(events, index * keventSize + filterOffset);
        Socket socket = new Socket(NativeUtil.getInt(events, index * keventSize + identOffset));
        Actor actor = socketMap.get(socket);
        if(actor != null) {
            if(filter == Constants.EVFILT_READ) {
                actor.canRead(buffer);
            }else if(filter == Constants.EVFILT_WRITE) {
                actor.canWrite();
            }else {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
        }
    }

    @Override
    public int connect(Socket socket, MemorySegment sockAddr) {
        return connect(socket.intValue(), sockAddr, addressLen);
    }

    @Override
    public ClientSocket accept(NetworkConfig config, Socket socket) {
        try(Arena arena = Arena.ofConfined()) {
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
        try(Arena arena = Arena.ofConfined()) {
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
        check(close(mux.kqfd()), "close kqueue fd");
    }

    @Override
    public void exit() {
        // No action, kqueue doesn't need external operations for clean up
    }

    static {
        long nano = Clock.nano();
        SymbolLookup symbolLookup = NativeUtil.loadLibrary(Native.LIB);
        kqueueMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_kqueue", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        keventCtlMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_kevent_ctl", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_SHORT, ValueLayout.JAVA_SHORT));
        keventWaitMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_kevent_wait", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        addressMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_address", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        portMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_port", FunctionDescriptor.of(ValueLayout.JAVA_SHORT, ValueLayout.ADDRESS));
        socketCreateMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_socket_create", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        acceptMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_accept", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        setSockAddrMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_set_sock_addr", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_SHORT));
        setReuseAddrMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_set_reuse_addr", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        setKeepAliveMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_set_keep_alive", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        setTcpNoDelayMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_set_tcp_no_delay", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        getErrOptMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_get_err_opt", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        setNonBlockingMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_set_nonblocking", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        bindMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_bind", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        connectMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_connect", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        listenMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_listen", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        recvMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_recv", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        sendMethodHandle = NativeUtil.methodHandle(symbolLookup,
                "m_send", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
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
        log.info("Initializing Native successfully, platform : {}, time consuming : {} ms", NativeUtil.osName(), TimeUnit.NANOSECONDS.toMillis(Clock.elapsed(nano)));
    }

    /**
     *  Corresponding to `int m_kqueue()`
     */
    public int kqueue() {
        try{
            return (int) kqueueMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking kqueue()", throwable);
        }
    }

    /**
     *  Corresponding to `int m_kevent_ctl(int kq, int fd, int16_t filter, uint16_t flags)`
     */
    public int keventCtl(int kq, int fd, short filter, short flags) {
        try{
            return (int) keventCtlMethodHandle.invokeExact(kq, fd, filter, flags);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking keventCtl()", throwable);
        }
    }

    /**
     *  Corresponding to `int m_kevent_wait(int kq, struct kevent* eventlist, int nevents, struct timespec* timeout)`
     */
    public int keventWait(int kq, MemorySegment eventlist, int nevents, MemorySegment timeout) {
        try{
            return (int) keventWaitMethodHandle.invokeExact(kq, eventlist, nevents, timeout);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking keventWait()", throwable);
        }
    }

    /**
     *  Corresponding to `int m_address(struct sockaddr_in* sockAddr, char* addrStr, socklen_t len)`
     */
    public int address(MemorySegment sockAddr, MemorySegment addrStr, int len) {
        try{
            return (int) addressMethodHandle.invokeExact(sockAddr, addrStr, len);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking address()", throwable);
        }
    }

    /**
     *  Corresponding to `uint16_t m_port(struct sockaddr_in* sockAddr)`
     */
    public short port(MemorySegment sockAddr) {
        try{
            return (short) portMethodHandle.invokeExact(sockAddr);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking port()", throwable);
        }
    }

    /**
     *  Corresponding to `int m_socket_create()`
     */
    public int socketCreate() {
        try{
            return (int) socketCreateMethodHandle.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking socketCreate()", throwable);
        }
    }

    /**
     *  Corresponding to `int m_accept(int socket, struct sockaddr_in* clientAddr, socklen_t clientAddrSize)`
     */
    public int accept(int socket, MemorySegment clientAddr, int clientAddrSize) {
        try{
            return (int) acceptMethodHandle.invokeExact(socket, clientAddr, clientAddrSize);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking accept()", throwable);
        }
    }

    /**
     *  Corresponding to `int m_set_sock_addr(struct sockaddr_in* sockAddr, char* address, uint16_t port)`
     */
    public int setSockAddr(MemorySegment sockAddr, MemorySegment address, short port) {
        try{
            return (int) setSockAddrMethodHandle.invokeExact(sockAddr, address, port);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setSockAddr()", throwable);
        }
    }

    /**
     *  Corresponding to `int m_set_reuse_addr(int socket, int value)`
     */
    public int setReuseAddr(int socket, int value) {
        try{
            return (int) setReuseAddrMethodHandle.invokeExact(socket, value);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setReuseAddr()", throwable);
        }
    }

    /**
     *  Corresponding to `int m_set_keep_alive(int socket, int value)`
     */
    public int setKeepAlive(int socket, int value) {
        try{
            return (int) setKeepAliveMethodHandle.invokeExact(socket, value);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setKeepAlive()", throwable);
        }
    }

    /**
     *  Corresponding to `int m_set_tcp_no_delay(int socket, int value)`
     */
    public int setTcpNoDelay(int socket, int value) {
        try{
            return (int) setTcpNoDelayMethodHandle.invokeExact(socket, value);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setTcpNoDelay()", throwable);
        }
    }

    /**
     *  Corresponding to `int m_get_err_opt(int socket, int* ptr)`
     */
    public int getErrOpt(int socket, MemorySegment ptr) {
        try{
            return (int) getErrOptMethodHandle.invokeExact(socket, ptr);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking getErrOpt()", throwable);
        }
    }

    /**
     *  Corresponding to `int m_set_nonblocking(int socket)`
     */
    public int setNonBlocking(int socket) {
        try{
            return (int) setNonBlockingMethodHandle.invokeExact(socket);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setNonBlocking()", throwable);
        }
    }

    /**
     *  Corresponding to `int m_bind(int socket, struct sockaddr_in* sockAddr, socklen_t size)`
     */
    public int bind(int socket, MemorySegment sockAddr, int size) {
        try{
            return (int) bindMethodHandle.invokeExact(socket, sockAddr, size);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking bind()", throwable);
        }
    }

    /**
     *  Corresponding to `int m_connect(int socket, struct sockaddr_in* sockAddr, socklen_t size)`
     */
    public int connect(int socket, MemorySegment sockAddr, int size) {
        try{
            return (int) connectMethodHandle.invokeExact(socket, sockAddr, size);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking connect()", throwable);
        }
    }

    /**
     *  Corresponding to `int m_listen(int socket, int backlog)`
     */
    public int listen(int socket, int backlog) {
        try{
            return (int) listenMethodHandle.invokeExact(socket, backlog);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking listen()", throwable);
        }
    }

    /**
     *  Corresponding to `ssize_t m_recv(int socket, void* buf, size_t len)`
     */
    public long recv(int socket, MemorySegment buf, long len) {
        try{
            return (long) recvMethodHandle.invokeExact(socket, buf, len);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking recv()", throwable);
        }
    }

    /**
     *  Corresponding to `ssize_t m_send(int socket, void* buf, size_t len)`
     */
    public long send(int socket, MemorySegment buf, long len) {
        try{
            return (long) sendMethodHandle.invokeExact(socket, buf, len);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking recv()", throwable);
        }
    }

    /**
     *  Corresponding to `int m_close(int fd)`
     */
    public int close(int fd) {
        try{
            return (int) closeMethodHandle.invokeExact(fd);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking close()", throwable);
        }
    }

    /**
     *  Corresponding to `int m_shutdown_write(int fd)`
     */
    public int shutdownWrite(int fd) {
        try{
            return (int) shutdownWriteMethodHandle.invokeExact(fd);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking shutdownWrite()", throwable);
        }
    }

    /**
     *  Corresponding to `int m_errno()`
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
