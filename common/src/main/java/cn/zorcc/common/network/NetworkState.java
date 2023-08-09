package cn.zorcc.common.network;

import java.lang.foreign.MemorySegment;
import java.util.Map;

/**
 * Multiplexing state for Master and Worker
 * Note that there is no need to manually close all the sockets in the map when exiting, the operating system would do it automatically.
 * @param mux Multiplexing handle, For master and worker
 * @param events events array
 * @param socketMap socket node map for established acceptor bindings or channel bindings
 */
public record NetworkState(
        Mux mux,
        MemorySegment events,
        Map<Socket, Object> socketMap
) {

}
