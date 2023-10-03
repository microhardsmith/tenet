package cn.zorcc.common.network;

import java.lang.foreign.MemorySegment;
import java.time.Duration;

public sealed interface Actor permits Acceptor, Channel {
    /**
     *   Indicating that current actor could perform read action
     */
    void canRead(MemorySegment buffer);

    /**
     *   Indicating that current actor could perform write action
     */
    void canWrite();

    /**
     *   Indicating that current actor could perform shutdown operation
     */
    void canShutdown(Duration duration);
}
