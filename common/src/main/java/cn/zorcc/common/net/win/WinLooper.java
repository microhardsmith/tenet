package cn.zorcc.common.net.win;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.net.Looper;
import cn.zorcc.common.net.NetConfig;
import cn.zorcc.common.util.NativeUtil;
import cn.zorcc.common.util.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.foreign.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

/**
 *  looper implementation under windows
 */
@Slf4j
public class WinLooper implements Looper {
    private final AtomicBoolean startFlag = new AtomicBoolean(false);
    private static final String NAME = "winLooper";
    private final NetConfig config;
    private final WinNative lib;
    private final WinState state;
    private final Thread loopThread;
    public WinLooper(NetConfig netConfig) {
        this.config = netConfig;
        this.lib = new WinNative();
        this.state = new WinState();
        this.loopThread = ThreadUtil.platform(NAME, () -> {
            final Thread currentThread = Thread.currentThread();
//            state.selfArena = Arena.openConfined();
//            state.sharedArena = Arena.openShared();
//            state.epollHandle = lib.epollCreate();
//            if(NativeUtil.checkNullPointer(state.epollHandle)) {
//                throw new FrameworkException(ExceptionType.NET, "EpollCreate() failed with NULL pointer exception");
//            }
//            state.socket = check(lib.socketCreate(), "socketCreate()");
//            check(lib.setReuseAddr(state.socket, config.getReuseAddr()), "setReuseAddr()");
//            check(lib.setKeepAlive(state.socket, config.getKeepAlive()), "setKeepAlive()");
//            check(lib.setTcpNoDelay(state.socket, config.getTcpNoDelay()), "setTcpNoDelay()");
//            check(lib.setNonBlocking(state.socket), "setNonBlocking()");
//
//            MemorySegment serverAddr = state.selfArena.allocate(WinNative.sockAddrLayout);
//            serverAddr.fill((byte) 0);
//            MemorySegment ip = state.selfArena.allocateUtf8String(netConfig.getIp());
//            int sockResult = check(lib.setSockAddr(serverAddr, ip, netConfig.getPort()), "setSockAddr()");
//            if(sockResult == 0) {
//                throw new FrameworkException(ExceptionType.NET, "IpAddress is not legal in configuration : {}", netConfig.getIp());
//            }
//
//            check(lib.bind(serverAddr, state.socket, WinNative.sockAddrSize), "bind()");
//
//            check(lib.listen(state.socket, netConfig.getBacklog()), "listen()");
//
//            MemorySegment ev = state.selfArena.allocate(WinNative.epollEventLayout);
//            ev.fill((byte) 0);
//            WinNative.eventsHandle.set(ev, Constants.EPOLL_IN);
//            WinNative.fdHandle.set(ev, (int) state.socket);
//            check(lib.epollCtlAdd(state.epollHandle, state.socket, ev), "epollCtlAdd()");
//
//            state.maxEvents = netConfig.getMaxEvents();
//            SequenceLayout eventsLayout = MemoryLayout.sequenceLayout(state.maxEvents, WinNative.epollEventLayout);
//            state.events = state.selfArena.allocate(eventsLayout);
//            state.eventsHandle = eventsLayout.varHandle(MemoryLayout.PathElement.sequenceElement(), MemoryLayout.PathElement.groupElement("events"));
//            state.fdHandle = eventsLayout.varHandle(MemoryLayout.PathElement.sequenceElement(), MemoryLayout.PathElement.groupElement("data"), MemoryLayout.PathElement.groupElement("fd"));
//            state.clientAddr = state.selfArena.allocate(WinNative.sockAddrLayout);
//            final int addrLen = lib.addressLen();
//            state.addrStr = state.selfArena.allocateArray(ValueLayout.JAVA_BYTE, addrLen);
//            final int segmentSize = netConfig.getSegmentSize();
//            while (!currentThread.isInterrupted()) {
//                int count = lib.epollWait(state.epollHandle, state.events, state.maxEvents, -1);
//                if(count == -1) {
//                    // epoll_wait returns -1 means an error occurred
//                    log.error("Error occurred while epollWait()");
//                }
//                for(int i = 0; i < count; i++) {
//                    int fd = (int) state.fdHandle.get(state.events, i);
//                    if(fd == state.socket) {
//                        // new connection
//                        int clientFd = lib.accept(state.socket, state.clientAddr, WinNative.sockAddrSize);
//                        if(clientFd == -1) {
//                            log.error("Accept failure with err : {}", lib.getLastError());
//                        }
//                        int addressResult = lib.address(state.clientAddr, state.addrStr, addrLen);
//                        if(addressResult == -1) {
//                            log.error("Parsing client address failed : {}", lib.getLastError());
//                        }
//                        String ipStr = state.addrStr.getUtf8String(0);
//                        int port = lib.port(state.clientAddr);
//                        log.info("Receiving connection from {}:{}", ipStr, port);
//                        if(lib.setNonBlocking(fd) == -1) {
//                            log.error("Failed to set fd as nonBlocking : {}, err : {}", fd, lib.getLastError());
//                        }
//
//                        ev.fill((byte) 0);
//                        WinNative.eventsHandle.set(ev, Constants.EPOLL_IN);
//                        WinNative.fdHandle.set(ev, clientFd);
//                        if(lib.epollCtlAdd(state.epollHandle, clientFd, ev) == -1) {
//                            log.error("Failed to epoll ctl add : {}, err : {}", fd, lib.getLastError());
//                        }
//                    }else {
//                        int event = (int) state.eventsHandle.get(state.events, i);
//                        if((event & Constants.EPOLL_IN) > 0) {
//                            MemorySegment buffer = state.sharedArena.allocateArray(ValueLayout.JAVA_BYTE, segmentSize);
//                            int n = lib.recv(fd, buffer, segmentSize);
//                            if(n < 0) {
//                                log.error("Recv error : {}, err : {}", fd, lib.getLastError());
//                            }else if(n == 0) {
//                                int delResult = lib.epollCtlDel(state.epollHandle, fd);
//                                if(delResult == -1) {
//                                    log.error("Failed to epoll ctl del : {}, err : {}", fd, lib.getLastError());
//                                }
//                                int closeResult = lib.closeSocket(fd);
//                                if(closeResult == -1) {
//                                    log.error("Failed to close socket : {}, err : {}", fd, lib.getLastError());
//                                }
//                            }else {
//                                log.debug("Reading {} bytes from client", n);
//                                byte[] bytes = new byte[n];
//                                MemorySegment.copy(buffer, JAVA_BYTE, 0, bytes, 0, n);
//                                log.debug("Msg : {}", new String(bytes, StandardCharsets.UTF_8));
//                            }
//                        }
//                    }
//                }
//            }
        });
    }

