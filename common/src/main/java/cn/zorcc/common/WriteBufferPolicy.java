package cn.zorcc.common;


public interface WriteBufferPolicy {
    /**
     *   Resize target writeBuffer to contain more bytes than nextIndex
     */
    void resize(WriteBuffer writeBuffer, long nextIndex);

    /**
     *   Close current writeBuffer after using
     */
    void close(WriteBuffer writeBuffer);
}
