package cn.zorcc.common.network.lib;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.*;
import cn.zorcc.common.structure.Allocator;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 *   Platform independent native interface for Network operation
 *
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
     *   Return the default ipv4 address struct align of the underlying operating system
     */
    int ipv4AddressAlign();

    /**
     *   Return the default ipv6 address struct align of the underlying operating system
     */
    int ipv6AddressAlign();

    /**
     *   Create a multiplexing object corresponding to the target operating system
     */
    Mux createMux();

    /**
     *   Return the target multiplexing struct memoryLayout corresponding to the target operating system
     */
    MemoryLayout eventLayout();

    /**
     *   Modifying the multiplexing wait status of the socket
     */
    int ctl(Mux mux, Socket socket, long from, long to);

    /**
     *   Start multiplexing waiting for events, return the event count that triggered
     */
    int muxWait(Mux mux, MemorySegment events, int maxEvents, Timeout timeout);

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
     */
    Socket createIpv4Socket();

    /**
     *   Create an ipv6 socket object
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
     *   Accept from a server socket
     */
    Socket accept(Socket socket, MemorySegment addr);

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
     *   Exit the whole application
     */
    void exit();

    /**
     *   Check the return value of a native function, the errno will be represented as a negative form to avoid conflict
     */
    default int check(int value, String errMsg) {
        if(value < 0) {
            int errno = Math.abs(value);
            throw new FrameworkException(ExceptionType.NETWORK, STR."Failed to \{errMsg} with err code : \{errno}");
        }
        return value;
    }

    default void ctlMux(Mux mux, Socket socket, long from, long to) {
        check(ctl(mux, socket, from, to), "ctl mux");
    }

    default void exitMux(Mux mux) {
        check(closeMux(mux), "close Mux");
    }

    /**
     *   Create a sockAddr memorySegment, could be IPV4 or IPV6
     */
    default MemorySegment createSockAddr(Loc loc) {
        if(loc.ipType() == IpType.IPV4) {
            return createIpv4SockAddr(loc);
        }else if(loc.ipType() == IpType.IPV6) {
            return createIpv6SockAddr(loc);
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    private MemorySegment createIpv4SockAddr(Loc loc) {
        MemorySegment r = Allocator.HEAP.allocate(ipv4AddressSize(), ipv4AddressAlign());
        MemorySegment ip = loc.ip() == null || loc.ip().isBlank() ? MemorySegment.NULL : Allocator.HEAP.allocateFrom(loc.ip());
        if(check(setIpv4SockAddr(r, ip, loc.shortPort()), "set ipv4 address") == 0) {
            throw new FrameworkException(ExceptionType.NETWORK, STR."Ipv4 address is not valid : \{loc.ip()}");
        }
        return r;
    }

    private MemorySegment createIpv6SockAddr(Loc loc) {
        MemorySegment r = Allocator.HEAP.allocate(ipv6AddressSize(), ipv6AddressAlign());
        MemorySegment ip = loc.ip() == null || loc.ip().isBlank() ? MemorySegment.NULL : Allocator.HEAP.allocateFrom(loc.ip());
        if(check(setIpv6SockAddr(r, ip, loc.shortPort()), "set ipv6 address") == 0) {
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
     */
    default void bindAndListen(Socket socket, Loc loc, int backlog) {
        MemorySegment addr = createSockAddr(loc);
        check(bind(socket, addr), "bind");
        check(listen(socket, backlog), "listen");
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

    String IPV4_MAPPED_FORMAT = "::ffff:";
    int IPV4_PREFIX_LENGTH = IPV4_MAPPED_FORMAT.length();
    private SocketAndLoc acceptIpv6Connection(Socket socket, SocketConfig socketConfig) {
        MemorySegment clientAddr = Allocator.HEAP.allocate(ipv6AddressSize(), ipv6AddressAlign());
        MemorySegment address = Allocator.HEAP.allocate(ValueLayout.JAVA_BYTE, ipv6AddressLen());
        Socket clientSocket = accept(socket, clientAddr);
        configureClientSocket(clientSocket, socketConfig);
        check(getIpv6Address(clientAddr, address), "get client's ipv6 address");
        String ip = address.getString(0L);
        int port = 0xFFFF & getIpv6Port(clientAddr);
        if(ip.startsWith(IPV4_MAPPED_FORMAT)) {
            return new SocketAndLoc(clientSocket, new Loc(IpType.IPV4, ip.substring(IPV4_PREFIX_LENGTH), port));
        }else {
            return new SocketAndLoc(clientSocket, new Loc(IpType.IPV6, ip, port));
        }
    }

    private SocketAndLoc acceptIpv4Connection(Socket socket, SocketConfig socketConfig) {
        MemorySegment clientAddr = Allocator.HEAP.allocate(ipv4AddressSize(), ipv4AddressAlign());
        MemorySegment address = Allocator.HEAP.allocate(ValueLayout.JAVA_BYTE, ipv4AddressLen());
        Socket clientSocket = accept(socket, clientAddr);
        configureClientSocket(clientSocket, socketConfig);
        check(getIpv4Address(clientAddr, address), "get client's ipv4 address");
        String ip = address.getString(0L);
        int port = 0xFFFF & getIpv4Port(clientAddr);
        Loc clientLoc = new Loc(IpType.IPV4, ip, port);
        return new SocketAndLoc(clientSocket, clientLoc);
    }

    OsNetworkLibrary CURRENT = switch (NativeUtil.ostype()) {
        case Windows -> new WindowsNetworkLibrary();
        case Linux -> new LinuxNetworkLibrary();
        case MacOS -> new MacOSNetworkLibrary();
        default -> throw new FrameworkException(ExceptionType.NETWORK, "Unsupported operating system");
    };
}
