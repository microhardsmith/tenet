package cn.zorcc.common.network.lib;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.*;
import cn.zorcc.common.structure.IntPair;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 *   Platform independent native interface for Network operation
 *   The error handling model in java native programming is troublesome, we want to avoid constantly throwing RuntimeExceptions since it has a great impact on performance,
 *   but we can't delegate everything, so the major idea is :
 *   1. Don't use exceptions as flow control in your protocol, unless it's unreached by default
 *   2. Throw exception when dealing with operating system to provide useful stacktrace
 *   3. Catch exception in the upper level and close the underlying channel to provide robustness
 */
public sealed interface OsNetworkLibrary permits WindowsNetworkLibrary, LinuxNetworkLibrary, MacOSNetworkLibrary {
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
     *   Modifying ipv4 sockAddr to target ip and port
     */
    int setIpv4SockAddr(MemorySegment sockAddr, MemorySegment address, short port);

    /**
     *   Modifying ipv6 sockAddr to target ip and port
     */
    int setIpv6SockAddr(MemorySegment sockAddr, MemorySegment address, short port);

    /**
     *   Create a multiplexing object corresponding to the target operating system
     */
    Mux createMux();

    /**
     *   Return the target multiplexing struct memoryLayout corresponding to the target operating system
     */
    MemoryLayout eventLayout();

    /**
     *   Create an ipv4 socket object
     */
    Socket createIpv4Socket();

    /**
     *   Create an ipv6 socket object
     */
    Socket createIpv6Socket();

    /**
     *   Set socket's SO_REUSE_ADDR option
     */
    void setReuseAddr(Socket socket, boolean b);

    /**
     *   Set socket's SO_KEEPALIVE option
     */
    void setKeepAlive(Socket socket, boolean b);


    /**
     *   Set socket's TCP_NODELAY option
     */
    void setTcpNoDelay(Socket socket, boolean b);

    /**
     *   Set socket's IPV6_V6ONLY option
     */
    void setIpv6Only(Socket socket, boolean b);

    /**
     *   Set socket's non-blocking option
     */
    void setNonBlocking(Socket socket);

    /**
     *   Bind a server socket to target address
     */
    int bind(Socket socket, MemorySegment addr);

    /**
     *   Let a server socket start listening
     */
    int listen(Socket socket, int backlog);

    /**
     *   Modifying the multiplexing wait status of the socket
     */
    void ctl(Mux mux, Socket socket, int from, int to);

    /**
     *   Start multiplexing waiting for events, return the event count that triggered
     */
    int muxWait(Mux mux, MemorySegment events, int maxEvents, Timeout timeout);

    /**
     *   For poller to access the events array, the first return value represents the socket, the second value represents the event type
     */
    IntPair access(MemorySegment events, int index);

    /**
     *   Retrieve an ipv4 port from the target addr
     */
    short ipv4Port(MemorySegment addr);

    /**
     *   Retrieve an ipv6 port from the target addr
     */
    short ipv6Port(MemorySegment addr);

    /**
     *   Connect to the remote sockAddr
     */
    int connect(Socket socket, MemorySegment sockAddr);

    /**
     *   Accept from a server socket
     */
    Socket accept(Socket socket, MemorySegment addr);

    /**
     *   Retrieve an ipv4 address string from the clientAddr
     */
    int getIpv4Address(MemorySegment clientAddr, MemorySegment address);

    /**
     *   Retrieve an ipv6 address string from the clientAddr
     */
    int getIpv6Address(MemorySegment clientAddr, MemorySegment address);

    /**
     *   Recv from target socket, len should be the exact byteSize of data, return the actual bytes received
     */
    int recv(Socket socket, MemorySegment data, int len);

    /**
     *   Send using target socket, len should be the exact byteSize of data, return the actual bytes sent
     */
    int send(Socket socket, MemorySegment data, int len);

    /**
     *   Retrieve the err-opt from the target socket
     */
    int getErrOpt(Socket socket);

    /**
     *   Shutdown the write side of a socket
     */
    int shutdownWrite(Socket socket);

    /**
     *   Close a socket object
     */
    int closeSocket(Socket socket);

    /**
     *   Return the errno of current thread
     */
    int errno();

    /**
     *   Exit a multiplexing object
     */
    void exitMux(Mux mux);

    /**
     *   Exit the whole application
     */
    void exit();

    /**
     *   Check the return value of a native function, throw an exception if it's -1
     */
    default int checkInt(int value, String errMsg) {
        if(value == -1) {
            throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to \{errMsg} with err code : \{errno()}");
        }
        return value;
    }

