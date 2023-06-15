package cn.zorcc.common.network;

public record WriterTask(
        Channel channel,
        Object msg
) {
    /**
     *   Indicating the writer thread should be interrupted
     */
    public static final WriterTask INTERRUPT_TASK = new WriterTask(null, null);
}
