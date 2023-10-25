package cn.zorcc.common.network;

import java.lang.foreign.MemorySegment;

public interface Multiplexer {

    /**
     *   Indicates that worker can now read from this channel
     *   This method will only be invoked in worker's reader thread
     */
    void canRead(MemorySegment reserved);

    /**
     *   Indicates that worker can now write to this channel
     *   This method will only be invoked in worker's reader thread
     */
    void canWrite();
}
