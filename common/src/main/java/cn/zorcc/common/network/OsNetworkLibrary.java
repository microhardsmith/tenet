package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.bindings.TenetLinuxBinding;
import cn.zorcc.common.bindings.TenetMacosBinding;
import cn.zorcc.common.bindings.TenetWindowsBinding;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.structure.Allocator;
import cn.zorcc.common.structure.MemApi;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 *   Platform independent native interface for Network operation
 *
 */
@SuppressWarnings("Duplicates")
public sealed interface OsNetworkLibrary permits OsNetworkLibrary.WindowsNetworkLibrary, OsNetworkLibrary.LinuxNetworkLibrary, OsNetworkLibrary.MacOSNetworkLibrary {

    /**
     *   Return the default connect block errno of the underlying operating system
     */
    int connectBlockCode();

    /**
     *   Return the default send block errno of the underlying operating system
     */
    int sendBlockCode();

    /**
     *   Return the default interrupt errno of the underlying operating system
     */
    int interruptCode();

    /**
     *   Return the default ipv4 address length of the underlying operating system
     */
    int ipv4AddressLen();

    /**
     *   Return the default ipv6 address length of the underlying operating system
     */
    int ipv6AddressLen();

    /**
     *   Return the default ipv4 address struct byteSize of the underlying operating system
     */
    int ipv4AddressSize();

    /**
     *   Return the default ipv6 address struct byteSize of the underlying operating system
     */
    int ipv6AddressSize();

    /**
     *   Return the default ipv4 address struct align of the underlying operating system
     */
    int ipv4AddressAlign();

    /**
     *   Return the default ipv6 address struct align of the underlying operating system
     */
    int ipv6AddressAlign();

    /**
     *   Create a multiplexing object corresponding to the target operating system
     *   Using system default malloc
     */
    Mux createMux();

    /**
     *   Return the target multiplexing struct memoryLayout corresponding to the target operating system
     */
    MemoryLayout eventLayout();

    /**
     *   Modifying the multiplexing wait status of the socket, using target MemApi
     */
    int ctl(Mux mux, Socket socket, int from, int to, MemApi memApi);

    /**
     *   Start multiplexing waiting for events, return the event count that triggered
     */
    int wait(Mux mux, MemorySegment events, int maxEvents, int timeout);

    /**
     *   For poller to access the events array, the first return value represents the socket, the second value represents the event type
     */
    MuxEvent access(MemorySegment events, int index);

    /**
     *   Exit a multiplexing object
     */
    int closeMux(Mux mux);

    /**
     *   Modifying ipv4 sockAddr to target ip and port
     */
    int setIpv4SockAddr(MemorySegment sockAddr, MemorySegment address, short port);

    /**
     *   Modifying ipv6 sockAddr to target ip and port
     */
    int setIpv6SockAddr(MemorySegment sockAddr, MemorySegment address, short port);

    /**
     *   Create an ipv4 socket object
     *   Using system default malloc
     */
    Socket createIpv4Socket();

    /**
     *   Create an ipv6 socket object
     *   Using system default malloc
     */
    Socket createIpv6Socket();

    /**
     *   Set socket's SO_REUSE_ADDR option
     */
    int setReuseAddr(Socket socket, boolean b);

    /**
     *   Set socket's SO_KEEPALIVE option
     */
    int setKeepAlive(Socket socket, boolean b);

    /**
     *   Set socket's TCP_NODELAY option
     */
    int setTcpNoDelay(Socket socket, boolean b);

    /**
     *   Set socket's IPV6_V6ONLY option
     */
    int setIpv6Only(Socket socket, boolean b);

    /**
     *   Set socket's non-blocking option
     */
    int setNonBlocking(Socket socket);

    /**
     *   Retrieve an ipv4 port from the target addr
     */
    short getIpv4Port(MemorySegment addr);

    /**
     *   Retrieve an ipv6 port from the target addr
     */
    short getIpv6Port(MemorySegment addr);

    /**
     *   Retrieve an ipv4 address string from the clientAddr
     */
    int getIpv4Address(MemorySegment clientAddr, MemorySegment address);

    /**
     *   Retrieve an ipv6 address string from the clientAddr
     */
    int getIpv6Address(MemorySegment clientAddr, MemorySegment address);

