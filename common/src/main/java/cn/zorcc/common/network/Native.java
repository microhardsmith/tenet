package cn.zorcc.common.network;

import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.MemorySegment;

/**
 *   Platform native interface for Network operation
 */
public interface Native {

    /**
     *   err code means connect operation would block
     */
    int connectBlockCode();

    /**
     *   err code means send operation would block
     */
    int sendBlockCode();

    /**
     *   prepare multiplexing resources (wepoll, epoll or kqueue)
     */
    Mux createMux();

    MemorySegment createEventsArray(NetworkConfig config);

    /**
     *   create socket, could be server socket or client socket
     */
    Socket createSocket(NetworkConfig config, boolean isServer);

    /**
     *   bind and listen target port
     */
    void bindAndListen(NetworkConfig config, Socket socket);

    /**
     *   register read event
     *   use case:
     *      1. when client-side channel is connected, register read events
     *      2. when server-side channel is accepted, register read events
     */
    void registerRead(Mux mux, Socket socket);

    /**
     *   register write event for only one shot
     *   use case:
     *      1. when channel is not writable (TCP write buffer is full)
     *      2. when nonblocking channel is establishing connection
     */
    void registerWrite(Mux mux, Socket socket);

    /**
     *   unregister socket event, used when channel has disconnected
     */
    void unregister(Mux mux, Socket socket);

    /**
     *   waiting for new connections, don't throw exception here cause it would exit the loop thread
     */
    void waitForAccept(Net net, NetworkState state);

    /**
     *   waiting for data, don't throw exception here cause it would exit the loop thread
     */
    void waitForData(ReadBuffer[] buffers, NetworkState state);

    /**
     *   Connect remote Loc, adding it to current Remote instance
     */
    void connect(Net net, Remote remote, Codec codec);

    /**
     *   Close a socket(read 0 or manually disconnect)
     */
    void closeSocket(Socket socket);

    /**
     *   send data to remote socket
     */
    int send(Socket socket, MemorySegment data, int len);

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
     *   internal check the return value
     */
    default int check(int value, String errMsg) {
        if(value == -1) {
            Integer err = errno();
            throw new FrameworkException(ExceptionType.NETWORK, "Failed to %s with err code : %d", errMsg, err);
        }
        return value;
    }

    /**
     *   internal check the return value
     */
    default long check(long value, String errMsg) {
        if(value == -1L) {
            Integer err = errno();
            throw new FrameworkException(ExceptionType.NETWORK, "Failed to %s with err code : %d", errMsg, err);
        }
        return value;
    }

    Native n = createNative();
    static Native createNative() {
        if(NativeUtil.isLinux()) {
            return new LinuxNative();
        }else if(NativeUtil.isWindows()) {
            return new WinNative();
        }else if(NativeUtil.isMacos()) {
            // TODO 需要区分x64和arm
            return new MacNative();
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, "Unsupported operating system");
        }
    }
}
