package cn.zorcc.common.network.lib;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.bindings.TenetWindowsBinding;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.Mux;
import cn.zorcc.common.network.MuxEvent;
import cn.zorcc.common.network.Socket;
import cn.zorcc.common.network.Timeout;
import cn.zorcc.common.structure.Allocator;

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
    private final int ipv4AddressLen;
    private final int ipv6AddressLen;
    private final int ipv4AddressSize;
    private final int ipv6AddressSize;
    private final int ipv4AddressAlign;
    private final int ipv6AddressAlign;

    public WindowsNetworkLibrary() {
        connectBlockCode = TenetWindowsBinding.connectBlockCode();
        sendBlockCode = TenetWindowsBinding.sendBlockCode();
        interruptCode = TenetWindowsBinding.interruptCode();
        ipv4AddressLen = TenetWindowsBinding.ipv4AddressLen();
        ipv6AddressLen = TenetWindowsBinding.ipv6AddressLen();
        ipv4AddressSize = TenetWindowsBinding.ipv4AddressSize();
        ipv6AddressSize = TenetWindowsBinding.ipv6AddressSize();
        ipv4AddressAlign = TenetWindowsBinding.ipv4AddressAlign();
        ipv6AddressAlign = TenetWindowsBinding.ipv6AddressAlign();
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
    public int ipv4AddressAlign() {
        return ipv4AddressAlign;
    }

    @Override
    public int ipv6AddressAlign() {
        return ipv6AddressAlign;
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
        MemorySegment ptr = Allocator.HEAP.allocate(ValueLayout.ADDRESS);
        check(TenetWindowsBinding.epollCreate(ptr), "wepoll_create");
        MemorySegment winHandle = ptr.get(ValueLayout.ADDRESS, 0L);
        return Mux.win(winHandle);
    }

    @Override
    public MemoryLayout eventLayout() {
        return epollEventLayout;
    }

    @Override
    public Socket createIpv4Socket() {
        MemorySegment ptr = Allocator.HEAP.allocate(ValueLayout.JAVA_LONG);
        check(TenetWindowsBinding.ipv4SocketCreate(ptr), "ipv4 socket create");
        return new Socket(ptr.get(ValueLayout.JAVA_LONG_UNALIGNED, 0L));
    }

    @Override
    public Socket createIpv6Socket() {
        MemorySegment ptr = Allocator.HEAP.allocate(ValueLayout.JAVA_LONG);
        check(TenetWindowsBinding.ipv6SocketCreate(ptr), "ipv6 socket create");
        return new Socket(ptr.get(ValueLayout.JAVA_LONG_UNALIGNED, 0L));
    }

    @Override
    public int setReuseAddr(Socket socket, boolean b) {
        return TenetWindowsBinding.setReuseAddr(socket.longValue(), b ? 1 : 0);
    }

    @Override
    public int setKeepAlive(Socket socket, boolean b) {
        return TenetWindowsBinding.setKeepAlive(socket.longValue(), b ? 1 : 0);
    }

    @Override
    public int setTcpNoDelay(Socket socket, boolean b) {
        return TenetWindowsBinding.setTcpNoDelay(socket.longValue(), b ? 1 : 0);
    }

    @Override
    public int setIpv6Only(Socket socket, boolean b) {
        return TenetWindowsBinding.setIpv6Only(socket.longValue(), b ? 1 : 0);
    }

    @Override
    public int setNonBlocking(Socket socket) {
        return TenetWindowsBinding.setNonBlocking(socket.longValue());
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
    public int ctl(Mux mux, Socket socket, long from, long to) {
        if(from == to) {
            return 0;
        }
        MemorySegment winHandle = mux.winHandle();
        long fd = socket.longValue();
        if(to == Constants.NET_NONE) {
            return TenetWindowsBinding.epollCtl(winHandle, Constants.EPOLL_CTL_DEL, fd, MemorySegment.NULL);
        }else {
            int target = ((to & Constants.NET_R) != Constants.NET_NONE ? (Constants.EPOLL_IN | Constants.EPOLL_RDHUP) : 0) |
                    ((to & Constants.NET_W) != Constants.NET_NONE ? Constants.EPOLL_OUT : 0);
            MemorySegment ev = Allocator.HEAP.allocate(epollEventLayout);
            ev.set(ValueLayout.JAVA_INT_UNALIGNED, eventsOffset, target);
            ev.set(ValueLayout.JAVA_LONG_UNALIGNED, dataOffset + sockOffset, fd);
            return TenetWindowsBinding.epollCtl(winHandle, from == Constants.NET_NONE ? Constants.EPOLL_CTL_ADD : Constants.EPOLL_CTL_MOD, fd, ev);
        }
    }

    @Override
    public int muxWait(Mux mux, MemorySegment events, int maxEvents, Timeout timeout) {
        return TenetWindowsBinding.epollWait(mux.winHandle(), events, maxEvents, timeout.val());
    }

    @Override
    public MuxEvent access(MemorySegment events, int index) {
        int event = events.get(ValueLayout.JAVA_INT_UNALIGNED, index * eventSize + eventsOffset);
        int socket = Math.toIntExact(events.get(ValueLayout.JAVA_LONG_UNALIGNED, index * eventSize + dataOffset + sockOffset));
        if((event & (Constants.EPOLL_IN | Constants.EPOLL_RDHUP)) != 0) {
            return new MuxEvent(socket, Constants.NET_R);
        }else if((event & Constants.EPOLL_OUT) != 0) {
            return new MuxEvent(socket, Constants.NET_W);
        }else if((event & (Constants.EPOLL_ERR | Constants.EPOLL_HUP)) != 0) {
            return new MuxEvent(socket, Constants.NET_OTHER);
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public short getIpv4Port(MemorySegment addr) {
        return TenetWindowsBinding.ipv4Port(addr);
    }

    @Override
    public short getIpv6Port(MemorySegment addr) {
        return TenetWindowsBinding.ipv6Port(addr);
    }

    @Override
    public int connect(Socket socket, MemorySegment sockAddr) {
        return TenetWindowsBinding.connect(socket.longValue(), sockAddr, (int) sockAddr.byteSize());
    }

    @Override
    public Socket accept(Socket socket, MemorySegment addr) {
        MemorySegment ptr = Allocator.HEAP.allocate(ValueLayout.JAVA_LONG);
        check(TenetWindowsBinding.accept(socket.longValue(), ptr, addr, (int) addr.byteSize()), "accept");
        long socketFd = ptr.get(ValueLayout.JAVA_LONG_UNALIGNED, 0L);
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
    public long recv(Socket socket, MemorySegment data, long len) {
        return TenetWindowsBinding.recv(socket.longValue(), data, Math.toIntExact(len));
    }

    @Override
    public long send(Socket socket, MemorySegment data, long len) {
        return TenetWindowsBinding.send(socket.longValue(), data, Math.toIntExact(len));
    }

    @Override
    public int getErrOpt(Socket socket) {
        MemorySegment ptr = Allocator.HEAP.allocate(ValueLayout.JAVA_INT);
        ptr.set(ValueLayout.JAVA_INT_UNALIGNED, 0L, -1);
        check(TenetWindowsBinding.getErrOpt(socket.longValue(), ptr), "get socket err opt");
        return ptr.get(ValueLayout.JAVA_INT_UNALIGNED, 0);
    }

    @Override
    public int shutdownWrite(Socket socket) {
        return TenetWindowsBinding.shutdownWrite(socket.longValue());
    }

    @Override
    public int closeSocket(Socket socket) {
        return TenetWindowsBinding.closeSocket(socket.longValue());
    }

    @Override
    public int closeMux(Mux mux) {
        return TenetWindowsBinding.epollClose(mux.winHandle());
    }

    @Override
    public void exit() {
        check(TenetWindowsBinding.wsaCleanUp(), "wsa_clean_up");
    }
}
