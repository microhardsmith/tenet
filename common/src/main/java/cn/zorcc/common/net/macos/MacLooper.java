package cn.zorcc.common.net.macos;

import cn.zorcc.common.Constants;
import cn.zorcc.common.IntMap;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.net.Looper;
import cn.zorcc.common.net.NetConfig;
import cn.zorcc.common.net.Socket;
import cn.zorcc.common.pojo.Loc;
import lombok.extern.slf4j.Slf4j;

import java.lang.foreign.*;
import java.lang.invoke.VarHandle;

@Slf4j
public class MacLooper implements Looper {
    private final NetConfig config;
    private final MacNative lib;
    private final MacState state;
    public MacLooper(NetConfig netConfig) {
        this.config = netConfig;
        this.lib = new MacNative();
        this.state = new MacState();
    }

    @Override
    public void init() {

    }

    @Override
    public void create(Arena arena) {
        int kq = lib.kqueue();
        if(kq == -1) {
            throw new FrameworkException(ExceptionType.NET, "Failed to create kqueue with errno : %d", lib.errno());
        }
        state.setKq(kq);
        state.setKqErr(lib.keventErr());
        state.setKqEof(lib.keventEof());
        state.setAddressLen(lib.addressLen());
        state.setEwouldblock(lib.ewouldblock());
        state.setEinprogress(lib.einprogress());
    }

    @Override
    public void socket(Arena arena) {
        int serverSocket = lib.socketCreate();
        if(serverSocket == -1) {
            throw new FrameworkException(ExceptionType.NET, "Failed to create server socket with errno : %d", lib.errno());
        }
        state.setServerSocket(new Socket(serverSocket));
        final int reuseAddr = lib.setReuseAddr(serverSocket, config.getReuseAddr() ? 1 : 0);
        if(reuseAddr == -1) {
            throw new FrameworkException(ExceptionType.NET, "Failed to setReuseAddr with err code : %d", lib.errno());
        }
        final int keepAlive = lib.setKeepAlive(serverSocket, config.getKeepAlive() ? 1 : 0);
        if(keepAlive == -1) {
            throw new FrameworkException(ExceptionType.NET, "Failed to setKeepAlive with err code : %d", lib.errno());
        }
        final int tcpNoDelay = lib.setTcpNoDelay(serverSocket, config.getTcpNoDelay() ? 1 : 0);
        if(tcpNoDelay == -1) {
            throw new FrameworkException(ExceptionType.NET, "Failed to setTcpNoDelay with err code : %d", lib.errno());
        }
        final int nonBlocking = lib.setNonBlocking(serverSocket);
        if(nonBlocking == -1) {
            throw new FrameworkException(ExceptionType.NET, "Failed to setNonBlocking with err code : %d", lib.errno());
        }
    }

    @Override
    public void bind(Arena arena) {
        MemorySegment serverAddr = arena.allocate(MacNative.sockAddrLayout);
        MemorySegment ipString = arena.allocateArray(ValueLayout.JAVA_BYTE, lib.addressLen());
        ipString.setUtf8String(0L, config.getIp());
        final int port = config.getPort();
        final int setSockAddr = lib.setSockAddr(serverAddr, ipString, port);
        if(setSockAddr == -1) {
            throw new FrameworkException(ExceptionType.NET, "Failed to set server address with err code : %d", lib.errno());
        }else if(setSockAddr == 0) {
            throw new FrameworkException(ExceptionType.NET, "IpAddress is not valid : %s", config.getIp());
        }
        int bind = lib.bind(serverAddr, state.serverSocket().intValue(), MacNative.sockAddrSize);
        if(bind == -1) {
            throw new FrameworkException(ExceptionType.NET, "Failed to bind server address with err code : %d", lib.errno());
        }
    }

    @Override
    public void listen(Arena arena) {
        final int listen = lib.listen(state.serverSocket().intValue(), config.getBacklog());
        if(listen == -1) {
            throw new FrameworkException(ExceptionType.NET, "Failed to listen server address with err code : %d", lib.errno());
        }
    }

