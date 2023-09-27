package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.binding.TenetLinuxBinding;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.pojo.Loc;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Map;

/**
 *   Native implementation under Linux, using epoll
 */
public final class LinuxNative implements Native {
    /**
     *  Corresponding to union epoll_data in epoll.h
     */
    private static final MemoryLayout epollDataLayout = MemoryLayout.unionLayout(
            ValueLayout.ADDRESS_UNALIGNED.withName("ptr"),
            ValueLayout.JAVA_INT_UNALIGNED.withName("fd"),
            ValueLayout.JAVA_INT_UNALIGNED.withName("u32"),
            ValueLayout.JAVA_LONG_UNALIGNED.withName("u64")
    );
    /**
     *  Corresponding to struct epoll_event in epoll.h
     *  Note that epoll_event struct is defined as __attribute__ ((__packed__))
     *  so there is no need for a padding layout
     */
    private static final MemoryLayout epollEventLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT_UNALIGNED.withName("events"),
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
            MemoryLayout.paddingLayout(8)
    );
    private static final int sockAddrSize = (int) sockAddrLayout.byteSize();
    private final int addressLen;
    private final int connectBlockCode;
    private final int sendBlockCode;

    public LinuxNative() {
        addressLen = TenetLinuxBinding.addressLen();
        connectBlockCode = TenetLinuxBinding.connectBlockCode();
        sendBlockCode = TenetLinuxBinding.sendBlockCode();
    }

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
    public int interruptCode() {
        return Constants.EINTR;
    }

    @Override
    public MemorySegment createSockAddr(Loc loc, Arena arena) {
        MemorySegment r = arena.allocate(sockAddrLayout);
        MemorySegment ip = NativeUtil.allocateStr(arena, loc.ip(), addressLen);
        if(check(TenetLinuxBinding.setSockAddr(r, ip, loc.shortPort()), "setSockAddr") == 0) {
            throw new FrameworkException(ExceptionType.NETWORK, "Loc is not valid");
        }
        return r;
    }

    @Override
    public Mux createMux() {
        int epfd = check(TenetLinuxBinding.epollCreate(), "epoll create");
        return Mux.linux(epfd);
    }

    @Override
    public MemorySegment createEventsArray(MuxConfig config, Arena arena) {
        MemoryLayout eventsArrayLayout = MemoryLayout.sequenceLayout(config.getMaxEvents(), epollEventLayout);
        return arena.allocate(eventsArrayLayout);
    }

    @Override
    public Socket createSocket() {
        int socketFd = check(TenetLinuxBinding.socketCreate(), "create socket");
        return new Socket(socketFd);
    }

    @Override
    public void configureSocket(NetworkConfig config, Socket socket) {
        int socketFd = socket.intValue();
        check(TenetLinuxBinding.setReuseAddr(socketFd, config.getReuseAddr() > 0 ? 1 : 0), "set SO_REUSE_ADDR");
        check(TenetLinuxBinding.setKeepAlive(socketFd, config.getKeepAlive() > 0 ? 1 : 0), "set SO_KEEPALIVE");
        check(TenetLinuxBinding.setTcpNoDelay(socketFd, config.getTcpNoDelay() > 0 ? 1 : 0), "set TCP_NODELAY");
        check(TenetLinuxBinding.setNonBlocking(socketFd), "set NON_BLOCKING");
    }

    @Override
    public void bindAndListen(Loc loc, MuxConfig config, Socket socket) {
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment addr = arena.allocate(sockAddrLayout);
            MemorySegment ip = NativeUtil.allocateStr(arena, loc.ip(), addressLen);
            int setSockAddr = check(TenetLinuxBinding.setSockAddr(addr, ip, loc.shortPort()), "set SockAddr");
            if(setSockAddr == 0) {
                throw new FrameworkException(ExceptionType.NETWORK, "Network address is not valid");
            }
            check(TenetLinuxBinding.bind(socket.intValue(), addr, sockAddrSize), "bind");
            check(TenetLinuxBinding.listen(socket.intValue(), config.getBacklog()), "listen");
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
            check(TenetLinuxBinding.epollCtl(epfd, Constants.EPOLL_CTL_DEL, fd, NativeUtil.NULL_POINTER), "epoll_ctl");
        }else {
            int target = ((to & Native.REGISTER_READ) != 0 ? Constants.EPOLL_IN | Constants.EPOLL_RDHUP : 0) |
                    ((to & Native.REGISTER_WRITE) != 0 ? Constants.EPOLL_OUT : 0);
            try(Arena arena = Arena.ofConfined()) {
                MemorySegment ev = arena.allocate(epollEventLayout);
                NativeUtil.setInt(ev, eventsOffset, target);
                NativeUtil.setInt(ev, dataOffset + fdOffset, fd);
                check(TenetLinuxBinding.epollCtl(epfd, from == Native.REGISTER_NONE ? Constants.EPOLL_CTL_ADD : Constants.EPOLL_CTL_MOD, fd, ev), "epoll_ctl");
            }
        }
    }

    @Override
    public int multiplexingWait(Mux mux, MemorySegment events, int maxEvents, Timeout timeout) {
        return TenetLinuxBinding.epollWait(mux.epfd(), events, maxEvents, timeout.val());
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
    public void waitForData(Map<Socket, Actor> socketMap, MemorySegment buffer, MemorySegment events, int index) {
        int event = NativeUtil.getInt(events, index * eventSize + eventsOffset);
        Socket socket = new Socket(NativeUtil.getInt(events, index * eventSize + dataOffset + fdOffset));
        Actor actor = socketMap.get(socket);
        if(actor != null) {
            if((event & (Constants.EPOLL_IN | Constants.EPOLL_HUP | Constants.EPOLL_RDHUP)) != 0) {
                actor.canRead(buffer);
            }else if((event & Constants.EPOLL_OUT) != 0) {
                actor.canWrite();
            }else {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
        }
    }

    @Override
    public int connect(Socket socket, MemorySegment sockAddr) {
        return TenetLinuxBinding.connect(socket.intValue(), sockAddr, addressLen);
    }

    @Override
    public ClientSocket accept(NetworkConfig config, Socket socket) {
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment clientAddr = arena.allocate(sockAddrLayout);
            MemorySegment address = arena.allocateArray(ValueLayout.JAVA_BYTE, addressLen);
            int socketFd = TenetLinuxBinding.accept(socket.intValue(), clientAddr, sockAddrSize);
            if(socketFd == -1) {
                throw new FrameworkException(ExceptionType.NETWORK, "Failed to accept client socket, errno : %d".formatted(errno()));
            }
            Socket clientSocket = new Socket(socketFd);
            configureSocket(config, clientSocket);
            if(TenetLinuxBinding.address(clientAddr, address, addressLen) == -1) {
                throw new FrameworkException(ExceptionType.NETWORK, "Failed to get client socket's remote address, errno : %d".formatted(errno()));
            }
            String ip = NativeUtil.getStr(address, addressLen);
            int port = Loc.toIntPort(TenetLinuxBinding.port(clientAddr));
            Loc loc = new Loc(ip, port);
            return new ClientSocket(clientSocket, loc);
        }
    }

    @Override
    public long recv(Socket socket, MemorySegment data, long len) {
        return TenetLinuxBinding.recv(socket.intValue(), data, len);
    }

    @Override
    public long send(Socket socket, MemorySegment data, long len) {
        return TenetLinuxBinding.send(socket.intValue(), data, len);
    }

    @Override
    public int getErrOpt(Socket socket) {
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment ptr = arena.allocate(ValueLayout.JAVA_INT, Integer.MIN_VALUE);
            check(TenetLinuxBinding.getErrOpt(socket.intValue(), ptr), "get socket err opt");
            return NativeUtil.getInt(ptr, Constants.ZERO);
        }
    }

    @Override
    public void closeSocket(Socket socket) {
        check(TenetLinuxBinding.close(socket.intValue()), "close socket");
    }

    @Override
    public void shutdownWrite(Socket socket) {
        check(TenetLinuxBinding.shutdownWrite(socket.intValue()), "shutdown write");
    }

    @Override
    public void exitMux(Mux mux) {
        check(TenetLinuxBinding.close(mux.epfd()), "close epoll fd");
    }

    @Override
    public void exit() {
        // No action, epoll doesn't need external operations for clean up
    }

    @Override
    public int errno() {
        return TenetLinuxBinding.errno();
    }
}