    @Override
    public void init() {
        if(!startFlag.compareAndSet(false, true)) {
            throw new FrameworkException(ExceptionType.NET, "WinLooper already started");
        }
        this.loopThread.start();
    }

    @Override
    public void create(Arena arena) {
        MemorySegment handle = lib.epollCreate();
        if(NativeUtil.checkNullPointer(handle)) {
            throw new FrameworkException(ExceptionType.NET, "Failed to create wepoll with NULL pointer exception");
        }
        state.setHandle(handle);
    }

    @Override
    public void socket(Arena arena) {
        final long serverSocket = lib.socketCreate();
        if(serverSocket == -1L) {
            int err = lib.getLastError();
            throw new FrameworkException(ExceptionType.NET, "Failed to create socket with err code : %d", err);
        }
        state.setServerSocket(serverSocket);
        final int reuseAddr = lib.setReuseAddr(serverSocket, config.getReuseAddr());
        if(reuseAddr == -1) {
            int err = lib.getLastError();
            throw new FrameworkException(ExceptionType.NET, "Failed to setReuseAddr with err code : %d", err);
        }
        final int keepAlive = lib.setKeepAlive(serverSocket, config.getKeepAlive());
        if(keepAlive == -1) {
            int err = lib.getLastError();
            throw new FrameworkException(ExceptionType.NET, "Failed to setKeepAlive with err code : %d", err);
        }
        final int tcpNoDelay = lib.setTcpNoDelay(serverSocket, config.getTcpNoDelay());
        if(tcpNoDelay == -1) {
            int err = lib.getLastError();
            throw new FrameworkException(ExceptionType.NET, "Failed to setTcpNoDelay with err code : %d", err);
        }
        final int nonBlocking = lib.setNonBlocking(serverSocket);
        if(nonBlocking == -1) {
            int err = lib.getLastError();
            throw new FrameworkException(ExceptionType.NET, "Failed to setNonBlocking with err code : %d", err);
        }
    }

