package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.binding.TenetWindowsBinding;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 *  Native implementation under Windows, using wepoll
 */
public final class WindowsNetworkLibrary implements OsNetworkLibrary {
    private static final MemoryLayout epollDataLayout = MemoryLayout.unionLayout(
            ValueLayout.ADDRESS.withName("ptr"),
            ValueLayout.JAVA_INT.withName("fd"),
            ValueLayout.JAVA_INT.withName("u32"),
            ValueLayout.JAVA_LONG.withName("u64"),
            ValueLayout.JAVA_INT.withName("sock"),
            ValueLayout.ADDRESS.withName("hnd")
    );
    private static final MemoryLayout epollEventLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("events"),
            MemoryLayout.paddingLayout(4),
            epollDataLayout.withName("data")
    );
    private static final long eventSize = epollEventLayout.byteSize();
    private static final long eventsOffset = epollEventLayout.byteOffset(MemoryLayout.PathElement.groupElement("events"));
    private static final long dataOffset = epollEventLayout.byteOffset(MemoryLayout.PathElement.groupElement("data"));
    private static final long sockOffset = epollDataLayout.byteOffset(MemoryLayout.PathElement.groupElement("sock"));

    private final int connectBlockCode;
    private final int sendBlockCode;
    private final int interruptCode;
    private final long invalidSocket;
    private final int ipv4AddressLen;
    private final int ipv6AddressLen;
    private final int ipv4AddressSize;
    private final int ipv6AddressSize;

    private long checkSocket(long socket, String errMsg) {
        if(socket == invalidSocket) {
            throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to \{errMsg} with err code : \{errno()}");
        }
        return socket;
    }

    public WindowsNetworkLibrary() {
        connectBlockCode = TenetWindowsBinding.connectBlockCode();
        sendBlockCode = TenetWindowsBinding.sendBlockCode();
        interruptCode = TenetWindowsBinding.interruptCode();
        invalidSocket = TenetWindowsBinding.invalidSocket();
        ipv4AddressLen = TenetWindowsBinding.ipv4AddressLen();
        ipv6AddressLen = TenetWindowsBinding.ipv6AddressLen();
        ipv4AddressSize = TenetWindowsBinding.ipv4AddressSize();
        ipv6AddressSize = TenetWindowsBinding.ipv6AddressSize();
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
        return TenetWindowsBinding.setIpv4SockAddr(sockAddr, address, port);
    }

    @Override
    public int setIpv6SockAddr(MemorySegment sockAddr, MemorySegment address, short port) {
        return TenetWindowsBinding.setIpv6SockAddr(sockAddr, address, port);
    }

    @Override
    public Mux createMux() {
        MemorySegment winHandle = checkPtr(TenetWindowsBinding.epollCreate(), "wepoll");
        return Mux.win(winHandle);
    }

    @Override
    public MemoryLayout eventLayout() {
        return epollEventLayout;
    }

    @Override
    public Socket createIpv4Socket() {
        long socket = checkSocket(TenetWindowsBinding.ipv4SocketCreate(), "ipv4 socket create");
        return new Socket(socket);
    }

    @Override
    public Socket createIpv6Socket() {
        long socket = checkSocket(TenetWindowsBinding.ipv6SocketCreate(), "ipv6 socket create");
        return new Socket(socket);
    }

    @Override
    public void setReuseAddr(Socket socket, boolean b) {
        checkInt(TenetWindowsBinding.setReuseAddr(socket.longValue(), b ? 1 : 0), "set SO_REUSE_ADDR");
    }

    @Override
    public void setKeepAlive(Socket socket, boolean b) {
        checkInt(TenetWindowsBinding.setKeepAlive(socket.longValue(), b ? 1 : 0), "set SO_KEEPALIVE");
    }

    @Override
    public void setTcpNoDelay(Socket socket, boolean b) {
        checkInt(TenetWindowsBinding.setTcpNoDelay(socket.longValue(), b ? 1 : 0), "set TCP_NODELAY");
    }

    @Override
    public void setIpv6Only(Socket socket, boolean b) {
        checkInt(TenetWindowsBinding.setIpv6Only(socket.longValue(), b ? 1 : 0), "set IPv6 only");
    }

    @Override
    public void setNonBlocking(Socket socket) {
        checkInt(TenetWindowsBinding.setNonBlocking(socket.longValue()), "set NON_BLOCKING");
    }

    @Override
    public int bind(Socket socket, MemorySegment addr) {
        return TenetWindowsBinding.bind(socket.longValue(), addr, (int) addr.byteSize());
    }

    @Override
    public int listen(Socket socket, int backlog) {
        return TenetWindowsBinding.listen(socket.longValue(), backlog);
    }

    @Override
    public void ctl(Mux mux, Socket socket, int from, int to) {
        if(from == to) {
            return ;
        }
        MemorySegment winHandle = mux.winHandle();
        long fd = socket.longValue();
        if(to == OsNetworkLibrary.REGISTER_NONE) {
            checkInt(TenetWindowsBinding.epollCtl(winHandle, Constants.EPOLL_CTL_DEL, fd, NativeUtil.NULL_POINTER), "epollCtl");
        }else {
            int target = ((to & OsNetworkLibrary.REGISTER_READ) != 0 ? Constants.EPOLL_IN | Constants.EPOLL_RDHUP : 0) |
                    ((to & OsNetworkLibrary.REGISTER_WRITE) != 0 ? Constants.EPOLL_OUT : 0);
            try(Arena arena = Arena.ofConfined()) {
                MemorySegment ev = arena.allocate(epollEventLayout);
                NativeUtil.setInt(ev, eventsOffset, target);
                NativeUtil.setLong(ev, dataOffset + sockOffset, fd);
                checkInt(TenetWindowsBinding.epollCtl(winHandle, from == OsNetworkLibrary.REGISTER_NONE ? Constants.EPOLL_CTL_ADD : Constants.EPOLL_CTL_MOD, fd, ev), "epollCtl");
            }
        }
    }

    @Override
    public int muxWait(Mux mux, MemorySegment events, int maxEvents, Timeout timeout) {
        return TenetWindowsBinding.epollWait(mux.winHandle(), events, maxEvents, timeout.val());
    }

    @Override
    public void masterWait(Socket serverSocket, MemorySegment events, int index) {
        int event = NativeUtil.getInt(events, index * eventSize + eventsOffset);
        long socket = NativeUtil.getLong(events, index * eventSize + dataOffset + sockOffset);
        if(socket != serverSocket.longValue() || (event & Constants.EPOLL_IN) == 0) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public long workerWait(MemorySegment buffer, MemorySegment events, int index) {
        int event = NativeUtil.getInt(events, index * eventSize + eventsOffset);
        long socket = NativeUtil.getLong(events, index * eventSize + dataOffset + sockOffset);
        if((event & (Constants.EPOLL_IN | Constants.EPOLL_HUP | Constants.EPOLL_RDHUP)) != 0) {
            return R + socket;
        }else if((event & Constants.EPOLL_OUT) != 0) {
            return W + socket;
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public short ipv4Port(MemorySegment addr) {
        return TenetWindowsBinding.ipv4Port(addr);
    }

    @Override
    public short ipv6Port(MemorySegment addr) {
        return TenetWindowsBinding.ipv6Port(addr);
    }

    @Override
    public int connect(Socket socket, MemorySegment sockAddr) {
        return TenetWindowsBinding.connect(socket.longValue(), sockAddr, (int) sockAddr.byteSize());
    }

    @Override
    public Socket accept(Socket socket, MemorySegment addr) {
        long socketFd = checkSocket(TenetWindowsBinding.accept(socket.longValue(), addr, (int) addr.byteSize()), "accept");
        return new Socket(socketFd);
    }

    @Override
    public int getIpv4Address(MemorySegment clientAddr, MemorySegment address) {
        return TenetWindowsBinding.getIpv4Address(clientAddr, address, (int) address.byteSize());
    }

    @Override
    public int getIpv6Address(MemorySegment clientAddr, MemorySegment address) {
        return TenetWindowsBinding.getIpv6Address(clientAddr, address, (int) address.byteSize());
    }

    @Override
    public int recv(Socket socket, MemorySegment data, int len) {
        return TenetWindowsBinding.recv(socket.longValue(), data, len);
    }

    @Override
    public int send(Socket socket, MemorySegment data, int len) {
        return TenetWindowsBinding.send(socket.longValue(), data, len);
    }

    @Override
    public int getErrOpt(Socket socket) {
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment ptr = arena.allocate(ValueLayout.JAVA_INT, Integer.MIN_VALUE);
            checkInt(TenetWindowsBinding.getErrOpt(socket.longValue(), ptr), "get socket err opt");
            return NativeUtil.getInt(ptr, 0);
        }
    }

    @Override
    public void closeSocket(Socket socket) {
        checkInt(TenetWindowsBinding.closeSocket(socket.longValue()), "close socket");
    }

    @Override
    public void shutdownWrite(Socket socket) {
        checkInt(TenetWindowsBinding.shutdownWrite(socket.longValue()), "shutdown write");
    }

    @Override
    public int errno() {
        return TenetWindowsBinding.wsaGetLastError();
    }

    @Override
    public void exitMux(Mux mux) {
        checkInt(TenetWindowsBinding.epollClose(mux.winHandle()), "close wepoll fd");
    }

    @Override
    public void exit() {
        checkInt(TenetWindowsBinding.wsaCleanUp(), "wsa_clean_up");
    }
}
