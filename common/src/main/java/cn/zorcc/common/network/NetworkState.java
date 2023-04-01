package cn.zorcc.common.network;

import lombok.Data;

import java.lang.foreign.MemorySegment;
import java.util.Map;

/**
 *   Multiplexing state for Master and Worker
 */
@Data
public class NetworkState {

    /**
     *   Multiplexing handle, For master and worker
     */
    private Mux mux;

    /**
     *   Current server socket, could be long in Windows or int in macOS and Linux
     */
    private Socket socket;

    /**
     *   events array
     */
    private MemorySegment events;

    /**
     *   socket map for established channel binding
     */
    private Map<Long, Channel> longMap;
    private Map<Integer, Channel> intMap;
}
