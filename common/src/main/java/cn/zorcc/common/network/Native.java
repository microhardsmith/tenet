package cn.zorcc.common.network;

import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.pojo.Loc;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *   Platform native interface for Network operation
 */
public interface Native {

    /**
     *   Return err code which means connect operation would possibly block
     */
    int connectBlockCode();

    /**
     *   Return err code which means send operation would possibly block
     */
    int sendBlockCode();

    /**
     *   Create sockAddr struct according to loc using target arena
     */
    MemorySegment createSockAddr(Loc loc, Arena arena);

    /**
     *   Create multiplexing resources (wepoll, epoll or kqueue)
     */
    Mux createMux();

    /**
     *   Create multiplexing struct array
     */
    MemorySegment createEventsArray(NetworkConfig config);

    /**
     *   Create a socket, could be server socket or client socket
     */
    Socket createSocket(NetworkConfig config);

    /**
     *   bind and listen target port
     */
    void bindAndListen(NetworkConfig config, Socket socket);

    /**
     *   Register mux event, from represent the old status, to represent the target status
     *   return 0 if success, -1 if failed
     */
    void register(Mux mux, Socket socket, int from, int to);

    /**
     *   Unregister socket event, delete all events from current mux
     *   note that kqueue will automatically remote the registry when socket was closed, but we still manually unregister it for consistency
     *   return 0 if success, -1 if failed
     */
    void unregister(Mux mux, Socket socket, int current);

    /**
     *   Multiplexing wait for events, return the available events
     */
    int multiplexingWait(NetworkState state, int maxEvents);

    /**
     *   waiting for new connections, don't throw exception here because it would exit the loop thread
     */
    void waitForAccept(NetworkState state, int index, Net net);

    /**
     *   waiting for data, don't throw exception here cause it would exit the loop thread
     */
    void waitForData(NetworkState state, int index, ReadBuffer readBuffer);

    /**
     *   Connect socket with target socketAddr, return 0 if connection is successful, return -1 if error occurred
     */
    int connect(Socket socket, MemorySegment sockAddr);

    /**
     *   Accept socket connection, return the accepted client loc and socket
     */
    ClientSocket accept(NetworkConfig config, Socket socket);

    /**
     *   recv data from remote socket, return the actual bytes received
     */
    int recv(Socket socket, MemorySegment data, int len);

    /**
     *   send data to remote socket, return the actual bytes sent
     */
    int send(Socket socket, MemorySegment data, int len);

    /**
     *   get target socket's err opt, should return 0 if there is no error
     */
    int getErrOpt(Socket socket);

    /**
     *   Close a socket
     */
    void closeSocket(Socket socket);

    /**
     *   Shutdown the write side of the socket
     */
    void shutdownWrite(Socket socket);

    /**
     *   get current errno
     */
    int errno();

    /**
     *   exit mux resource
     */
    void exitMux(Mux mux);

    /**
     *   exit, releasing all resources
     */
    void exit();

    /**
     *   read write status code
     */
    int REGISTER_NONE = 0;
    int REGISTER_READ = 1;
    int REGISTER_WRITE = 2;
    int REGISTER_READ_WRITE = 3;

    static void tryRegisterRead(AtomicInteger state, Mux mux, Socket socket) {
        int current = state.getAndUpdate(i -> (i & REGISTER_READ) == 0 ? i + REGISTER_READ : i);
        if((current & REGISTER_READ) == 0) {
            n.register(mux, socket, current, current + REGISTER_READ);
        }
    }

    static void tryRegisterWrite(AtomicInteger state, Mux mux, Socket socket) {
        int current = state.getAndUpdate(i -> (i & REGISTER_WRITE) == 0 ? i + REGISTER_WRITE : i);
        if((current & REGISTER_WRITE) == 0) {
            n.register(mux, socket, current, current + REGISTER_READ);
        }
    }

    /**
     *   Convert a int port to a unsigned short type
     */
    default short shortPort(int port) {
        if(port < 1 || port > 65535) {
            throw new FrameworkException(ExceptionType.NETWORK, "Port overflow");
        }
        // force retain the lower 16bits, the answer could be negative
        return (short) port;
    }

    /**
     *   Convert  a unsigned short type to a int
     */
    default int intPort(short port) {
        return 0xFFFF & port;
    }

    /**
     *   Internal check the return value
     */
    default int check(int value, String errMsg) {
        if(value == -1) {
            Integer err = errno();
            throw new FrameworkException(ExceptionType.NETWORK, "Failed to %s with err code : %d", errMsg, err);
        }
        return value;
    }

    /**
     *   Internal check the return value
     */
    default long check(long value, String errMsg) {
        if(value == -1L) {
            Integer err = errno();
            throw new FrameworkException(ExceptionType.NETWORK, "Failed to %s with err code : %d", errMsg, err);
        }
        return value;
    }

    /**
     *   Global native network library
     */
    Native n = createNative();

    /**
     *   Create native library based on target operating system
     */
    private static Native createNative() {
        if(NativeUtil.isLinux()) {
            return new LinuxNative();
        }else if(NativeUtil.isWindows()) {
            return new WinNative();
        }else if(NativeUtil.isMacos()) {
            return new MacNative();
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, "Unsupported operating system");
        }
    }
}
