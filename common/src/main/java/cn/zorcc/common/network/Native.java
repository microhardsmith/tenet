package cn.zorcc.common.network;

import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.pojo.Loc;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

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
     *   Bind and listen target port
     */
    void bindAndListen(NetworkConfig config, Socket socket);

    /**
     *   Modify mux event registration, from represent the old status, to represent the target status
     *   return 0 if success, -1 if failed
     */
    void ctl(Mux mux, Socket socket, int from, int to);

    /**
     *   Multiplexing wait for events, return the available events
     */
    int multiplexingWait(NetworkState state, int maxEvents);

    /**
     *   Waiting for new connections, don't throw exception here because it would exit the loop thread
     */
    void waitForAccept(NetworkState state, int index, Net net);

    /**
     *   Waiting for data, don't throw exception here cause it would exit the loop thread
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
     *   Recv data from remote socket, return the actual bytes received TODO 8 bytes len and result
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
     *   Network library environment variable
     */
    String LIB = "tenet.lib";

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
