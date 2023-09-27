package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.binding.TenetWindowsBinding;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.pojo.Loc;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Map;

/**
 *  Native implementation under Windows, using wepoll
 */
public final class WinNative implements Native {
    private static final Logger log = new Logger(WinNative.class);
    /**
     *  Corresponding to union epoll_data in wepoll.h
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
     *  Corresponding to struct epoll_event in wepoll.h
     */
    private static final MemoryLayout epollEventLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("events"),
            MemoryLayout.paddingLayout(4),
            epollDataLayout.withName("data")
    );
    private static final long eventSize = epollEventLayout.byteSize();
    private static final long eventsOffset = epollEventLayout.byteOffset(MemoryLayout.PathElement.groupElement("events"));
    private static final long dataOffset = epollEventLayout.byteOffset(MemoryLayout.PathElement.groupElement("data"));
    private static final long sockOffset = epollDataLayout.byteOffset(MemoryLayout.PathElement.groupElement("sock"));

    /**
     *  Corresponding to struct sockaddr_in
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
    private final long invalidSocket;

    public WinNative() {
        addressLen = TenetWindowsBinding.addressLen();
        connectBlockCode = TenetWindowsBinding.connectBlockCode();
        sendBlockCode = TenetWindowsBinding.sendBlockCode();
        invalidSocket = TenetWindowsBinding.invalidSocket();
    }

    /**
     *   Corresponding to `int w_connect_block_code()`
     */
    @Override
    public int connectBlockCode() {
        return connectBlockCode;
    }

    /**
     *   Corresponding to `int w_send_block_code()`
     */
    @Override
    public int sendBlockCode() {
        return sendBlockCode;
    }

    @Override
    public int interruptCode() {
        return Constants.WSAEINTR;
    }

    @Override
    public MemorySegment createSockAddr(Loc loc, Arena arena) {
        MemorySegment r = arena.allocate(sockAddrLayout);
        MemorySegment ip = NativeUtil.allocateStr(arena, loc.ip(), addressLen);
        if(check(TenetWindowsBinding.setSockAddr(r, ip, loc.shortPort()), "setSockAddr") == Constants.ZERO) {
            throw new FrameworkException(ExceptionType.NETWORK, "Loc is not valid");
        }
        return r;
    }

    @Override
    public Mux createMux() {
        MemorySegment winHandle = TenetWindowsBinding.epollCreate();
        if(NativeUtil.checkNullPointer(winHandle)) {
            throw new FrameworkException(ExceptionType.NETWORK, "Failed to create wepoll with NULL pointer exception");
        }
        return Mux.win(winHandle);
    }

    @Override
    public MemorySegment createEventsArray(MuxConfig config, Arena arena) {
        MemoryLayout eventsArrayLayout = MemoryLayout.sequenceLayout(config.getMaxEvents(), epollEventLayout);
        return arena.allocate(eventsArrayLayout);
    }

    @Override
    public Socket createSocket() {
        long socketFd = check(TenetWindowsBinding.socketCreate(), "create socket");
        return new Socket(socketFd);
    }

    @Override
    public void configureSocket(NetworkConfig config, Socket socket) {
        long socketFd = socket.longValue();
        check(TenetWindowsBinding.setReuseAddr(socketFd, config.getReuseAddr() > 0 ? 1 : 0), "set SO_REUSE_ADDR");
        check(TenetWindowsBinding.setKeepAlive(socketFd, config.getKeepAlive() > 0 ? 1 : 0), "set SO_KEEPALIVE");
        check(TenetWindowsBinding.setTcpNoDelay(socketFd, config.getTcpNoDelay() > 0 ? 1 : 0), "set TCP_NODELAY");
        check(TenetWindowsBinding.setNonBlocking(socketFd), "set NON_BLOCKING");
    }

    @Override
    public void bindAndListen(Loc loc, MuxConfig config, Socket socket) {
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment addr = arena.allocate(sockAddrLayout);
            MemorySegment ip = NativeUtil.allocateStr(arena, loc.ip(), addressLen);
            int setSockAddr = check(TenetWindowsBinding.setSockAddr(addr, ip, loc.shortPort()), "set SockAddr");
            if(setSockAddr == 0) {
                throw new FrameworkException(ExceptionType.NETWORK, "Network address is not valid");
            }
            check(TenetWindowsBinding.bind(socket.longValue(), addr, sockAddrSize), "bind");
            check(TenetWindowsBinding.listen(socket.longValue(), config.getBacklog()), "listen");
        }
    }

    @Override
    public void ctl(Mux mux, Socket socket, int from, int to) {
        if(from == to) {
            return ;
        }
        MemorySegment winHandle = mux.winHandle();
        long fd = socket.longValue();
        if(to == Native.REGISTER_NONE) {
            check(TenetWindowsBinding.epollCtl(winHandle, Constants.EPOLL_CTL_DEL, fd, NativeUtil.NULL_POINTER), "epollCtl");
        }else {
            int target = ((to & Native.REGISTER_READ) != 0 ? Constants.EPOLL_IN | Constants.EPOLL_RDHUP : 0) |
                    ((to & Native.REGISTER_WRITE) != 0 ? Constants.EPOLL_OUT : 0);
            try(Arena arena = Arena.ofConfined()) {
                MemorySegment ev = arena.allocate(epollEventLayout);
                NativeUtil.setInt(ev, eventsOffset, target);
                NativeUtil.setLong(ev, dataOffset + sockOffset, fd);
                check(TenetWindowsBinding.epollCtl(winHandle, from == Native.REGISTER_NONE ? Constants.EPOLL_CTL_ADD : Constants.EPOLL_CTL_MOD, fd, ev), "epollCtl");
            }
        }
    }

    @Override
    public int multiplexingWait(Mux mux, MemorySegment events, int maxEvents, Timeout timeout) {
        return TenetWindowsBinding.epollWait(mux.winHandle(), events, maxEvents, timeout.val());
    }

    @Override
    public ClientSocket waitForAccept(NetworkConfig config, Socket serverSocket, MemorySegment events, int index) {
        int event = NativeUtil.getInt(events, index * eventSize + eventsOffset);
        long socket = NativeUtil.getLong(events, index * eventSize + dataOffset + sockOffset);
        if(socket == serverSocket.longValue() && (event & Constants.EPOLL_IN) != 0) {
            return accept(config, serverSocket);
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public void waitForData(Map<Socket, Actor> socketMap, MemorySegment buffer, MemorySegment events, int index) {
        int event = NativeUtil.getInt(events, index * eventSize + eventsOffset);
        Socket socket = new Socket(NativeUtil.getLong(events, index * eventSize + dataOffset + sockOffset));
        Actor actor = socketMap.get(socket);
        if(actor != null) {
            if((event & (Constants.EPOLL_IN | Constants.EPOLL_HUP | Constants.EPOLL_RDHUP)) != Constants.ZERO) {
                actor.canRead(buffer);
            }else if((event & Constants.EPOLL_OUT) != Constants.ZERO) {
                actor.canWrite();
            }else {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
        }
    }

    @Override
    public int connect(Socket socket, MemorySegment sockAddr) {
        return TenetWindowsBinding.connect(socket.longValue(), sockAddr, addressLen);
    }

    @Override
    public ClientSocket accept(NetworkConfig config, Socket socket) {
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment clientAddr = arena.allocate(sockAddrLayout);
            MemorySegment address = arena.allocateArray(ValueLayout.JAVA_BYTE, addressLen);
            long socketFd = TenetWindowsBinding.accept(socket.longValue(), clientAddr, sockAddrSize);
            if(socketFd == invalidSocket) {
                throw new FrameworkException(ExceptionType.NETWORK, "Failed to accept client socket, errno : %d".formatted(errno()));
            }
            Socket clientSocket = new Socket(socketFd);
            configureSocket(config, clientSocket);
            if(TenetWindowsBinding.address(clientAddr, address, addressLen) == -1) {
                throw new FrameworkException(ExceptionType.NETWORK, "Failed to get client socket's remote address, errno : %d".formatted(errno()));
            }
            String ip = NativeUtil.getStr(address, addressLen);
            int port = Loc.toIntPort(TenetWindowsBinding.port(clientAddr));
            Loc loc = new Loc(ip, port);
            return new ClientSocket(clientSocket, loc);
        }
    }

    @Override
    public long recv(Socket socket, MemorySegment data, long len) {
        return TenetWindowsBinding.recv(socket.longValue(), data, (int) len);
    }

    @Override
    public long send(Socket socket, MemorySegment data, long len) {
        return TenetWindowsBinding.send(socket.longValue(), data, (int) len);
    }

    @Override
    public int getErrOpt(Socket socket) {
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment ptr = arena.allocate(ValueLayout.JAVA_INT, Integer.MIN_VALUE);
            check(TenetWindowsBinding.getErrOpt(socket.longValue(), ptr), "get socket err opt");
            return NativeUtil.getInt(ptr, Constants.ZERO);
        }
    }

    @Override
    public void closeSocket(Socket socket) {
        check(TenetWindowsBinding.closeSocket(socket.longValue()), "close socket");
    }

    @Override
    public void shutdownWrite(Socket socket) {
        check(TenetWindowsBinding.shutdownWrite(socket.longValue()), "shutdown write");
    }

    @Override
    public void exitMux(Mux mux) {
        check(TenetWindowsBinding.epollClose(mux.winHandle()), "close wepoll fd");
    }

    @Override
    public void exit() {
        check(TenetWindowsBinding.wsaCleanUp(), "wsa_clean_up");
    }

    @Override
    public int errno() {
        return TenetWindowsBinding.wsaGetLastError();
    }
}