    @Override
    public void bind(Arena arena) {
        MemorySegment serverAddr = arena.allocate(WinNative.sockAddrLayout);
        int addressLen = lib.addressLen();
        MemorySegment ipString = arena.allocateArray(ValueLayout.JAVA_BYTE, addressLen);
        ipString.setUtf8String(0L, config.getIp());
        int port = config.getPort();
        int setSockAddr = lib.setSockAddr(serverAddr, ipString, port);
        if(setSockAddr == -1) {
            int err = lib.getLastError();
            throw new FrameworkException(ExceptionType.NET, "Failed to set server address with err code : %d", err);
        }else if(setSockAddr == 0) {
            throw new FrameworkException(ExceptionType.NET, "IpAddress is not valid : %s", config.getIp());
        }
        int bind = lib.bind(serverAddr, state.getServerSocket(), WinNative.sockAddrSize);
        if(bind == -1) {
            int err = lib.getLastError();
            throw new FrameworkException(ExceptionType.NET, "Failed to bind server address with err code : %d", err);
        }
    }

    @Override
    public void listen(Arena arena) {
        int listen = lib.listen(state.getServerSocket(), config.getBacklog());
        if(listen == -1) {
            int err = lib.getLastError();
            throw new FrameworkException(ExceptionType.NET, "Failed to listen server address with err code : %d", err);
        }
    }

    @Override
    public void ctl(Arena arena) {
//            MemorySegment ev = state.selfArena.allocate(WinNative.epollEventLayout);
//            ev.fill((byte) 0);
//            WinNative.eventsHandle.set(ev, Constants.EPOLL_IN);
//            WinNative.fdHandle.set(ev, (int) state.socket);
//            check(lib.epollCtlAdd(state.epollHandle, state.socket, ev), "epollCtlAdd()");
        MemorySegment ev = arena.allocate(WinNative.epollEventLayout);
        ev.fill(Constants.b0);
        WinNative.eventsHandle.set(ev, Constants.EPOLL_IN);
        // TODO 验证一下设置里面的socket是否可行，因为数据类型感觉有点对不上
    }

    @Override
    public void loop(Arena arena) {

    }

    @Override
    public void shutdown() {
        // release handle
        MemorySegment handle = state.getHandle();
        if(handle != null) {
            int epollCloseResult = lib.epollClose(handle);
            if(epollCloseResult == -1) {
                int err = lib.getLastError();
                log.error("epollClose() failed with error code : {}", err);
            }
        }
        // release established sockets
        state.getSocketMap().forEach((k,v) -> {
            lib.closeSocket(k);
            // TODO close thread
        });
        // release server socket
        Long serverSocket = state.getServerSocket();
        if(serverSocket != null) {
            lib.closeSocket(serverSocket);
        }
        // wsa clean-up
        int cleanUpResult = lib.cleanUp();
        if(cleanUpResult == -1) {
            int err = lib.getLastError();
            log.error("wsaCleanUp() failed with error code : {}", err);
        }
    }
}
