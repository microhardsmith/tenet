package cn.zorcc.common.network;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.pojo.Loc;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Map;

/**
 *   Platform independent native interface for Network operation
 */
public sealed interface Native permits WinNative, LinuxNative, MacNative {

    /**
     *   Return err code which means connect operation would possibly block
     */
    int connectBlockCode();

    /**
     *   Return err code which means send operation would possibly block
     */
    int sendBlockCode();

    /**
     *   Return err code which means the underlying function all was interrupted
     */
    int interruptCode();

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
    MemorySegment createEventsArray(MuxConfig config, Arena arena);

    /**
     *   Create a socket, could be server socket or client socket
     */
    Socket createSocket();

    /**
     *   Configure socket based on NetworkConfig
     */
    void configureSocket(NetworkConfig config, Socket socket);

    /**
     *   Bind and listen target port
     */
    void bindAndListen(Loc loc, MuxConfig config, Socket socket);

    /**
     *   Modify mux event registration, from represent the old status, to represent the target status
     *   return 0 if success, -1 if failed
     */
    void ctl(Mux mux, Socket socket, int from, int to);

    /**
     *   Multiplexing wait for events, return the available events
     *   Method will block for maximum timeout milliseconds, use -1 for epoll, NULL_PTR for kqueue to block infinitely
     */
    int multiplexingWait(Mux mux, MemorySegment events, int maxEvents, Timeout timeout);

    /**
     *   Waiting for new connections, don't throw exception here because it would exit the loop thread
     */
    ClientSocket waitForAccept(NetworkConfig config, Socket serverSocket, MemorySegment events, int index);

    /**
     *   Waiting for data, don't throw exception here cause it would exit the loop thread
     */
    void waitForData(Map<Socket, Actor> socketMap, MemorySegment buffer, MemorySegment events, int index);

    /**
     *   Connect socket with target socketAddr, return 0 if connection is successful, return -1 if error occurred
     */
    int connect(Socket socket, MemorySegment sockAddr);

    /**
     *   Accept socket connection, return the accepted client loc and socket
     */
    ClientSocket accept(NetworkConfig config, Socket socket);

    /**
     *   Recv data from remote socket, return the actual bytes received
     */
    long recv(Socket socket, MemorySegment data, long len);

    /**
     *   Send data to remote socket, return the actual bytes sent
     */
    long send(Socket socket, MemorySegment data, long len);

    /**
     *   Target socket's err opt, should return 0 if there is no error
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
     *   Current thread's errno
     */
    int errno();

    /**
     *   Exit mux resource
     */
    void exitMux(Mux mux);

    /**
     *   Exit, releasing all resources
     */
    void exit();

    /**
     *   Read write status constants
     */
    int REGISTER_NONE = 0;
    int REGISTER_READ = 1;
    int REGISTER_WRITE = 2;
    int REGISTER_READ_WRITE = 3;

    /**
     *   Internal check the return value
     */
    default int check(int value, String errMsg) {
        if(value == -1) {
            int err = errno();
            throw new FrameworkException(ExceptionType.NETWORK, "Failed to %s with err code : %d", errMsg, err);
        }
        return value;
    }

    /**
     *   Internal check the return value
     */
    default long check(long value, String errMsg) {
        if(value == -1L) {
            int err = errno();
            throw new FrameworkException(ExceptionType.NETWORK, "Failed to %s with err code : %d", errMsg, err);
        }
        return value;
    }

    /**
     *   Network library environment variable
     */
    String LIB = "lib";

    /**
     *   Global native network library
     */
    Native n = createNative();

    /**
     *   Create native library based on target operating system
     */
    private static Native createNative() {
        return switch (NativeUtil.ostype()) {
            case Windows -> new WinNative();
            case Linux -> new LinuxNative();
            case MacOS -> new MacNative();
            default -> throw new FrameworkException(ExceptionType.NETWORK, "Unsupported operating system");
        };
    }
}
