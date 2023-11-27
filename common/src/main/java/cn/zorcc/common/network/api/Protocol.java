package cn.zorcc.common.network.api;

import java.lang.foreign.MemorySegment;

/**
 *   Protocol determines how a channel should interact with the poller thread and writer thread, protocol instance will be shared among them
 *   so if an action will be performed by both thread, there must be an external lock to guarantee the safety of it
 */
public interface Protocol {

    /**
     *   Indicates that protocol could read from the socket now, this function will always be invoked on poller thread
     *   the parameter len will always be the exact length of reserved segment
     *   return a positive number to indicate a state change, or a negative number indicates the actually bytes read
     */
    int onReadableEvent(MemorySegment reserved, int len);

    /**
     *   Indicates that protocol could write to the socket now, this function will always be invoked on poller thread
     *   return a positive number to indicate a state change
     */
    int onWritableEvent();

    /**
     *   Perform the actual write operation
     *   the parameter len will always be the exact length of data segment
     *   return a positive number to indicate a state change, or a negative number indicates the actually bytes read
     */
    int doWrite(MemorySegment data, int len);

    /**
     *   Perform the actual shutdown operation, this function will always be invoked on writer thread
     *   It can be guaranteed that this function will be only invoked once, no external synchronization needed
     *   If a RuntimeException was thrown in this function, channel will be closed
     */
    void doShutdown();

    /**
     *   Perform the actual close operation, this function could be invoked in poller thread or writer thread depending on the situation (the later closed one gets to call doClose())
     *   It can be guaranteed that this function will be only invoked once, no external synchronization needed
     *   RuntimeException thrown in this function would not be handled, it would only be recorded in log
     */
    void doClose();
}
