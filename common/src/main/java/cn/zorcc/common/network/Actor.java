package cn.zorcc.common.network;

import java.lang.foreign.MemorySegment;

public sealed interface Actor permits Acceptor, Channel {
    void canRead(MemorySegment buffer);

    void canWrite();

    void canShutdown(Shutdown shutdown);
}
