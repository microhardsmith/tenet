package cn.zorcc.common.network;

public enum PollerTaskType {
    /**
     *   Bind a SentryPollerNode to current poller instance
     */
    BIND,
    /**
     *   Unbind a SentryPollerNode on timeout, used for client-side application
     */
    UNBIND,
    /**
     *   Register a message tag
     */
    REGISTER,
    /**
     *   Unregister a message tag
     */
    UNREGISTER,
    /**
     *   Indicates a taggedMsg has been timeout
     */
    TIMEOUT,
    /**
     *   Force close a underlying channel
     */
    CLOSE,
    /**
     *   Indicates that current writer instance has no channel bound to it, it might be a potential exit for the whole application
     */
    POTENTIAL_EXIT,
    /**
     *   Tell the poller to shut down all the channel bound to it for exiting the whole application
     */
    EXIT,
}
