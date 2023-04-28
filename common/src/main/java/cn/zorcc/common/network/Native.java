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
     *   return err code which means connect operation would block on current operating system
     */
    int connectBlockCode();

    /**
     *   return err code which means send operation would block on current operating system
     */
    int sendBlockCode();

    /**
     *   create sockAddr struct according to loc using target arena
     */
    MemorySegment createSockAddr(Loc loc, Arena arena);

    /**
     *   create multiplexing resources (wepoll, epoll or kqueue)
     */
    Mux createMux();

    /**
     *   create multiplexing struct array
     */
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
     *   register write event for only one-shot
     *   use case:
     *      1. when channel is not writable (TCP write buffer is full)
     *      2. when nonblocking channel is establishing connection
     */
    void registerWrite(Mux mux, Socket socket);

    /**
     *   unregister socket event
     *   note that kqueue will automatically remote the registry when socket was closed, but we still manually unregister it for consistency
     */
    void unregister(Mux mux, Socket socket);

    /**
     *   multiplexing wait for events, return the available events
     */
    int multiplexingWait(NetworkState state, int maxEvents);

    void checkConnection(NetworkState state, int index, Net net);

    void checkData(NetworkState state, int index, ReadBuffer readBuffer);

    /**
     *   waiting for new connections, don't throw exception here because it would exit the loop thread
     */
    void waitForAccept(Net net, NetworkState state);

    /**
     *   waiting for data, don't throw exception here cause it would exit the loop thread
     */
    void waitForData(ReadBuffer[] buffers, NetworkState state);

    /**
     *   Close a socket
     */
    void closeSocket(Socket socket);

    /**
     *   Shutdown the write side of the socket
     */
    void shutdownWrite(Socket socket);

    /**
     *   Accept socket connection, return the accepted client loc and socket
     */
    ClientSocket accept(Socket socket);

    /**
     *   Connect socket with target socketAddr, return true if connection is successful, return false if connection is in-process
     *   throw exception if error occurred
     */
    boolean connect(Socket socket, MemorySegment sockAddr);

    /**
     *   Connect target channel, if non-blocking channel's connection is in-process, net will register write event
     */
    boolean connect(Net net, Channel channel);

    /**
     *   recv data from remote socket, return the actual bytes received
     */
    int recv(Socket socket, MemorySegment data, int len);

    /**
     *   send data to remote socket, return the actual bytes sent
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
            return new MacNative();
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, "Unsupported operating system");
        }
    }
}
