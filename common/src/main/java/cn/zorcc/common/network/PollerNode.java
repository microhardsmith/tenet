package cn.zorcc.common.network;

import java.lang.foreign.MemorySegment;
import java.time.Duration;

public sealed interface PollerNode permits SentryPollerNode, ProtocolPollerNode {
    /**
     *   This function would be invoked when PollerNode was first mounted on the poller instance
     */
    void onMounted();
    /**
     *   This function would be invoked when channel become readable
     */
    void onReadableEvent(MemorySegment reserved, int len);

    /**
     *   This function would be invoked when channel become writable
     */
    void onWritableEvent();

    /**
     *   Register a taggedMsg to the pollerNode
     */
    void onRegisterTaggedMsg(PollerTask pollerTask);

    /**
     *   Unregister a taggedMsg from the pollerNode
     */
    void onUnregisterTaggedMsg(PollerTask pollerTask);

    /**
     *   This function would be invoked if channel has exception thrown or needs to be force closed
     */
    void onClose(PollerTask pollerTask);

    /**
     *   Exit current pollerNode
     */
    void exit(Duration duration);
}
