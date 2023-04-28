package cn.zorcc.common.network;

import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.MemorySegment;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multiplexing state for Master and Worker
 * @param mux Multiplexing handle, For master and worker
 * @param socket Current server socket, could be long in Windows or int in macOS and Linux
 * @param events events array
 * @param longMap socket map for established channel binding for windows
 * @param intMap socket map for established channel binding for linux and macos
 */
public record NetworkState(
        Mux mux,
        Socket socket,
        MemorySegment events,
        Map<Long, Channel> longMap,
        Map<Integer, Channel> intMap
) {
    private static final Native n = Native.n;

    /**
     *   create NetworkState for master
     */
    public static NetworkState forMaster(NetworkConfig config) {
        Mux m = n.createMux();
        Socket s = n.createSocket(config, true);
        MemorySegment array = n.createEventsArray(config);
        if(NativeUtil.isWindows()) {
            return new NetworkState(m, s, array, new ConcurrentHashMap<>(config.getMapSize()), null);
        }else {
            return new NetworkState(m, s, array, null, new ConcurrentHashMap<>(config.getMapSize()));
        }
    }

    /**
     *   create NetworkState for worker
     */
    public static NetworkState forWorker(NetworkConfig config) {
        Mux m = n.createMux();
        MemorySegment array = n.createEventsArray(config);
        if(NativeUtil.isWindows()) {
            return new NetworkState(m, null, array, new ConcurrentHashMap<>(config.getMapSize()), null);
        }else {
            return new NetworkState(m, null, array, null, new ConcurrentHashMap<>(config.getMapSize()));
        }
    }

    public void registerChannel(Channel channel) {
        Socket s = channel.socket();
        if(NativeUtil.isWindows()) {
            longMap.put(s.longValue(), channel);
        }else {
            intMap.put(s.intValue(), channel);
        }
    }
}
