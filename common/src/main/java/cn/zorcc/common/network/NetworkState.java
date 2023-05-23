package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import lombok.extern.slf4j.Slf4j;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multiplexing state for Master and Worker
 * Note that there is no need to manually close all the sockets in the map when exit, the operating system would do it automatically.
 * @param mux Multiplexing handle, For master and worker
 * @param socket Current server socket, could be long in Windows or int in macOS and Linux
 * @param events events array
 * @param socketMap socket node map for established acceptor bindings or channel bindings
 */
@Slf4j
public record NetworkState(
        Mux mux,
        Socket socket,
        MemorySegment events,
        ConcurrentHashMap<Socket, Object> socketMap
) {
    private static final Native n = Native.n;

    /**
     *   create NetworkState for master
     */
    public static NetworkState forMaster(NetworkConfig config) {
        Mux m = n.createMux();
        Socket s = n.createSocket(config, true);
        MemorySegment array = n.createEventsArray(config);
        return new NetworkState(m, s, array, new ConcurrentHashMap<>(Net.MAP_SIZE));
    }

    /**
     *   create NetworkState for worker
     */
    public static NetworkState forWorker(NetworkConfig config) {
        Mux m = n.createMux();
        MemorySegment array = n.createEventsArray(config);
        return new NetworkState(m, null, array, new ConcurrentHashMap<>(Net.MAP_SIZE));
    }

    public void shouldRead(Socket socket, ReadBuffer readBuffer) {
        switch (socketMap.get(socket)) {
            case null -> {}
            case Acceptor acceptor -> acceptor.connector().shouldRead(acceptor);
            case Channel channel -> channel.protocol().canRead(channel, readBuffer);
            case default -> throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    public void shouldWrite(Socket socket) {
        switch (socketMap.get(socket)) {
            case null -> {}
            case Acceptor acceptor -> acceptor.connector().shouldWrite(acceptor);
            case Channel channel -> channel.protocol().canWrite(channel);
            case default -> throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }
}
