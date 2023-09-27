package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.binding.TenetMacosBinding;
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
 *   Native implementation under macOS, using kqueue
 *   Note that the .dylib library is only suitable for ARM-based chips since I only tested on M1 series MacBook
 *   If developer needs to run it on X86 processors, recompile a new .dylib would work, it should be working on freebsd or openbsd operating system too since they are quite similar to macOS
 */
public final class MacNative implements Native {
    private static final Logger log = new Logger(MacNative.class);
    /**
     *  Corresponding to struct kevent in event.h
     */
    private static final MemoryLayout keventLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_LONG_UNALIGNED.withName("ident"),
            ValueLayout.JAVA_SHORT_UNALIGNED.withName("filter"),
            ValueLayout.JAVA_SHORT_UNALIGNED.withName("flags"),
            ValueLayout.JAVA_INT_UNALIGNED.withName("fflags"),
            ValueLayout.JAVA_LONG_UNALIGNED.withName("data"),
            ValueLayout.ADDRESS_UNALIGNED.withName("udata")
    ).withByteAlignment(4L);
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
    private final int addressLen;
    private final int connectBlockCode;
    private final int sendBlockCode;

    public MacNative() {
        addressLen = TenetMacosBinding.addressLen();
        connectBlockCode = TenetMacosBinding.connectBlockCode();
        sendBlockCode = TenetMacosBinding.sendBlockCode();
    }


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
        if(check(TenetMacosBinding.setSockAddr(r, ip, loc.shortPort()), "setSockAddr") == Constants.ZERO) {
            throw new FrameworkException(ExceptionType.NETWORK, "Loc is not valid");
        }
        return r;
    }

    @Override
    public Mux createMux() {
        int kqfd = check(TenetMacosBinding.kqueue(), "kqueue create");
        return Mux.mac(kqfd);
    }

    @Override
    public MemorySegment createEventsArray(MuxConfig config, Arena arena) {
        MemoryLayout eventsArrayLayout = MemoryLayout.sequenceLayout(config.getMaxEvents(), keventLayout);
        return arena.allocate(eventsArrayLayout);
    }

    @Override
    public Socket createSocket() {
        int socketFd = check(TenetMacosBinding.socketCreate(), "create socket");
        return new Socket(socketFd);
    }

    @Override
    public void configureSocket(NetworkConfig config, Socket socket) {
        int socketFd = socket.intValue();
        check(TenetMacosBinding.setReuseAddr(socketFd, config.getReuseAddr() > 0 ? 1 : 0), "set SO_REUSE_ADDR");
        check(TenetMacosBinding.setKeepAlive(socketFd, config.getKeepAlive() > 0 ? 1 : 0), "set SO_KEEPALIVE");
        check(TenetMacosBinding.setTcpNoDelay(socketFd, config.getTcpNoDelay() > 0 ? 1 : 0), "set TCP_NODELAY");
        check(TenetMacosBinding.setNonBlocking(socketFd), "set NON_BLOCKING");
    }

    @Override
    public void bindAndListen(Loc loc, MuxConfig config, Socket socket) {
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment addr = arena.allocate(sockAddrLayout);
            MemorySegment ip = NativeUtil.allocateStr(arena, loc.ip(), addressLen);
            int setSockAddr = check(TenetMacosBinding.setSockAddr(addr, ip, loc.shortPort()), "set SockAddr");
            if(setSockAddr == 0) {
                throw new FrameworkException(ExceptionType.NETWORK, "Network address is not valid");
            }
            check(TenetMacosBinding.bind(socket.intValue(), addr, sockAddrSize), "bind");
            check(TenetMacosBinding.listen(socket.intValue(), config.getBacklog()), "listen");
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
            check(TenetMacosBinding.keventCtl(kqfd, fd, Constants.EVFILT_READ, r1 > r2 ? Constants.EV_DELETE : Constants.EV_ADD), "kevent_ctl");
        }
        int w1 = from & Native.REGISTER_WRITE, w2 = to & Native.REGISTER_WRITE;
        if(w1 != w2) {
            check(TenetMacosBinding.keventCtl(kqfd, fd, Constants.EVFILT_WRITE, w1 > w2 ? Constants.EV_DELETE : Constants.EV_ADD), "kevent_ctl");
        }
    }

    @Override
    public int multiplexingWait(Mux mux, MemorySegment events, int maxEvents, Timeout timeout) {
        return TenetMacosBinding.keventWait(mux.kqfd(), events, maxEvents, timeout.ptr());
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
        return TenetMacosBinding.connect(socket.intValue(), sockAddr, addressLen);
    }

    @Override
    public ClientSocket accept(NetworkConfig config, Socket socket) {
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment clientAddr = arena.allocate(sockAddrLayout);
            MemorySegment address = arena.allocateArray(ValueLayout.JAVA_BYTE, addressLen);
            int socketFd = TenetMacosBinding.accept(socket.intValue(), clientAddr, sockAddrSize);
            if(socketFd == -1) {
                throw new FrameworkException(ExceptionType.NETWORK, "Failed to accept client socket, errno : %d".formatted(errno()));
            }
            Socket clientSocket = new Socket(socketFd);
            configureSocket(config, clientSocket);
            if(TenetMacosBinding.address(clientAddr, address, addressLen) == -1) {
                throw new FrameworkException(ExceptionType.NETWORK, "Failed to get client socket's remote address, errno : %d".formatted(errno()));
            }
            String ip = NativeUtil.getStr(address, addressLen);
            int port = Loc.toIntPort(TenetMacosBinding.port(clientAddr));
            Loc loc = new Loc(ip, port);
            return new ClientSocket(clientSocket, loc);
        }
    }

    @Override
    public long recv(Socket socket, MemorySegment data, long len) {
        return TenetMacosBinding.recv(socket.intValue(), data, len);
    }

    @Override
    public long send(Socket socket, MemorySegment data, long len) {
        return TenetMacosBinding.send(socket.intValue(), data, len);
    }

    @Override
    public int getErrOpt(Socket socket) {
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment ptr = arena.allocate(ValueLayout.JAVA_INT, Integer.MIN_VALUE);
            check(TenetMacosBinding.getErrOpt(socket.intValue(), ptr), "get socket err opt");
            return NativeUtil.getInt(ptr, Constants.ZERO);
        }
    }

    @Override
    public void closeSocket(Socket socket) {
        check(TenetMacosBinding.close(socket.intValue()), "close socket");
    }

    @Override
    public void shutdownWrite(Socket socket) {
        check(TenetMacosBinding.shutdownWrite(socket.intValue()), "shutdown write");
    }

    @Override
    public void exitMux(Mux mux) {
        check(TenetMacosBinding.close(mux.kqfd()), "close kqueue fd");
    }

    @Override
    public void exit() {
        // No action, kqueue doesn't need external operations for clean up
    }

    @Override
    public int errno() {
        return TenetMacosBinding.errno();
    }
}
