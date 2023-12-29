package cn.zorcc.common.network.lib;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.bindings.TenetMacosBinding;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.Mux;
import cn.zorcc.common.network.Socket;
import cn.zorcc.common.network.Timeout;
import cn.zorcc.common.structure.IntPair;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 *   Native implementation under macOS, using kqueue
 *   Note that the .dylib library is only suitable for ARM-based chips since I only tested on M1 series MacBook
 *   If developer needs to run it on X86 processors, recompile a new .dylib would work, it should be working on freebsd or openbsd operating system too since they are quite similar to macOS
 */
public final class MacOSNetworkLibrary implements OsNetworkLibrary {
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
    private static final long flagsOffset = keventLayout.byteOffset(MemoryLayout.PathElement.groupElement("flags"));

    private final int connectBlockCode;
    private final int sendBlockCode;
    private final int interruptCode;
    private final int ipv4AddressLen;
    private final int ipv6AddressLen;
    private final int ipv4AddressSize;
    private final int ipv6AddressSize;

    public MacOSNetworkLibrary() {
        connectBlockCode = TenetMacosBinding.connectBlockCode();
        sendBlockCode = TenetMacosBinding.sendBlockCode();
        interruptCode = TenetMacosBinding.interruptCode();
        ipv4AddressLen = TenetMacosBinding.ipv4AddressLen();
        ipv6AddressLen = TenetMacosBinding.ipv6AddressLen();
        ipv4AddressSize = TenetMacosBinding.ipv4AddressSize();
        ipv6AddressSize = TenetMacosBinding.ipv6AddressSize();
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
        return TenetMacosBinding.setIpv4SockAddr(sockAddr, address, port);
    }

    @Override
    public int setIpv6SockAddr(MemorySegment sockAddr, MemorySegment address, short port) {
        return TenetMacosBinding.setIpv6SockAddr(sockAddr, address, port);
    }

    @Override
    public Mux createMux() {
        int kqfd = check(TenetMacosBinding.kqueue(), "kqueue create");
        return Mux.mac(kqfd);
    }

    @Override
    public MemoryLayout eventLayout() {
        return keventLayout;
    }

    @Override
    public Socket createIpv4Socket() {
        int fd = check(TenetMacosBinding.ipv4SocketCreate(), "ipv4 socket create");
        return new Socket(fd);
    }

    @Override
    public Socket createIpv6Socket() {
        int fd = check(TenetMacosBinding.ipv6SocketCreate(), "ipv6 socket create");
        return new Socket(fd);
    }

    @Override
    public int setReuseAddr(Socket socket, boolean b) {
        return TenetMacosBinding.setReuseAddr(socket.intValue(), b ? 1 : 0);
    }

    @Override
    public int setKeepAlive(Socket socket, boolean b) {
        return TenetMacosBinding.setKeepAlive(socket.intValue(), b ? 1 : 0);
    }

    @Override
    public int setTcpNoDelay(Socket socket, boolean b) {
        return TenetMacosBinding.setTcpNoDelay(socket.intValue(), b ? 1 : 0);
    }

    @Override
    public int setIpv6Only(Socket socket, boolean b) {
        return TenetMacosBinding.setIpv6Only(socket.intValue(), b ? 1 : 0);
    }

    @Override
    public int setNonBlocking(Socket socket) {
        return TenetMacosBinding.setNonBlocking(socket.intValue());
    }

    @Override
    public int bind(Socket socket, MemorySegment addr) {
        return TenetMacosBinding.bind(socket.intValue(), addr, (int) addr.byteSize());
    }

    @Override
    public int listen(Socket socket, int backlog) {
        return TenetMacosBinding.listen(socket.intValue(), backlog);
    }