    @Override
    public void ctl(Arena arena) {
        MemorySegment changeList = arena.allocate(MacNative.keventLayout);
        final int ctl = lib.keventAdd(state.kq(), changeList, state.serverSocket().intValue(), Constants.EVFILT_READ);
        if(ctl == -1) {
            throw new FrameworkException(ExceptionType.NET, "Failed to ctl kqueue state with err code : %d", lib.errno());
        }
        short flag = (short) MacNative.flagsHandle.get(changeList);
        if(flag == state.kqErr()) {
            throw new FrameworkException(ExceptionType.NET, "Failed to set kevent, error data : %l", MacNative.dataHandle.get(changeList));
        }
    }

    @Override
    public void loop(Arena arena) {
        final int maxEvents = config.getMaxEvents();
        final SequenceLayout eventListLayout = MemoryLayout.sequenceLayout(maxEvents, MacNative.keventLayout);
        final MemorySegment eventList = arena.allocate(eventListLayout);
        final VarHandle seqHandle = eventListLayout.varHandle(MemoryLayout.PathElement.sequenceElement());

        final Thread currentThread = Thread.currentThread();
        while (!currentThread.isInterrupted()) {
            int size = lib.keventWait(state.kq(), eventList, maxEvents);
            if(size == -1) {
                throw new FrameworkException(ExceptionType.NET, "Kevent wait failure : %d", lib.errno());
            }
            for(int i = 0; i < size; i++) {
                MemorySegment current = (MemorySegment) seqHandle.get(eventList, i);
                // ident is unsigned long because it has to represent a variety of different types of events, here we only need int
                int fd = (int) MacNative.identHandle.get(current);
                short filter = (short) MacNative.filterHandle.get(current);
                short flags = (short) MacNative.flagsHandle.get(current);
                if(fd == state.serverSocket().intValue()) {
                    // needs to accept the connection
                    acceptConnection(arena);
                }else if((flags & state.kqEof()) > 0){
                    // connection closed
                    removeConnection(fd);
                }else if(filter == Constants.EVFILT_READ) {
                    // read data
                    recvData(fd);
                }else if(filter == Constants.EVFILT_WRITE) {
                    // indicates a connection is successfully established
                }
            }
        }
    }

    /**
     *   接受客户端连接
     */
    private void acceptConnection(Arena arena) {
        MemorySegment clientAddr = arena.allocate(MacNative.sockAddrLayout);
        final int len = state.addressLen();
        MemorySegment ip = arena.allocateArray(ValueLayout.JAVA_BYTE, len);
        final int client = lib.accept(state.serverSocket().intValue(), clientAddr, MacNative.sockAddrSize);
        if(client == -1) {
            int errno = lib.errno();
            if(errno != state.ewouldblock()) {
                log.error("Accept new connection failed with errno : {}", errno);
            }
            return ;
        }
        int address = lib.address(clientAddr, ip, len);
        if(address == -1) {
            log.error("Failed to convert client's ip address, errno : {}", lib.errno());
            return ;
        }
        String ipStr = ip.getUtf8String(0L);
        int port = lib.port(clientAddr);
//        Conn conn = new Conn(new Socket(client), new Loc(ipStr, port));
//        final IntMap<Conn> connMap = state.connMap();
//        connMap.put(client, conn);
    }

    private void removeConnection(int fd) {
//        final IntMap<Conn> connMap = state.connMap();
//        Conn conn = connMap.remove(fd);
//        // TODO
//        int close = lib.close(fd);
//        if(close == -1) {
//            log.error("Failed to close socket : {}, errno : {}", conn.loc(), lib.errno());
//        }
    }

    private void recvData(int fd) {
//        final IntMap<Conn> connMap = state.connMap();
//        final Conn conn = connMap.get(fd);
//        final Arena arena = conn.arena();

    }

    @Override
    public void shutdown() {

    }
}
