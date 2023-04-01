package cn.zorcc.common;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 *   读直接内存缓冲区,非线程安全
 */
public final class ReadBuffer implements AutoCloseable {
    private final Arena arena;
    private final MemorySegment segment;
    private final int len;
    private long readIndex;
    private long writeIndex;

    public ReadBuffer(int size) {
        this.arena = Arena.openConfined();
        this.segment = arena.allocateArray(ValueLayout.JAVA_BYTE, size);
        this.len = size;
        this.readIndex = 0L;
        this.writeIndex = 0L;
    }

    public ReadBuffer(Arena arena, MemorySegment segment) {
        this.arena = arena;
        this.segment = segment;
        this.len = (int) segment.byteSize();
        this.readIndex = 0L;
        this.writeIndex = segment.byteSize();
    }

    public long available() {
        return writeIndex - readIndex;
    }

    public void setWriteIndex(long writeIndex) {
        this.writeIndex = writeIndex;
    }

    public long readIndex() {
        return readIndex;
    }

    public long writeIndex() {
        return writeIndex;
    }

    public byte readByte() {
        long nextIndex = readIndex + 1;
        if(nextIndex > segment.byteSize()) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        byte b = NativeUtil.getByte(segment, readIndex);
        readIndex = nextIndex;
        return b;
    }

    public short readShort() {
        long nextIndex = readIndex + 2;
        if(nextIndex > segment.byteSize()) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        short s = NativeUtil.getShort(segment, readIndex);
        readIndex = nextIndex;
        return s;
    }

    public int readInt() {
        long nextIndex = readIndex + 4;
        if(nextIndex > segment.byteSize()) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        int i = NativeUtil.getInt(segment, readIndex);
        readIndex = nextIndex;
        return i;
    }

    public long readLong() {
        long nextIndex = readIndex + 8;
        if(nextIndex > segment.byteSize()) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        long l = NativeUtil.getByte(segment, readIndex);
        readIndex = nextIndex;
        return l;
    }

    public int len() {
        return len;
    }

    public MemorySegment segment() {
        return segment;
    }

    public boolean remains() {
        return readIndex != writeIndex;
    }

    public MemorySegment remaining() {
        return remains() ? segment.asSlice(readIndex, writeIndex) : null;
    }

    public void reset() {
        readIndex = 0L;
        writeIndex = 0L;
    }

    @Override
    public void close() {
        arena.close();
    }
}
