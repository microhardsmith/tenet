package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.util.NativeUtil;

public final class WriterConfig {
    /**
     *  WriterCount determines how many writer thread will be created
     */
    private int writerCount = Math.max(NativeUtil.getCpuCores() >> 1, 4);
    /**
     *  The write buffer initial size for each writer instance
     */
    private int writeBufferSize = 64 * Constants.KB;
    /**
     *  Writer map size, normally 1024 would be enough
     *  if your application has to maintain thousands of connections at the same time, then you can increase this value appropriately
     */
    private int mapSize = Constants.KB;

    public int getWriterCount() {
        return writerCount;
    }

    public void setWriterCount(int writerCount) {
        this.writerCount = writerCount;
    }

    public int getWriteBufferSize() {
        return writeBufferSize;
    }

    public void setWriteBufferSize(int writeBufferSize) {
        this.writeBufferSize = writeBufferSize;
    }

    public int getMapSize() {
        return mapSize;
    }

    public void setMapSize(int mapSize) {
        this.mapSize = mapSize;
    }
}