    /**
     *   Bind a server socket to target address
     */
    int bind(Socket socket, MemorySegment addr);

    /**
     *   Let a server socket start listening
     */
    int listen(Socket socket, int backlog);

    /**
     *   Connect to the remote sockAddr
     */
    int connect(Socket socket, MemorySegment sockAddr);

    /**
     *   Accept from a server socket, using target memApi
     */
    Socket accept(Socket socket, MemorySegment addr, MemApi memApi);

    /**
     *   Recv from target socket, len should be the exact byteSize of data, return the actual bytes received
     */
    long recv(Socket socket, MemorySegment data, long len);

    /**
     *   Send using target socket, len should be the exact byteSize of data, return the actual bytes sent
     */
    long send(Socket socket, MemorySegment data, long len);

    /**
     *   Retrieve the err-opt from the target socket
     */
    int getErrOpt(Socket socket, MemApi memApi);

    /**
     *   Shutdown the write side of a socket
     */
    int shutdownWrite(Socket socket);

    /**
     *   Close a socket object
     */
    int closeSocket(Socket socket);

    /**
     *   Exit the whole application
     */
    void exit();

    /**
     *   Check the return value of a native function, the errno will be represented as a negative form to avoid conflict
     */
    default int check(int value, String errMsg) {
        if(value < 0) {
            throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to \{errMsg} with err code : \{Math.abs(value)}");
        }
        return value;
    }

    /**
     *   Change the mux state
     */
    default void ctlMux(Mux mux, Socket socket, int from, int to, MemApi memApi) {
        check(ctl(mux, socket, from, to, memApi), "ctl mux");
    }

    /**
     *   Blocking wait the mux until event triggered
     */
    default int waitMux(Mux mux, MemorySegment events, int maxEvents, int timeout) {
        int r = wait(mux, events, maxEvents, timeout);
        if(r < 0) {
            int errno = Math.abs(r);
            if(errno == interruptCode()) {
                // If the current epoll_wait() were interrupted, we will do it again later
                return 0;
            }else {
                throw new FrameworkException(ExceptionType.NETWORK, STR."Mux wait failed with errno : \{errno}");
            }
        }else {
            return r;
        }
    }

    /**
     *   Exit and close the underlying mux
     */
    default void exitMux(Mux mux) {
        check(closeMux(mux), "close Mux");
    }

