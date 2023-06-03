package cn.zorcc.common;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;

/**
 *   读直接内存缓冲区,非线程安全
 */
public final class ReadBuffer implements AutoCloseable {
    private final Arena arena;
    private final MemorySegment segment;
    private final long len;
    /**
     *   当前已读取的index
     */
    private long readIndex;
    /**
     *   当前已写入的index
     */
    private long writeIndex;

    /**
     *   用于构建新的空白的readBuffer
     */
    public ReadBuffer(long size) {
        this.arena = Arena.openConfined();
        this.segment = arena.allocateArray(ValueLayout.JAVA_BYTE, size);
        this.len = size;
        this.readIndex = 0L;
        this.writeIndex = 0L;
    }

    /**
     *   用于从WriteBuffer中转化
     */
    public ReadBuffer(Arena arena, MemorySegment segment) {
        this.arena = arena;
        this.segment = segment;
        this.len = segment.byteSize();
        this.readIndex = 0L;
        this.writeIndex = segment.byteSize();
    }

    public long available() {
        return writeIndex - readIndex;
    }

    public void setReadIndex(long index) {
        if(index < 0 || index > writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "ReadIndex out of bound");
        }
        readIndex = index;
    }

    public void setWriteIndex(long index) {
        if(index > segment.byteSize()) {
            throw new FrameworkException(ExceptionType.NATIVE, "ReadBuffer write index overflow");
        }
        writeIndex = index;
    }

    public long readIndex() {
        return readIndex;
    }

    public long writeIndex() {
        return writeIndex;
    }

    public byte readByte() {
        long nextIndex = readIndex + 1;
        if(nextIndex > writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        byte b = NativeUtil.getByte(segment, readIndex);
        readIndex = nextIndex;
        return b;
    }

    public byte[] readBytes(int len) {
        long nextIndex = readIndex + len;
        if(nextIndex > writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        byte[] result = segment.asSlice(readIndex, len).toArray(ValueLayout.JAVA_BYTE);
        readIndex = nextIndex;
        return result;
    }

    public short readShort() {
        long nextIndex = readIndex + 2;
        if(nextIndex > writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        short s = NativeUtil.getShort(segment, readIndex);
        readIndex = nextIndex;
        return s;
    }

    public int readInt() {
        long nextIndex = readIndex + 4;
        if(nextIndex > writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        int i = NativeUtil.getInt(segment, readIndex);
        readIndex = nextIndex;
        return i;
    }

    public long readLong() {
        long nextIndex = readIndex + 8;
        if(nextIndex > writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        long l = NativeUtil.getByte(segment, readIndex);
        readIndex = nextIndex;
        return l;
    }

    /**
     *   读取当前内存块直到读取到指定分隔符,返回已读取的内容（不包含分隔符）,如果未获取到则返回null
     */
    public byte[] readUntil(byte sep) {
        long currentIndex = readIndex;
        while (currentIndex < writeIndex) {
            byte b = NativeUtil.getByte(segment, currentIndex);
            if(b == sep) {
                byte[] result = currentIndex == readIndex ? Constants.EMPTY_BYTES : segment.asSlice(readIndex, currentIndex - readIndex).toArray(ValueLayout.JAVA_BYTE);
                readIndex = currentIndex + 1;
                return result;
            }else {
                currentIndex += 1;
            }
        }
        readIndex = writeIndex;
        return null;
    }

    public byte[] readUntil(byte sep1, byte sep2) {
        long currentIndex = readIndex;
        long maxIndex = writeIndex - 1;
        while (currentIndex < maxIndex) {
            byte b = NativeUtil.getByte(segment, currentIndex);
            if(b == sep1 && NativeUtil.getByte(segment, currentIndex + 1) == sep2) {
                byte[] result = currentIndex == readIndex ? Constants.EMPTY_BYTES : segment.asSlice(readIndex, currentIndex - readIndex).toArray(ValueLayout.JAVA_BYTE);
                readIndex = currentIndex + 2;
                return result;
            }else {
                currentIndex += 1;
            }
        }
        readIndex = maxIndex;
        return null;
    }

    /**
     *   Read a C style string from current readBuffer
     */
    public String readCStr() {
        byte[] bytes = readUntil(Constants.NUT);
        if(bytes == null || bytes.length == 0) {
            return null;
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public long len() {
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
