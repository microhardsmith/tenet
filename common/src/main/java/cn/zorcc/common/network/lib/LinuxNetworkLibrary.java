package cn.zorcc.common.network.lib;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.bindings.TenetLinuxBinding;
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
    private final int ipv4AddressAlign;
    private final int ipv6AddressAlign;

    public LinuxNetworkLibrary() {
        connectBlockCode = TenetLinuxBinding.connectBlockCode();
        sendBlockCode = TenetLinuxBinding.sendBlockCode();
        interruptCode = TenetLinuxBinding.interruptCode();
        ipv4AddressLen = TenetLinuxBinding.ipv4AddressLen();
        ipv6AddressLen = TenetLinuxBinding.ipv6AddressLen();
        ipv4AddressSize = TenetLinuxBinding.ipv4AddressSize();
        ipv6AddressSize = TenetLinuxBinding.ipv6AddressSize();
        ipv4AddressAlign = TenetLinuxBinding.ipv4AddressAlign();
        ipv6AddressAlign = TenetLinuxBinding.ipv6AddressAlign();
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
        return TenetLinuxBinding.setIpv4SockAddr(sockAddr, address, port);
    }

    @Override
    public int setIpv6SockAddr(MemorySegment sockAddr, MemorySegment address, short port) {
        return TenetLinuxBinding.setIpv6SockAddr(sockAddr, address, port);
    }

    @Override
    public Mux createMux() {
        int epfd = check(TenetLinuxBinding.epollCreate(), "epoll create");
        return Mux.linux(epfd);
    }

    @Override
    public MemoryLayout eventLayout() {
        return epollEventLayout;
    }

    @Override
    public Socket createIpv4Socket() {
        int fd = check(TenetLinuxBinding.ipv4SocketCreate(), "ipv4 socket create");
        return new Socket(fd);
    }

    @Override
    public Socket createIpv6Socket() {
        int fd = check(TenetLinuxBinding.ipv6SocketCreate(), "ipv6 socket create");
        return new Socket(fd);
    }

    @Override
    public int setReuseAddr(Socket socket, boolean b) {
        return TenetLinuxBinding.setReuseAddr(socket.intValue(), b ? 1 : 0);
    }

    @Override
    public int setKeepAlive(Socket socket, boolean b) {
        return TenetLinuxBinding.setKeepAlive(socket.intValue(), b ? 1 : 0);
    }

    @Override
    public int setTcpNoDelay(Socket socket, boolean b) {
        return TenetLinuxBinding.setTcpNoDelay(socket.intValue(), b ? 1 : 0);
    }

    @Override
    public int setIpv6Only(Socket socket, boolean b) {
        return TenetLinuxBinding.setIpv6Only(socket.intValue(), b ? 1 : 0);
    }

    @Override
    public int setNonBlocking(Socket socket) {
        return TenetLinuxBinding.setNonBlocking(socket.intValue());
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
    public int ctl(Mux mux, Socket socket, long from, long to) {
        if(from == to) {
            return 0;
        }
        int epfd = mux.epfd();
        int fd = socket.intValue();
        if(to == Constants.NET_NONE) {
            return TenetLinuxBinding.epollCtl(epfd, Constants.EPOLL_CTL_DEL, fd, MemorySegment.NULL);
        }else {
            int target = ((to & Constants.NET_R) != Constants.NET_NONE ? (Constants.EPOLL_IN | Constants.EPOLL_RDHUP) : 0) |
                    ((to & Constants.NET_W) != Constants.NET_NONE ? Constants.EPOLL_OUT : 0);
            MemorySegment ev = Allocator.HEAP.allocate(epollEventLayout);
            ev.set(ValueLayout.JAVA_INT_UNALIGNED, eventsOffset, target);
            ev.set(ValueLayout.JAVA_INT_UNALIGNED, dataOffset + fdOffset, fd);
            return TenetLinuxBinding.epollCtl(epfd, from == Constants.NET_NONE ? Constants.EPOLL_CTL_ADD : Constants.EPOLL_CTL_MOD, fd, ev);
        }
    }

    @Override
    public int muxWait(Mux mux, MemorySegment events, int maxEvents, Timeout timeout) {
        return TenetLinuxBinding.epollWait(mux.epfd(), events, maxEvents, timeout.val());
    }

    @Override
    public MuxEvent access(MemorySegment events, int index) {
        int event = events.get(ValueLayout.JAVA_INT_UNALIGNED, index * eventSize + eventsOffset);
        int socket = events.get(ValueLayout.JAVA_INT_UNALIGNED, index * eventSize + dataOffset + fdOffset);
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
        return TenetLinuxBinding.ipv4Port(addr);
    }

    @Override
    public short getIpv6Port(MemorySegment addr) {
        return TenetLinuxBinding.ipv6Port(addr);
    }

    @Override
    public int connect(Socket socket, MemorySegment sockAddr) {
        return TenetLinuxBinding.connect(socket.intValue(), sockAddr, (int) sockAddr.byteSize());
    }

    @Override
    public Socket accept(Socket socket, MemorySegment addr) {
        int fd = check(TenetLinuxBinding.accept(socket.intValue(), addr, (int) addr.byteSize()), "accept");
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
    public long recv(Socket socket, MemorySegment data, long len) {
        return TenetLinuxBinding.recv(socket.intValue(), data, len);
    }

    @Override
    public long send(Socket socket, MemorySegment data, long len) {
        return TenetLinuxBinding.send(socket.intValue(), data, len);
    }

    @Override
    public int getErrOpt(Socket socket) {
        MemorySegment ptr = Allocator.HEAP.allocate(ValueLayout.JAVA_INT);
        ptr.set(ValueLayout.JAVA_INT_UNALIGNED, 0L, -1);
        check(TenetLinuxBinding.getErrOpt(socket.intValue(), ptr), "get socket err opt");
        return ptr.get(ValueLayout.JAVA_INT_UNALIGNED, 0L);
    }

    @Override
    public int shutdownWrite(Socket socket) {
        return TenetLinuxBinding.shutdownWrite(socket.intValue());
    }

    @Override
    public int closeSocket(Socket socket) {
        return TenetLinuxBinding.close(socket.intValue());
    }

    @Override
    public int closeMux(Mux mux) {
        return TenetLinuxBinding.close(mux.epfd());
    }

    @Override
    public void exit() {
        // No action, epoll doesn't need external operations for clean up
    }
}