    /**
     *   Create a sockAddr memorySegment, could be IPV4 or IPV6
     *   Using system default malloc
     */
    default void useSockAddr(Loc loc, MemApi memApi, Consumer<MemorySegment> consumer) {
        if(loc.ipType() == IpType.IPV4) {
            createIpv4SockAddr(loc, memApi, consumer);
        }else if(loc.ipType() == IpType.IPV6) {
            createIpv6SockAddr(loc, memApi, consumer);
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    private void createIpv4SockAddr(Loc loc, MemApi memApi, Consumer<MemorySegment> consumer) {
        try(Allocator allocator = Allocator.newDirectAllocator(memApi)) {
            MemorySegment r = allocator.allocate(ipv4AddressSize(), ipv4AddressAlign());
            MemorySegment ip = loc.ip() == null || loc.ip().isBlank() ? MemorySegment.NULL : allocator.allocateFrom(loc.ip(), StandardCharsets.UTF_8);
            if(check(setIpv4SockAddr(r, ip, loc.shortPort()), "set ipv4 address") == 0) {
                throw new FrameworkException(ExceptionType.NETWORK, STR."Ipv4 address is not valid : \{loc.ip()}");
            }
            consumer.accept(r);
        }
    }

    private void createIpv6SockAddr(Loc loc, MemApi memApi, Consumer<MemorySegment> consumer) {
        try(Allocator allocator = Allocator.newDirectAllocator(memApi)) {
            MemorySegment r = allocator.allocate(ipv6AddressSize(), ipv6AddressAlign());
            MemorySegment ip = loc.ip() == null || loc.ip().isBlank() ? MemorySegment.NULL : allocator.allocateFrom(loc.ip(), StandardCharsets.UTF_8);
            if(check(setIpv6SockAddr(r, ip, loc.shortPort()), "set ipv6 address") == 0) {
                throw new FrameworkException(ExceptionType.NETWORK, STR."Ipv6 address is not valid : \{loc.ip()}");
            }
            consumer.accept(r);
        }
    }

    /**
     *   Create a socket object based on loc
     */
    default Socket createSocket(Loc loc) {
        return switch (loc.ipType()) {
            case IPV4 -> createIpv4Socket();
            case IPV6 -> createIpv6Socket();
            case null -> throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        };
    }

    /**
     *   Configure a socket for the client side
     */
    default void configureClientSocket(Socket socket, SocketConfig socketConfig) {
        check(setKeepAlive(socket, socketConfig.isKeepAlive()), "set client SO_REUSE_ADDR");
        check(setTcpNoDelay(socket, socketConfig.isTcpNoDelay()), "set client TCP_NODELAY");
        check(setNonBlocking(socket), "set client non-blocking");
    }

    /**
     *   Configure a socket for the server side
     */
    default void configureServerSocket(Socket socket, Loc loc, SocketConfig socketConfig) {
        check(setReuseAddr(socket, socketConfig.isReuseAddr()), "set server SO_REUSE_ADDR");
        check(setKeepAlive(socket, socketConfig.isKeepAlive()), "set server SO_KEEPALIVE");
        check(setTcpNoDelay(socket, socketConfig.isTcpNoDelay()), "set server TCP_NODELAY");
        if(loc.ipType() == IpType.IPV6) {
            check(setIpv6Only(socket, socketConfig.isIpv6Only()), "set server IPV6_V6ONLY");
        }
        check(setNonBlocking(socket), "set server non-blocking");
    }

    /**
     *   Let the server-side bind and listen
     *   Using system default allocator
     */
    default void bindAndListen(Socket socket, Loc loc, MemApi memApi, int backlog) {
        useSockAddr(loc, memApi, addr -> {
            check(bind(socket, addr), "bind");
            check(listen(socket, backlog), "listen");
        });
    }

    /**
     *   Accept a connection, note that IPV6 is compatible with IPV4, so even if Loc is IPV6 based, it may also accept IPV4 connection
     */
    default SocketAndLoc accept(Loc loc, Socket socket, SocketConfig socketConfig, MemApi memApi) {
        return switch (loc.ipType()) {
            case IPV4 -> acceptIpv4Connection(socket, socketConfig, memApi);
            case IPV6 -> acceptIpv6Connection(socket, socketConfig, memApi);
            case null -> throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        };
    }

    String IPV4_MAPPED_FORMAT = "::ffff:";
    int IPV4_PREFIX_LENGTH = IPV4_MAPPED_FORMAT.length();
    private SocketAndLoc acceptIpv6Connection(Socket socket, SocketConfig socketConfig, MemApi memApi) {
        try(Allocator allocator = Allocator.newDirectAllocator(memApi)) {
            MemorySegment clientAddr = allocator.allocate(ipv6AddressSize(), ipv6AddressAlign());
            MemorySegment address = allocator.allocate(ValueLayout.JAVA_BYTE, ipv6AddressLen());
            Socket clientSocket = accept(socket, clientAddr, memApi);
            configureClientSocket(clientSocket, socketConfig);
            check(getIpv6Address(clientAddr, address), "get client's ipv6 address");
            String ip = address.getString(0L, StandardCharsets.UTF_8);
            int port = 0xFFFF & getIpv6Port(clientAddr);
            if(ip.startsWith(IPV4_MAPPED_FORMAT)) {
                return new SocketAndLoc(clientSocket, new Loc(IpType.IPV4, ip.substring(IPV4_PREFIX_LENGTH), port));
            }else {
                return new SocketAndLoc(clientSocket, new Loc(IpType.IPV6, ip, port));
            }
        }
    }

    private SocketAndLoc acceptIpv4Connection(Socket socket, SocketConfig socketConfig, MemApi memApi) {
        try(Allocator allocator = Allocator.newDirectAllocator(memApi)){
            MemorySegment clientAddr = allocator.allocate(ipv4AddressSize(), ipv4AddressAlign());
            MemorySegment address = allocator.allocate(ValueLayout.JAVA_BYTE, ipv4AddressLen());
            Socket clientSocket = accept(socket, clientAddr, memApi);
            configureClientSocket(clientSocket, socketConfig);
            check(getIpv4Address(clientAddr, address), "get client's ipv4 address");
            String ip = address.getString(0L, StandardCharsets.UTF_8);
            int port = 0xFFFF & getIpv4Port(clientAddr);
            Loc clientLoc = new Loc(IpType.IPV4, ip, port);
            return new SocketAndLoc(clientSocket, clientLoc);
        }
    }

    OsNetworkLibrary CURRENT = switch (NativeUtil.ostype()) {
        case Windows -> new WindowsNetworkLibrary();
        case Linux -> new LinuxNetworkLibrary();
        case MacOS -> new MacOSNetworkLibrary();
        default -> throw new FrameworkException(ExceptionType.NETWORK, "Unsupported operating system");
    };

    /**
     *  Native implementation under Windows, using wepoll
     */
    final class WindowsNetworkLibrary implements OsNetworkLibrary {
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

        private static final int connectBlockCode = TenetWindowsBinding.connectBlockCode();
        private static final int sendBlockCode = TenetWindowsBinding.sendBlockCode();
        private static final int interruptCode = TenetWindowsBinding.interruptCode();
        private static final int ipv4AddressLen = TenetWindowsBinding.ipv4AddressLen();
        private static final int ipv6AddressLen = TenetWindowsBinding.ipv6AddressLen();
        private static final int ipv4AddressSize = TenetWindowsBinding.ipv4AddressSize();
        private static final int ipv6AddressSize = TenetWindowsBinding.ipv6AddressSize();
        private static final int ipv4AddressAlign = TenetWindowsBinding.ipv4AddressAlign();
        private static final int ipv6AddressAlign = TenetWindowsBinding.ipv6AddressAlign();

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
            try(Allocator allocator = Allocator.newDirectAllocator(MemApi.DEFAULT)) {
                MemorySegment ptr = allocator.allocate(ValueLayout.ADDRESS);
                check(TenetWindowsBinding.epollCreate(ptr), "wepoll_create");
                return Mux.win(NativeUtil.getAddress(ptr, 0L));
            }
        }

        @Override
        public MemoryLayout eventLayout() {
            return epollEventLayout;
        }

        @Override
        public Socket createIpv4Socket() {
            try(Allocator allocator = Allocator.newDirectAllocator(MemApi.DEFAULT)) {
                MemorySegment ptr = allocator.allocate(ValueLayout.JAVA_LONG);
                check(TenetWindowsBinding.ipv4SocketCreate(ptr), "ipv4 socket create");
                return Socket.ofLong(NativeUtil.getLong(ptr, 0L));
            }
        }

        @Override
        public Socket createIpv6Socket() {
            try(Allocator allocator = Allocator.newDirectAllocator(MemApi.DEFAULT)) {
                MemorySegment ptr = allocator.allocate(ValueLayout.JAVA_LONG);
                check(TenetWindowsBinding.ipv6SocketCreate(ptr), "ipv6 socket create");
                return Socket.ofLong(NativeUtil.getLong(ptr, 0L));
            }
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
        public int ctl(Mux mux, Socket socket, int from, int to, MemApi memApi) {
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
                try(Allocator allocator = Allocator.newDirectAllocator(memApi)) {
                    MemorySegment ev = allocator.allocate(epollEventLayout);
                    NativeUtil.setInt(ev, eventsOffset, target);
                    NativeUtil.setLong(ev, dataOffset + sockOffset, fd);
                    return TenetWindowsBinding.epollCtl(winHandle, from == Constants.NET_NONE ? Constants.EPOLL_CTL_ADD : Constants.EPOLL_CTL_MOD, fd, ev);
                }
            }
        }

        @Override
        public int wait(Mux mux, MemorySegment events, int maxEvents, int timeout) {
            return TenetWindowsBinding.epollWait(mux.winHandle(), events, maxEvents, timeout);
        }

        @Override
        public MuxEvent access(MemorySegment events, int index) {
            int event = NativeUtil.getInt(events, index * eventSize + eventsOffset);
            int socket = Math.toIntExact(NativeUtil.getLong(events, index * eventSize + dataOffset + sockOffset));
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
        public Socket accept(Socket socket, MemorySegment addr, MemApi memApi) {
            try(Allocator allocator = Allocator.newDirectAllocator(memApi)) {
                MemorySegment ptr = allocator.allocate(ValueLayout.JAVA_LONG);
                check(TenetWindowsBinding.accept(socket.longValue(), ptr, addr, (int) addr.byteSize()), "accept");
                long socketFd = NativeUtil.getLong(ptr, 0L);
                return Socket.ofLong(socketFd);
            }
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
        public int getErrOpt(Socket socket, MemApi memApi) {
            try(Allocator allocator = Allocator.newDirectAllocator(memApi)) {
                MemorySegment ptr = allocator.allocate(ValueLayout.JAVA_INT);
                NativeUtil.setInt(ptr, 0L, -1);
                check(TenetWindowsBinding.getErrOpt(socket.longValue(), ptr), "get socket err opt");
                return NativeUtil.getInt(ptr, 0L);
            }
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

    /**
     *   Native implementation under Linux, using epoll
     */
    final class LinuxNetworkLibrary implements OsNetworkLibrary {
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

        private static final int connectBlockCode = TenetLinuxBinding.connectBlockCode();
        private static final int sendBlockCode = TenetLinuxBinding.sendBlockCode();
        private static final int interruptCode = TenetLinuxBinding.interruptCode();
        private static final int ipv4AddressLen = TenetLinuxBinding.ipv4AddressLen();
        private static final int ipv6AddressLen = TenetLinuxBinding.ipv6AddressLen();
        private static final int ipv4AddressSize = TenetLinuxBinding.ipv4AddressSize();
        private static final int ipv6AddressSize = TenetLinuxBinding.ipv6AddressSize();
        private static final int ipv4AddressAlign = TenetLinuxBinding.ipv4AddressAlign();
        private static final int ipv6AddressAlign = TenetLinuxBinding.ipv6AddressAlign();

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
            return Socket.ofInt(fd);
        }

        @Override
        public Socket createIpv6Socket() {
            int fd = check(TenetLinuxBinding.ipv6SocketCreate(), "ipv6 socket create");
            return Socket.ofInt(fd);
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
        public int ctl(Mux mux, Socket socket, int from, int to, MemApi memApi) {
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
                try(Allocator allocator = Allocator.newDirectAllocator(memApi)) {
                    MemorySegment ev = allocator.allocate(epollEventLayout);
                    NativeUtil.setInt(ev, eventsOffset, target);
                    NativeUtil.setInt(ev, dataOffset + fdOffset, fd);
                    return TenetLinuxBinding.epollCtl(epfd, from == Constants.NET_NONE ? Constants.EPOLL_CTL_ADD : Constants.EPOLL_CTL_MOD, fd, ev);
                }
            }
        }

        @Override
        public int wait(Mux mux, MemorySegment events, int maxEvents, int timeout) {
            return TenetLinuxBinding.epollWait(mux.epfd(), events, maxEvents, timeout);
        }

        @Override
        public MuxEvent access(MemorySegment events, int index) {
            int event = NativeUtil.getInt(events, index * eventSize + eventsOffset);
            int socket = NativeUtil.getInt(events, index * eventSize + dataOffset + fdOffset);
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
        public Socket accept(Socket socket, MemorySegment addr, MemApi memApi) {
            int fd = check(TenetLinuxBinding.accept(socket.intValue(), addr, (int) addr.byteSize()), "accept");
            return Socket.ofInt(fd);
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
        public int getErrOpt(Socket socket, MemApi memApi) {
            try(Allocator allocator = Allocator.newDirectAllocator(memApi)) {
                MemorySegment ptr = allocator.allocate(ValueLayout.JAVA_INT);
                NativeUtil.setInt(ptr, 0L, -1);
                check(TenetLinuxBinding.getErrOpt(socket.intValue(), ptr), "get socket err opt");
                return NativeUtil.getInt(ptr, 0L);
            }
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

    /**
     *   Native implementation under macOS, using kqueue
     *   Note that the prebuilt .dylib library is only suitable for ARM-based chips since I only tested on M1 series MacBook
     *   If developer needs to run it on X86 processors, recompile a new .dylib would possibly work
     *   It should be working on freebsd or openbsd operating system too since they are quite similar to macOS, but further tests are needed
     */
    final class MacOSNetworkLibrary implements OsNetworkLibrary {
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

        private static final int connectBlockCode = TenetMacosBinding.connectBlockCode();
        private static final int sendBlockCode = TenetMacosBinding.sendBlockCode();
        private static final int interruptCode = TenetMacosBinding.interruptCode();
        private static final int ipv4AddressLen = TenetMacosBinding.ipv4AddressLen();
        private static final int ipv6AddressLen = TenetMacosBinding.ipv6AddressLen();
        private static final int ipv4AddressSize = TenetMacosBinding.ipv4AddressSize();
        private static final int ipv6AddressSize = TenetMacosBinding.ipv6AddressSize();
        private static final int ipv4AddressAlign = TenetMacosBinding.ipv4AddressAlign();
        private static final int ipv6AddressAlign = TenetMacosBinding.ipv6AddressAlign();

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
            return Socket.ofInt(fd);
        }

        @Override
        public Socket createIpv6Socket() {
            int fd = check(TenetMacosBinding.ipv6SocketCreate(), "ipv6 socket create");
            return Socket.ofInt(fd);
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
        public int ctl(Mux mux, Socket socket, int from, int to, MemApi memApi) {
            if(from == to) {
                return 0;
            }
            int kqfd = mux.kqfd();
            long fd = socket.longValue();
            int index = 0;
            try(Allocator allocator = Allocator.newDirectAllocator(memApi)) {
                MemorySegment ptr = allocator.allocate(MemoryLayout.sequenceLayout(2, keventLayout));
                int r1 = from & Constants.NET_R, r2 = to & Constants.NET_R;
                if(r1 != r2) {
                    NativeUtil.setLong(ptr, identOffset, fd);
                    NativeUtil.setShort(ptr, filterOffset, Constants.EVFILT_READ);
                    NativeUtil.setShort(ptr, flagsOffset, r1 == Constants.NET_NONE ? Constants.EV_ADD : Constants.EV_DELETE);
                    index++;
                }
                int w1 = from & Constants.NET_W, w2 = to & Constants.NET_W;
                if(w1 != w2) {
                    NativeUtil.setLong(ptr, index * keventSize + identOffset, fd);
                    NativeUtil.setShort(ptr, index * keventSize + filterOffset, Constants.EVFILT_WRITE);
                    NativeUtil.setShort(ptr, index * keventSize + flagsOffset, w1 == Constants.NET_NONE ? Constants.EV_ADD : Constants.EV_DELETE);
                    index++;
                }
                return TenetMacosBinding.keventCtl(kqfd, ptr, index);
            }
        }

        @Override
        public int wait(Mux mux, MemorySegment events, int maxEvents, int timeout) {
            return TenetMacosBinding.keventWait(mux.kqfd(), events, maxEvents, timeout);
        }

        @Override
        public MuxEvent access(MemorySegment events, int index) {
            short filter = NativeUtil.getShort(events, index * keventSize + filterOffset);
            int socket = Math.toIntExact(NativeUtil.getLong(events, index * keventSize + identOffset));
            if(filter == Constants.EVFILT_READ) {
                return new MuxEvent(socket, Constants.NET_R);
            }else if(filter == Constants.EVFILT_WRITE) {
                return new MuxEvent(socket, Constants.NET_W);
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
        public Socket accept(Socket socket, MemorySegment addr, MemApi memApi) {
            int fd = check(TenetMacosBinding.accept(socket.intValue(), addr, (int) addr.byteSize()), "accept");
            return Socket.ofInt(fd);
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
        public long recv(Socket socket, MemorySegment data, long len) {
            return TenetMacosBinding.recv(socket.intValue(), data, len);
        }

        @Override
        public long send(Socket socket, MemorySegment data, long len) {
            return TenetMacosBinding.send(socket.intValue(), data, len);
        }

        @Override
        public int getErrOpt(Socket socket, MemApi memApi) {
            try(Allocator allocator = Allocator.newDirectAllocator(memApi)) {
                MemorySegment ptr = allocator.allocate(ValueLayout.JAVA_INT);
                NativeUtil.setInt(ptr, 0L, -1);
                check(TenetMacosBinding.getErrOpt(socket.intValue(), ptr), "get socket err opt");
                return NativeUtil.getInt(ptr, 0L);
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
}
