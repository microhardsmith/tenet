package cn.zorcc.common;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;

/**
 *   写直接内存缓冲区,非线程安全
 */
public final class WriteBuffer implements AutoCloseable {
    private Arena arena;
    private MemorySegment segment;
    private long writeIndex;

    public WriteBuffer(long size) {
        this.arena = Arena.openConfined();
        this.segment = arena.allocateArray(ValueLayout.JAVA_BYTE, size);
        this.writeIndex = 0L;
    }

    public long writeIndex() {
        return writeIndex;
    }

    public void writeByte(byte b) {
        long nextIndex = ensureCapacity(1L);
        NativeUtil.setByte(segment, writeIndex, b);
        writeIndex = nextIndex;
    }

    public void writeBytes(byte... bytes) {
        long nextIndex = ensureCapacity(bytes.length);
        for(int i = 0; i < bytes.length; i++) {
            NativeUtil.setByte(segment, writeIndex + i, bytes[i]);
        }
        writeIndex = nextIndex;
    }

    public void writeBytes(byte[] bytes, int minWidth) {
        if(minWidth <= bytes.length) {
            writeBytes(bytes);
        }else {
            long nextIndex = ensureCapacity(minWidth);
            for(int i = 0; i < bytes.length; i++) {
                NativeUtil.setByte(segment, writeIndex + i, bytes[i]);
            }
            for(int i = bytes.length; i < minWidth; i++) {
                NativeUtil.setByte(segment, writeIndex + i, Constants.SPACE);
            }
            writeIndex = nextIndex;
        }

    }

    public void writeShort(short s) {
        long nextIndex = ensureCapacity(2L);
        NativeUtil.setShort(segment, writeIndex, s);
        writeIndex = nextIndex;
    }

    public void writeInt(int i) {
        long nextIndex = ensureCapacity(4L);
        NativeUtil.setInt(segment, writeIndex, i);
        writeIndex = nextIndex;
    }

    public void writeLong(long l) {
        long nextIndex = ensureCapacity(8L);
        NativeUtil.setLong(segment, writeIndex, l);
        writeIndex = nextIndex;
    }

    public void writeCStr(String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        long nextIndex = ensureCapacity(bytes.length + 1);
        for(int i = 0; i < bytes.length; i++) {
            NativeUtil.setByte(segment, writeIndex + i, bytes[i]);
        }
        NativeUtil.setByte(segment, writeIndex + bytes.length, Constants.NUT);
        writeIndex = nextIndex;
    }

    public void write(MemorySegment memorySegment) {
        if(memorySegment != null) {
            long len = memorySegment.byteSize();
            long nextIndex = ensureCapacity(len);
            MemorySegment.copy(memorySegment, 0L, segment, writeIndex, len);
            writeIndex = nextIndex;
        }else {
            throw new FrameworkException(ExceptionType.NATIVE, "Writing null segment");
        }
    }

    public void setInt(long index, int value) {
        if(index + 4 > writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "Index out of bound");
        }
        NativeUtil.setInt(segment, index, value);
    }

    /**
     *   获取当前已写入的segment
     */
    public MemorySegment segment() {
        return writeIndex == segment.byteSize() ? segment : segment.asSlice(0L, writeIndex);
    }

    /**
     *   截断最开始的count个元素
     */
    public void truncate(long count) {
        if(count >= writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "Couldn't truncate after write index");
        }
        long newWriteIndex = writeIndex - count;
        segment = segment.asSlice(count, newWriteIndex);
        writeIndex = newWriteIndex;
    }

    /**
     *   重置当前writeBuffer,不清除已写入的内容
     */
    public void reset() {
        writeIndex = 0L;
    }

    /**
     *   将当前WriteBuffer转化为ReadBuffer
     */
    public ReadBuffer toReadBuffer() {
        return new ReadBuffer(arena, segment());
    }

    @Override
    public void close() {
        arena.close();
    }

    /**
     *   Ensure current segment capacity
     */
    public long ensureCapacity(long cap) {
        long nextIndex = writeIndex + cap;
        if(nextIndex > segment.byteSize()) {
            resize(nextIndex);
        }
        return nextIndex;
    }

    /**
     *   Resize current segment to fit at least nextIndex bytes
     */
    private void resize(long nextIndex) {
        long newSize = segment.byteSize();
        while (newSize > 0 && newSize < nextIndex) {
            // 按照每次2倍的基数进行扩容
            newSize = newSize << 1;
        }
        if(newSize < 0) {
            throw new FrameworkException(ExceptionType.NATIVE, "MemorySize overflow");
        }
        Arena newArena = Arena.openConfined();
        MemorySegment newSegment = newArena.allocateArray(ValueLayout.JAVA_BYTE, newSize);
        MemorySegment.copy(segment, 0L, newSegment, 0L, writeIndex);
        arena.close();
        arena = newArena;
        segment = newSegment;
    }
}
