package cn.zorcc.common.net.win;

import cn.zorcc.common.net.SocketInfo;
import lombok.Data;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.VarHandle;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *   记录WinLooper的运行状态
 */
@Data
public final class WinState {
    private MemorySegment handle;
    private Long serverSocket;
    private Map<Long, SocketInfo> socketMap = new ConcurrentHashMap<>();
    public int maxEvents = -1;
    public MemorySegment events;
    public VarHandle eventsHandle;
    public VarHandle fdHandle;
    public MemorySegment clientAddr;
    public MemorySegment addrStr;
}
