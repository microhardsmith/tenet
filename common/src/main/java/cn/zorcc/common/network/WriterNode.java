package cn.zorcc.common.network;

import java.lang.foreign.MemorySegment;
import java.time.Duration;

public sealed interface WriterNode permits ProtocolWriterNode {
    /**
     *   This function would be invoked when channel wants to send a msg
     */
    void onMsg(MemorySegment reserved, WriterTask writerTask);

    /**
     *   This function would be invoked when channel wants to send multiple msgs
     */
    void onMultipleMsg(MemorySegment reserved, WriterTask writerTask);

    /**
     *   This function would be invoked when channel become writable
     */
    void onWritable(WriterTask writerTask);

    /**
     *   This function would be invoked when channel wants to shutdown
     */
    void onShutdown(WriterTask writerTask);

    /**
     *   This function would be invoked when channel wants to force-close
     */
    void onClose(WriterTask writerTask);

    /**
     *   Exit current writerNode
     */
    void exit(Duration duration);
}
