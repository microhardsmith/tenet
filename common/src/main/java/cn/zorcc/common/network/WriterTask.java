package cn.zorcc.common.network;

/**
 *   Task entity for writer thread
 */
public record WriterTask(
        WriterTaskType type,
        Channel channel,
        Object msg,
        WriterCallback callback
) {
    enum WriterTaskType {
        /**
         *   Bind a socket with target channel
         */
        INITIATE,
        /**
         *   Channel become writable again
         */
        WRITABLE,
        /**
         *   Send a mix of several msg
         */
        MULTIPLE_MSG,
        /**
         *   Send a single msg
         */
        MSG,
        /**
         *   Tell the channel to perform shutdown operation
         */
        SHUTDOWN,
        /**
         *   Tell the writer thread to remove target channel
         */
        REMOVE,
        /**
         *   Exit writer thread
         */
        EXIT,
    }
}
