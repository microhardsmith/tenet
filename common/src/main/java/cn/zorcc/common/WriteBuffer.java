package cn.zorcc.common;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;

/**
 *   Direct memory WriteBuffer, not thread-safe, custom Resizer could be chosen to modify the default expansion mechanism
 */
public final class WriteBuffer implements AutoCloseable, Writer {
    private MemorySegment segment;
    private long size;
    private long writeIndex;
    private final WriteBufferPolicy policy;

    /**
     *   Create a writeBuffer using custom policy
     */
    public WriteBuffer(MemorySegment segment, WriteBufferPolicy policy) {
        this.segment = segment;
        this.size = segment.byteSize();
        this.writeIndex = Constants.ZERO;
        this.policy = policy;
    }

    /**
     *   Create a writeBuffer using default policy
     */
    public WriteBuffer(Arena arena, long size) {
        this(arena.allocateArray(ValueLayout.JAVA_BYTE, size), new DefaultWriteBufferPolicy(arena));
    }

    public MemorySegment segment() {
        return segment;
    }

    public long size() {
        return size;
    }

    public void update(MemorySegment segment) {
        this.segment = segment;
        this.size = segment.byteSize();
    }

    public long writeIndex() {
        return writeIndex;
    }

    public void setWriteIndex(long newIndex) {
        writeIndex = newIndex;
    }

    public void resize(long nextIndex) {
        if(nextIndex > size) {
            policy.resize(this, nextIndex);
        }
    }

    @Override
    public void writeByte(byte b) {
        long nextIndex = writeIndex + 1;
        resize(nextIndex);
        NativeUtil.setByte(segment, writeIndex, b);
        writeIndex = nextIndex;
    }

    @Override
    public void writeBytes(byte... bytes) {
        long nextIndex = writeIndex + bytes.length;
        resize(nextIndex);
        for(int i = 0; i < bytes.length; i++) {
            NativeUtil.setByte(segment, writeIndex + i, bytes[i]);
        }
        writeIndex = nextIndex;
    }

    @Override
    public void writeBytes(byte[] data, int offset, int len) {
        if(len < Constants.ZERO || offset + len > data.length) {
            throw new FrameworkException(ExceptionType.NATIVE, "Index out of bound");
        }
        long nextIndex = writeIndex + len;
        for(int i = 0; i < len; i++) {
            NativeUtil.setByte(segment, writeIndex + i, data[offset + i]);
        }
        writeIndex = nextIndex;
    }

    public void writeBytes(byte[] bytes, int minWidth) {
        if(minWidth <= bytes.length) {
            writeBytes(bytes);
        }else {
            long nextIndex = writeIndex + minWidth;
            resize(nextIndex);
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
        resize(nextIndex);
        NativeUtil.setShort(segment, writeIndex, s);
        writeIndex = nextIndex;
    }

    public void writeInt(int i) {
        long nextIndex = writeIndex + 4;
        resize(nextIndex);
        NativeUtil.setInt(segment, writeIndex, i);
        writeIndex = nextIndex;
    }

    public void writeLong(long l) {
        long nextIndex = writeIndex + 8L;
        resize(nextIndex);
        NativeUtil.setLong(segment, writeIndex, l);
        writeIndex = nextIndex;
    }

    public void writeCStr(String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        long nextIndex = writeIndex + bytes.length + 1;
        resize(nextIndex);
        for(int i = 0; i < bytes.length; i++) {
            NativeUtil.setByte(segment, writeIndex + i, bytes[i]);
        }
        NativeUtil.setByte(segment, writeIndex + bytes.length, Constants.NUT);
        writeIndex = nextIndex;
    }

    public void write(MemorySegment memorySegment) {
        if(memorySegment != null) {
            long len = memorySegment.byteSize();
            long nextIndex = writeIndex + len;
            resize(nextIndex);
            MemorySegment.copy(memorySegment, 0L, segment, writeIndex, len);
            writeIndex = nextIndex;
        }else {
            throw new FrameworkException(ExceptionType.NATIVE, "Writing null segment");
        }
    }

    public void setByte(long index, byte value) {
        if(index + 1 > writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "Index out of bound");
        }
        NativeUtil.setByte(segment, index, value);
    }

    public void setShort(long index, short value) {
        if(index + 2 > writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "Index out of bound");
        }
        NativeUtil.setShort(segment, index, value);
    }

    public void setInt(long index, int value) {
        if(index + 4 > writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "Index out of bound");
        }
        NativeUtil.setInt(segment, index, value);
    }

    public void setLong(long index, long value) {
        if(index + 8 > writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "Index out of bound");
        }
        NativeUtil.setLong(segment, index, value);
    }

    /**
     *   Return current written segment, from 0 ~ writeIndex
     */
    public MemorySegment content() {
        return writeIndex == size ? segment : segment.asSlice(Constants.ZERO, writeIndex);
    }

    /**
     *   Truncate from the beginning of the segment to the offset, return a new WriteBuffer
     */
    public WriteBuffer truncate(long offset) {
        if(offset > writeIndex) {
            throw new RuntimeException("truncate overflow");
        }
        WriteBuffer w = new WriteBuffer(segment.asSlice(offset, size - offset), policy);
        w.setWriteIndex(writeIndex - offset);
        return w;
    }

    @Override
    public void close() {
        policy.close(this);
    }

    @Override
    public String asString() {
        byte[] bytes = content().toArray(ValueLayout.JAVA_BYTE);
        writeIndex = Constants.ZERO;
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public ReadBuffer asReadBuffer() {
        MemorySegment memorySegment = MemorySegment.ofArray(content().toArray(ValueLayout.JAVA_BYTE));
        writeIndex = Constants.ZERO;
        return new ReadBuffer(memorySegment);
    }
}
