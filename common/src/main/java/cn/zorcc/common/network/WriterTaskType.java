package cn.zorcc.common.network;

public enum WriterTaskType {
    /**
     *   Bind a protocol and channel with current writer instance
     */
    INITIATE,
    /**
     *   Send a single msg over the channel
     */
    SINGLE_MSG,
    /**
     *   Send multiple msg over the channel
     */
    MULTIPLE_MSG,
    /**
     *   Indicates that channel is writable again
     */
    WRITABLE,
    /**
     *   Tell the writer to shut down the channel
     */
    SHUTDOWN,
    /**
     *   Tell the channel to force close the channel
     */
    CLOSE,
    /**
     *   Indicates that current writer instance has no channel bound to it, it might be a potential exit for the whole application
     */
    POTENTIAL_EXIT,
    /**
     *   Tell the writer to shut down all the channel bound to it for exiting the whole application
     */
    EXIT
}
