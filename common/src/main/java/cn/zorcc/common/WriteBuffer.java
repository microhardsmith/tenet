package cn.zorcc.common;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 *   写直接内存缓冲区,非线程安全
 */
public final class WriteBuffer implements AutoCloseable {
    private Arena arena;
    private MemorySegment segment;
    private long writeIndex;

    public WriteBuffer(int size) {
        this.arena = Arena.openConfined();
        this.segment = arena.allocateArray(ValueLayout.JAVA_BYTE, size);
        this.writeIndex = 0L;
    }

    public void writeByte(byte b) {
        long nextIndex = writeIndex + 1;
        if(nextIndex > segment.byteSize()) {
            resize(nextIndex);
        }
        NativeUtil.setByte(segment, writeIndex, b);
        writeIndex = nextIndex;
    }

    public void writeBytes(byte... bytes) {
        long nextIndex = writeIndex + bytes.length;
        if(nextIndex > segment.byteSize()) {
            resize(nextIndex);
        }
        for(int i = 0; i < bytes.length; i++) {
            NativeUtil.setByte(segment, writeIndex + i, bytes[i]);
        }
        writeIndex = nextIndex;
    }

    public void writeBytes(byte[] bytes, int minWidth) {
        if(minWidth <= bytes.length) {
            writeBytes(bytes);
        }else {
            long nextIndex = writeIndex + minWidth;
            if(nextIndex > segment.byteSize()) {
                resize(nextIndex);
            }
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
        long nextIndex = writeIndex + 2;
        if(nextIndex > segment.byteSize()) {
            resize(nextIndex);
        }
        NativeUtil.setShort(segment, writeIndex, s);
        writeIndex = nextIndex;
    }

    public void writeInt(int i) {
        long nextIndex = writeIndex + 4;
        if(nextIndex > segment.byteSize()) {
            resize(nextIndex);
        }
        NativeUtil.setInt(segment, writeIndex, i);
        writeIndex = nextIndex;
    }

    public void writeLong(long l) {
        long nextIndex = writeIndex + 8;
        if(nextIndex > segment.byteSize()) {
            resize(nextIndex);
        }
        NativeUtil.setLong(segment, writeIndex, l);
        writeIndex = nextIndex;
    }

    public void write(MemorySegment memorySegment) {
        if(memorySegment != null) {
            long len = memorySegment.byteSize();
            long nextIndex = writeIndex + len;
            if(nextIndex > segment.byteSize()) {
                resize(nextIndex);
            }
            MemorySegment.copy(memorySegment, 0L, segment, writeIndex, len);
            writeIndex = nextIndex;
        }else {
            throw new FrameworkException(ExceptionType.NATIVE, "Writing null segment");
        }
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
     *   扩容当前segment到至少可容纳nextIndex大小
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
