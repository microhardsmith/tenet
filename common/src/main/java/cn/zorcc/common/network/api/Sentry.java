package cn.zorcc.common.network.api;

import java.lang.foreign.MemorySegment;

/**
 *   Sentry determines how a channel could upgrade to its Protocol
 *   All the methods will be executed in poller thread only, so there is no need to add external lock on it
 */
public interface Sentry {
    /**
     *   This function would be invoked when channel become readable
     *   the parameter len will always be the exact length of data segment
     *   return a flag to indicate a state change
     */
    long onReadableEvent(MemorySegment reserved, long len);

    /**
     *   This function would be invoked when channel become writable
     *   return a flag to indicate a state change
     */
    long onWritableEvent();

    /**
     *   Update current sentry to protocol
     */
    Protocol toProtocol();

    /**
     *   This function would be invoked when Sentry is being closed
     *   Node that when sentry was upgrade to protocol, this function would not be invoked, which means doClose() may never get executed at all
     */
    void doClose();
}
