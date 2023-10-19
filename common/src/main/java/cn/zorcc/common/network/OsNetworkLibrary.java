package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.structure.IpType;
import cn.zorcc.common.structure.Loc;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 *   Platform independent native interface for Network operation
 */
public sealed interface OsNetworkLibrary permits WindowsNetworkLibrary, LinuxNetworkLibrary, MacOSNetworkLibrary {
    int connectBlockCode();
    int sendBlockCode();
    int interruptCode();
    int ipv4AddressLen();
    int ipv6AddressLen();
    int ipv4AddressSize();
    int ipv6AddressSize();
    int setIpv4SockAddr(MemorySegment sockAddr, MemorySegment address, short port);
    int setIpv6SockAddr(MemorySegment sockAddr, MemorySegment address, short port);
    Mux createMux();
    MemoryLayout eventLayout();
    Socket createIpv4Socket();
    Socket createIpv6Socket();
    void setReuseAddr(Socket socket, boolean b);
    void setKeepAlive(Socket socket, boolean b);
    void setTcpNoDelay(Socket socket, boolean b);
    void setIpv6Only(Socket socket, boolean b);
    void setNonBlocking(Socket socket);
    int bind(Socket socket, MemorySegment addr);
    int listen(Socket socket, int backlog);
    void ctl(Mux mux, Socket socket, int from, int to);
    int muxWait(Mux mux, MemorySegment events, int maxEvents, Timeout timeout);
    void masterWait(Socket serverSocket, MemorySegment events, int index);
    long workerWait(MemorySegment buffer, MemorySegment events, int index);
    short ipv4Port(MemorySegment addr);
    short ipv6Port(MemorySegment addr);
    int connect(Socket socket, MemorySegment sockAddr);
    Socket accept(Socket socket, MemorySegment addr);
    int getIpv4Address(MemorySegment clientAddr, MemorySegment address);
    int getIpv6Address(MemorySegment clientAddr, MemorySegment address);
    int recv(Socket socket, MemorySegment data, int len);
    int send(Socket socket, MemorySegment data, int len);
    int getErrOpt(Socket socket);
    void closeSocket(Socket socket);
    void shutdownWrite(Socket socket);
    int errno();
    void exitMux(Mux mux);
    void exit();

    long R = 1L << 40;
    long W = 1L << 50;
    int REGISTER_NONE = 0;
    int REGISTER_READ = 1;
    int REGISTER_WRITE = 2;
    int REGISTER_READ_WRITE = 3;

    default int checkInt(int value, String errMsg) {
        if(value == -1) {
            throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to \{errMsg} with err code : \{errno()}");
        }
        return value;
    }

    default MemorySegment checkPtr(MemorySegment ptr, String errMsg) {
        if(NativeUtil.checkNullPointer(ptr)) {
            throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to \{errMsg} with err code : \{errno()}");
        }
        return ptr;
    }