    /**
     *   Check the return value of a native function, throw an exception if it's NULL pointer
     */
    default MemorySegment checkPtr(MemorySegment ptr, String errMsg) {
        if(NativeUtil.checkNullPointer(ptr)) {
            throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to \{errMsg} with err code : \{errno()}");
        }
        return ptr;
    }

    /**
     *   Create a sockAddr memorySegment, could be IPV4 or IPV6
     */
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
        if(checkInt(setIpv4SockAddr(r, ip, loc.shortPort()), "set ipv4 address") == 0) {
            throw new FrameworkException(ExceptionType.NETWORK, STR."Ipv4 address is not valid : \{loc.ip()}");
        }
        return r;
    }

    private MemorySegment createIpv6SockAddr(Loc loc, Arena arena) {
        MemorySegment r = arena.allocate(ipv6AddressSize());
        MemorySegment ip = loc.ip() == null || loc.ip().isBlank() ? NativeUtil.NULL_POINTER : NativeUtil.allocateStr(arena, loc.ip(), ipv6AddressLen());
        if(checkInt(setIpv6SockAddr(r, ip, loc.shortPort()), "set ipv6 address") == 0) {
            throw new FrameworkException(ExceptionType.NETWORK, STR."Ipv6 address is not valid : \{loc.ip()}");
        }
        return r;
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
        setKeepAlive(socket, socketConfig.isKeepAlive());
        setTcpNoDelay(socket, socketConfig.isTcpNoDelay());
        setNonBlocking(socket);
    }

    /**
     *   Configure a socket for the server side
     */
    default void configureServerSocket(Socket socket, Loc loc, SocketConfig socketConfig) {
        setReuseAddr(socket, socketConfig.isReuseAddr());
        setKeepAlive(socket, socketConfig.isKeepAlive());
        setTcpNoDelay(socket, socketConfig.isTcpNoDelay());
        if(loc.ipType() == IpType.IPV6) {
            setIpv6Only(socket, socketConfig.isIpv6Only());
        }
        setNonBlocking(socket);
    }

    /**
     *   Let the server-side bind and listen
     */
    default void bindAndListen(Socket socket, Loc loc, int backlog) {
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment addr = createSockAddr(loc, arena);
            checkInt(bind(socket, addr), "bind");
            checkInt(listen(socket, backlog), "listen");
        }
    }

    /**
     *   Accept a connection, note that IPV6 is compatible with IPV4, so even if Loc is IPV6 based, it may also accept IPV4 connection
     */
    default SocketAndLoc accept(Loc loc, Socket socket, SocketConfig socketConfig) {
        return switch (loc.ipType()) {
            case IPV4 -> acceptIpv4Connection(socket, socketConfig);
            case IPV6 -> acceptIpv6Connection(socket, socketConfig);
            case null -> throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        };
    }

    private SocketAndLoc acceptIpv6Connection(Socket socket, SocketConfig socketConfig) {
        final int ipv6AddressSize = ipv6AddressSize();
        final int ipv6AddressLen = ipv6AddressLen();
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment clientAddr = arena.allocate(ipv6AddressSize);
            MemorySegment address = arena.allocateArray(ValueLayout.JAVA_BYTE, ipv6AddressLen);
            Socket clientSocket = accept(socket, clientAddr);
            configureClientSocket(clientSocket, socketConfig);
            if(getIpv6Address(clientAddr, address) < 0) {
                throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to get client's ipv6 address, errno : \{errno()}");
            }
            String ip = NativeUtil.getStr(address, ipv6AddressLen);
            int port = 0xFFFF & ipv6Port(clientAddr);
            if(NativeUtil.isIpv4MappedIpv6Address(ip)) {
                return new SocketAndLoc(clientSocket, new Loc(IpType.IPV4, NativeUtil.toIpv4Address(ip), port));
            }else {
                return new SocketAndLoc(clientSocket, new Loc(IpType.IPV6, ip, port));
            }
        }
    }

    private SocketAndLoc acceptIpv4Connection(Socket socket, SocketConfig socketConfig) {
        final int ipv4AddressSize = ipv4AddressSize();
        final int ipv4AddressLen = ipv4AddressLen();
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment clientAddr = arena.allocate(ipv4AddressSize);
            MemorySegment address = arena.allocateArray(ValueLayout.JAVA_BYTE, ipv4AddressLen);
            Socket clientSocket = accept(socket, clientAddr);
            configureClientSocket(clientSocket, socketConfig);
            if(getIpv4Address(clientAddr, address) < 0) {
                throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to get client's ipv4 address, errno : \{errno()}");
            }
            String ip = NativeUtil.getStr(address, ipv4AddressLen);
            int port = 0xFFFF & ipv4Port(clientAddr);
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
}
