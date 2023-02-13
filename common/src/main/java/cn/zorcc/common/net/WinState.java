package cn.zorcc.common.net;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.VarHandle;

/**
 *   记录WinLooper的运行状态
 */
public final class WinState {
    public Arena selfArena;
    public Arena sharedArena;
    public MemorySegment epollHandle;
    public long socket = -1;

    public int maxEvents = -1;
    public MemorySegment events;
    public VarHandle eventsHandle;
    public VarHandle fdHandle;
    public MemorySegment clientAddr;
    public MemorySegment addrStr;

}
