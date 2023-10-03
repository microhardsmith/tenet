package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.binding.TenetLinuxBinding;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 *   Native implementation under Linux, using epoll
 */
public final class LinuxNetworkLibrary implements OsNetworkLibrary {
    private static final MemoryLayout epollDataLayout = MemoryLayout.unionLayout(
            ValueLayout.ADDRESS_UNALIGNED.withName("ptr"),
            ValueLayout.JAVA_INT_UNALIGNED.withName("fd"),
            ValueLayout.JAVA_INT_UNALIGNED.withName("u32"),
            ValueLayout.JAVA_LONG_UNALIGNED.withName("u64")
    );
    private static final MemoryLayout epollEventLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT_UNALIGNED.withName("events"),
            epollDataLayout.withName("data")
    );
    private static final long eventSize = epollEventLayout.byteSize();
    private static final long eventsOffset = epollEventLayout.byteOffset(MemoryLayout.PathElement.groupElement("events"));
    private static final long dataOffset = epollEventLayout.byteOffset(MemoryLayout.PathElement.groupElement("data"));
    private static final long fdOffset = epollDataLayout.byteOffset(MemoryLayout.PathElement.groupElement("fd"));
    private final int connectBlockCode;
    private final int sendBlockCode;
    private final int interruptCode;
    private final int ipv4AddressLen;
    private final int ipv6AddressLen;
    private final int ipv4AddressSize;
    private final int ipv6AddressSize;

    public LinuxNetworkLibrary() {
        connectBlockCode = TenetLinuxBinding.connectBlockCode();
        sendBlockCode = TenetLinuxBinding.sendBlockCode();
        interruptCode = TenetLinuxBinding.interruptCode();
        ipv4AddressLen = TenetLinuxBinding.ipv4AddressLen();
        ipv6AddressLen = TenetLinuxBinding.ipv6AddressLen();
        ipv4AddressSize = TenetLinuxBinding.ipv4AddressSize();
        ipv6AddressSize = TenetLinuxBinding.ipv6AddressSize();
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
        return interruptCode;
    }

    @Override
    public int ipv4AddressLen() {
        return ipv4AddressLen;
    }

    @Override
    public int ipv6AddressLen() {
        return ipv6AddressLen;
    }

    @Override
    public int ipv4AddressSize() {
        return ipv4AddressSize;
    }

    @Override
    public int ipv6AddressSize() {
        return ipv6AddressSize;
    }

    @Override
    public int setIpv4SockAddr(MemorySegment sockAddr, MemorySegment address, short port) {
        return TenetLinuxBinding.setIpv4SockAddr(sockAddr, address, port);
    }

    @Override
    public int setIpv6SockAddr(MemorySegment sockAddr, MemorySegment address, short port) {
        return TenetLinuxBinding.setIpv6SockAddr(sockAddr, address, port);
    }

    @Override
    public Mux createMux() {
        int epfd = checkInt(TenetLinuxBinding.epollCreate(), "epoll create");
        return Mux.linux(epfd);
    }

    @Override
    public MemoryLayout eventLayout() {
        return epollEventLayout;
    }

    @Override
    public Socket createIpv4Socket() {
        int fd = checkInt(TenetLinuxBinding.ipv4SocketCreate(), "ipv4 socket create");
        return new Socket(fd);
    }

    @Override
    public Socket createIpv6Socket() {
        int fd = checkInt(TenetLinuxBinding.ipv6SocketCreate(), "ipv6 socket create");
        return new Socket(fd);
    }

    @Override
    public void setReuseAddr(Socket socket, boolean b) {
        checkInt(TenetLinuxBinding.setReuseAddr(socket.intValue(), b ? Constants.ONE : Constants.ZERO), "set SO_REUSE_ADDR");
    }

    @Override
    public void setKeepAlive(Socket socket, boolean b) {
        checkInt(TenetLinuxBinding.setKeepAlive(socket.intValue(), b ? Constants.ONE : Constants.ZERO), "set SO_KEEPALIVE");
    }

    @Override
    public void setTcpNoDelay(Socket socket, boolean b) {
        checkInt(TenetLinuxBinding.setTcpNoDelay(socket.intValue(), b ? Constants.ONE : Constants.ZERO), "set TCP_NODELAY");
    }

    @Override
    public void setIpv6Only(Socket socket, boolean b) {
        checkInt(TenetLinuxBinding.setIpv6Only(socket.intValue(), b ? Constants.ONE : Constants.ZERO), "set IPv6 only");
    }

    @Override
    public void setNonBlocking(Socket socket) {
        checkInt(TenetLinuxBinding.setNonBlocking(socket.intValue()), "set NON_BLOCKING");
    }

    @Override
    public int bind(Socket socket, MemorySegment addr) {
        return TenetLinuxBinding.bind(socket.intValue(), addr, (int) addr.byteSize());
    }

    @Override
    public int listen(Socket socket, int backlog) {
        return TenetLinuxBinding.listen(socket.intValue(), backlog);
    }

    @Override
    public void ctl(Mux mux, Socket socket, int from, int to) {
        if(from == to) {
            return ;
        }
        int epfd = mux.epfd();
        int fd = socket.intValue();
        if(to == OsNetworkLibrary.REGISTER_NONE) {
            checkInt(TenetLinuxBinding.epollCtl(epfd, Constants.EPOLL_CTL_DEL, fd, NativeUtil.NULL_POINTER), "epoll_ctl");
        }else {
            int target = ((to & OsNetworkLibrary.REGISTER_READ) != Constants.ZERO ? Constants.EPOLL_IN | Constants.EPOLL_RDHUP : Constants.ZERO) |
                    ((to & OsNetworkLibrary.REGISTER_WRITE) != Constants.ZERO ? Constants.EPOLL_OUT : Constants.ZERO);
            try(Arena arena = Arena.ofConfined()) {
                MemorySegment ev = arena.allocate(epollEventLayout);
                NativeUtil.setInt(ev, eventsOffset, target);
                NativeUtil.setInt(ev, dataOffset + fdOffset, fd);
                checkInt(TenetLinuxBinding.epollCtl(epfd, from == OsNetworkLibrary.REGISTER_NONE ? Constants.EPOLL_CTL_ADD : Constants.EPOLL_CTL_MOD, fd, ev), "epoll_ctl");
            }
        }
    }

    @Override
    public int muxWait(Mux mux, MemorySegment events, int maxEvents, Timeout timeout) {
        return TenetLinuxBinding.epollWait(mux.epfd(), events, maxEvents, timeout.val());
    }

    @Override
    public void masterWait(Socket serverSocket, MemorySegment events, int index) {
        int event = NativeUtil.getInt(events, index * eventSize + eventsOffset);
        int socket = NativeUtil.getInt(events, index * eventSize + dataOffset + fdOffset);
        if(socket != serverSocket.intValue() || (event & Constants.EPOLL_IN) == Constants.ZERO) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public long workerWait(MemorySegment buffer, MemorySegment events, int index) {
        int event = NativeUtil.getInt(events, index * eventSize + eventsOffset);
        int socket = NativeUtil.getInt(events, index * eventSize + dataOffset + fdOffset);
        if((event & (Constants.EPOLL_IN | Constants.EPOLL_HUP | Constants.EPOLL_RDHUP)) != Constants.ZERO) {
            return R + socket;
        }else if((event & Constants.EPOLL_OUT) != Constants.ZERO) {
            return W + socket;
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public short ipv4Port(MemorySegment addr) {
        return TenetLinuxBinding.ipv4Port(addr);
    }

    @Override
    public short ipv6Port(MemorySegment addr) {
        return TenetLinuxBinding.ipv6Port(addr);
    }

    @Override
    public int connect(Socket socket, MemorySegment sockAddr) {
        return TenetLinuxBinding.connect(socket.intValue(), sockAddr, (int) sockAddr.byteSize());
    }

    @Override
    public Socket accept(Socket socket, MemorySegment addr) {
        int fd = checkInt(TenetLinuxBinding.accept(socket.intValue(), addr, (int) addr.byteSize()), "accept");
        return new Socket(fd);
    }

    @Override
    public int getIpv4Address(MemorySegment clientAddr, MemorySegment address) {
        return TenetLinuxBinding.getIpv4Address(clientAddr, address, (int) address.byteSize());
    }

    @Override
    public int getIpv6Address(MemorySegment clientAddr, MemorySegment address) {
        return TenetLinuxBinding.getIpv6Address(clientAddr, address, (int) address.byteSize());
    }

    @Override
    public int recv(Socket socket, MemorySegment data, int len) {
        return TenetLinuxBinding.recv(socket.intValue(), data, len);
    }

    @Override
    public int send(Socket socket, MemorySegment data, int len) {
        return TenetLinuxBinding.send(socket.intValue(), data, len);
    }

    @Override
    public int getErrOpt(Socket socket) {
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment ptr = arena.allocate(ValueLayout.JAVA_INT, Integer.MIN_VALUE);
            checkInt(TenetLinuxBinding.getErrOpt(socket.intValue(), ptr), "get socket err opt");
            return NativeUtil.getInt(ptr, Constants.ZERO);
        }
    }

    @Override
    public void closeSocket(Socket socket) {
        checkInt(TenetLinuxBinding.close(socket.intValue()), "close socket");
    }

    @Override
    public void shutdownWrite(Socket socket) {
        checkInt(TenetLinuxBinding.shutdownWrite(socket.intValue()), "shutdown write");
    }

    @Override
    public int errno() {
        return TenetLinuxBinding.errno();
    }

    @Override
    public void exitMux(Mux mux) {
        checkInt(TenetLinuxBinding.close(mux.epfd()), "close epoll fd");
    }

    @Override
    public void exit() {
        // No action, epoll doesn't need external operations for clean up
    }
}
