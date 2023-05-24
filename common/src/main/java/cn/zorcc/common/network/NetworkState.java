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
 * Note that there is no need to manually close all the sockets in the map when exiting, the operating system would do it automatically.
 * @param mux Multiplexing handle, For master and worker
 * @param events events array
 * @param socketMap socket node map for established acceptor bindings or channel bindings
 */
@Slf4j
public record NetworkState(
        Mux mux,
        MemorySegment events,
        ConcurrentHashMap<Socket, Object> socketMap
) {

    public void shouldRead(Socket socket, ReadBuffer readBuffer) {
        switch (socketMap.get(socket)) {
            case null -> {}
            case Acceptor acceptor -> acceptor.connector().shouldRead(acceptor);
            case Channel channel -> channel.protocol().canRead(channel, readBuffer);
            default -> throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    public void shouldWrite(Socket socket) {
        switch (socketMap.get(socket)) {
            case null -> {}
            case Acceptor acceptor -> acceptor.connector().shouldWrite(acceptor);
            case Channel channel -> channel.protocol().canWrite(channel);
            default -> throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }
}