    default MemorySegment createSockAddr(Loc loc, Arena arena) {
        if(loc.ipType() == IpType.IPV4) {
            return createIpv4SockAddr(loc, arena);
        }else if(loc.ipType() == IpType.IPV6) {
            return createIpv6SockAddr(loc, arena);
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    private MemorySegment createIpv4SockAddr(Loc loc, Arena arena) {
        MemorySegment r = arena.allocate(ipv4AddressSize());
        MemorySegment ip = loc.ip() == null || loc.ip().isBlank() ? NativeUtil.NULL_POINTER : NativeUtil.allocateStr(arena, loc.ip(), ipv4AddressLen());
        if(checkInt(setIpv4SockAddr(r, ip, loc.shortPort()), "set ipv4 address") == Constants.ZERO) {
            throw new FrameworkException(ExceptionType.NETWORK, STR."Ipv4 address is not valid : \{loc.ip()}");
        }
        return r;
    }

    private MemorySegment createIpv6SockAddr(Loc loc, Arena arena) {
        MemorySegment r = arena.allocate(ipv6AddressSize());
        MemorySegment ip = loc.ip() == null || loc.ip().isBlank() ? NativeUtil.NULL_POINTER : NativeUtil.allocateStr(arena, loc.ip(), ipv6AddressLen());
        if(checkInt(setIpv6SockAddr(r, ip, loc.shortPort()), "set ipv6 address") == Constants.ZERO) {
            throw new FrameworkException(ExceptionType.NETWORK, STR."Ipv6 address is not valid : \{loc.ip()}");
        }
        return r;
    }

    default Socket createSocket(Loc loc) {
        return switch (loc.ipType()) {
            case IPV4 -> createIpv4Socket();
            case IPV6 -> createIpv6Socket();
            case null -> throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        };
    }

    default void configureClientSocket(Socket socket, SocketOptions socketOptions) {
        setKeepAlive(socket, socketOptions.isKeepAlive());
        setTcpNoDelay(socket, socketOptions.isTcpNoDelay());
        setNonBlocking(socket);
    }

    default void configureServerSocket(Socket socket, Loc loc, SocketOptions socketOptions) {
        setReuseAddr(socket, socketOptions.isReuseAddr());
        setKeepAlive(socket, socketOptions.isKeepAlive());
        setTcpNoDelay(socket, socketOptions.isTcpNoDelay());
        if(loc.ipType() == IpType.IPV6) {
            setIpv6Only(socket, socketOptions.isIpv6Only());
        }
        setNonBlocking(socket);
    }

    default void bindAndListen(Socket socket, Loc loc, int backlog) {
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment addr = createSockAddr(loc, arena);
            checkInt(bind(socket, addr), "bind");
            checkInt(listen(socket, backlog), "listen");
        }
    }

    default ClientSocket accept(MasterConfig config, Loc loc, Socket socket) {
        return switch (loc.ipType()) {
            case IPV4 -> acceptIpv4Connection(config, socket);
            case IPV6 -> acceptIpv6Connection(config, socket);
            case null -> throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        };
    }

    private ClientSocket acceptIpv6Connection(MasterConfig masterConfig, Socket socket) {
        final int ipv6AddressSize = ipv6AddressSize();
        final int ipv6AddressLen = ipv6AddressLen();
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment clientAddr = arena.allocate(ipv6AddressSize);
            MemorySegment address = arena.allocateArray(ValueLayout.JAVA_BYTE, ipv6AddressLen);
            Socket clientSocket = accept(socket, clientAddr);
            configureClientSocket(clientSocket, masterConfig.getSocketOptions());
            if(getIpv6Address(clientAddr, address) < Constants.ZERO) {
                throw new FrameworkException(ExceptionType.NETWORK, "Failed to get client socket's remote address, errno : %d".formatted(errno()));
            }
            String ip = NativeUtil.getStr(address, ipv6AddressLen);
            int port = Loc.toIntPort(ipv6Port(clientAddr));
            if(NativeUtil.isIpv4MappedIpv6Address(ip)) {
                return new ClientSocket(clientSocket, new Loc(IpType.IPV4, NativeUtil.toIpv4Address(ip), port));
            }else {
                return new ClientSocket(clientSocket, new Loc(IpType.IPV6, ip, port));
            }
        }
    }

    private ClientSocket acceptIpv4Connection(MasterConfig config, Socket socket) {
        final int ipv4AddressSize = ipv4AddressSize();
        final int ipv4AddressLen = ipv4AddressLen();
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment clientAddr = arena.allocate(ipv4AddressSize);
            MemorySegment address = arena.allocateArray(ValueLayout.JAVA_BYTE, ipv4AddressLen);
            Socket clientSocket = accept(socket, clientAddr);
            configureClientSocket(clientSocket, config.getSocketOptions());
            if(getIpv4Address(clientAddr, address) < Constants.ZERO) {
                throw new FrameworkException(ExceptionType.NETWORK, "Failed to get client socket's remote address, errno : %d".formatted(errno()));
            }
            String ip = NativeUtil.getStr(address, ipv4AddressLen);
            int port = Loc.toIntPort(ipv4Port(clientAddr));
            Loc clientLoc = new Loc(IpType.IPV4, ip, port);
            return new ClientSocket(clientSocket, clientLoc);
        }
    }

    OsNetworkLibrary CURRENT = switch (NativeUtil.ostype()) {
        case Windows -> new WindowsNetworkLibrary();
        case Linux -> new LinuxNetworkLibrary();
        case MacOS -> new MacOSNetworkLibrary();
        default -> throw new FrameworkException(ExceptionType.NETWORK, "Unsupported operating system");
    };
}