    @Override
    public int ctl(Mux mux, Socket socket, int from, int to) {
        if(from == to) {
            return 0;
        }
        int kqfd = mux.kqfd();
        long fd = socket.longValue();
        int index = 0;
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment ptr = arena.allocate(MemoryLayout.sequenceLayout(2, keventLayout));
            int r1 = from & Constants.NET_R, r2 = to & Constants.NET_R;
            if(r1 != r2) {
                NativeUtil.setLong(ptr, identOffset, fd);
                NativeUtil.setShort(ptr, filterOffset, Constants.EVFILT_READ);
                NativeUtil.setShort(ptr, flagsOffset, r1 > r2 ? Constants.EV_DELETE : Constants.EV_ADD);
                index++;
            }
            int w1 = from & Constants.NET_W, w2 = to & Constants.NET_W;
            if(w1 != w2) {
                NativeUtil.setLong(ptr, index * keventSize + identOffset, fd);
                NativeUtil.setShort(ptr, index * keventSize + filterOffset, Constants.EVFILT_WRITE);
                NativeUtil.setShort(ptr, index * keventSize + flagsOffset, w1 > w2 ? Constants.EV_DELETE : Constants.EV_ADD);
                index++;
            }
            return TenetMacosBinding.keventCtl(kqfd, ptr, index);
        }

    }

    @Override
    public int muxWait(Mux mux, MemorySegment events, int maxEvents, Timeout timeout) {
        return TenetMacosBinding.keventWait(mux.kqfd(), events, maxEvents, timeout.ptr());
    }

    @Override
    public IntPair access(MemorySegment events, int index) {
        short filter = NativeUtil.getShort(events, index * keventSize + filterOffset);
        long socket = NativeUtil.getLong(events, index * keventSize + identOffset);
        if(filter == Constants.EVFILT_READ) {
            return new IntPair((int) socket, Constants.NET_R);
        }else if(filter == Constants.EVFILT_WRITE) {
            return new IntPair((int) socket, Constants.NET_W);
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public short getIpv4Port(MemorySegment addr) {
        return TenetMacosBinding.ipv4Port(addr);
    }

    @Override
    public short getIpv6Port(MemorySegment addr) {
        return TenetMacosBinding.ipv6Port(addr);
    }

    @Override
    public int connect(Socket socket, MemorySegment sockAddr) {
        return TenetMacosBinding.connect(socket.intValue(), sockAddr, (int) sockAddr.byteSize());
    }

    @Override
    public Socket accept(Socket socket, MemorySegment addr) {
        int fd = check(TenetMacosBinding.accept(socket.intValue(), addr, (int) addr.byteSize()), "accept");
        return new Socket(fd);
    }

    @Override
    public int getIpv4Address(MemorySegment clientAddr, MemorySegment address) {
        return TenetMacosBinding.getIpv4Address(clientAddr, address, (int) address.byteSize());
    }

    @Override
    public int getIpv6Address(MemorySegment clientAddr, MemorySegment address) {
        return TenetMacosBinding.getIpv6Address(clientAddr, address, (int) address.byteSize());
    }

    @Override
    public int recv(Socket socket, MemorySegment data, int len) {
        return TenetMacosBinding.recv(socket.intValue(), data, len);
    }

    @Override
    public int send(Socket socket, MemorySegment data, int len) {
        return TenetMacosBinding.send(socket.intValue(), data, len);
    }

    @Override
    public int getErrOpt(Socket socket) {
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment ptr = arena.allocate(ValueLayout.JAVA_INT, Integer.MIN_VALUE);
            check(TenetMacosBinding.getErrOpt(socket.intValue(), ptr), "get socket err opt");
            return NativeUtil.getInt(ptr, 0);
        }
    }

    @Override
    public int shutdownWrite(Socket socket) {
        return TenetMacosBinding.shutdownWrite(socket.intValue());
    }

    @Override
    public int closeSocket(Socket socket) {
        return TenetMacosBinding.close(socket.intValue());
    }

    @Override
    public int closeMux(Mux mux) {
        return TenetMacosBinding.close(mux.kqfd());
    }

    @Override
    public void exit() {
        // No action, kqueue doesn't need external operations for clean up
    }
}
