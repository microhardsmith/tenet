package cn.zorcc.common;

import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 *   Direct memory ReadBuffer, not thread-safe, ReadBuffer is read-only, shouldn't be modified directly
 *   Note that writeIndex is a mutable field, because
 */
public final class ReadBuffer {
    /**
     *   Actual memory segment part
     */
    private final MemorySegment segment;
    /**
     *   Actual memory size, should be equal to segment.byteSize()
     */
    private final long size;
    /**
     *   Read index starting from 0, step by reading
     */
    private long readIndex;

    public ReadBuffer(MemorySegment segment) {
        this.segment = segment;
        this.size = segment.byteSize();
        this.readIndex = 0;
    }


    public long readIndex() {
        return readIndex;
    }

    public void setReadIndex(long index) {
        if(index < 0 || index > size) {
            throw new FrameworkException(ExceptionType.NATIVE, "ReadIndex out of bound");
        }
        readIndex = index;
    }

    public long size() {
        return size;
    }

    public long available() {
        return size - readIndex;
    }

    public byte readByte() {
        long nextIndex = readIndex + NativeUtil.getByteSize();
        if(nextIndex > size) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        byte b = NativeUtil.getByte(segment, readIndex);
        readIndex = nextIndex;
        return b;
    }

    /**
     *   The returning segment would have the same scope as current ReadBuffer
     */
    public MemorySegment readSegment(long count) {
        long nextIndex = readIndex + count * NativeUtil.getByteSize();
        if(nextIndex > size) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        MemorySegment result = segment.asSlice(readIndex, count);
        readIndex = nextIndex;
        return result;
    }

    /**
     *   The returning segment would be converted to on-heap memorySegment
     */
    public MemorySegment readHeapSegment(long count) {
        MemorySegment m = readSegment(count);
        if(m.isNative()) {
            long len = m.byteSize();
            byte[] bytes = new byte[(int) len];
            MemorySegment h = MemorySegment.ofArray(bytes);
            MemorySegment.copy(m, 0, h, 0, len);
            return h;
        }else {
            return m;
        }
    }

    public byte[] readBytes(long count) {
        long nextIndex = readIndex + count * NativeUtil.getByteSize();
        if(nextIndex > size) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        byte[] result = segment.asSlice(readIndex, count).toArray(ValueLayout.JAVA_BYTE);
        readIndex = nextIndex;
        return result;
    }

    public short readShort() {
        long nextIndex = readIndex + NativeUtil.getShortSize();
        if(nextIndex > size) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        short s = NativeUtil.getShort(segment, readIndex);
        readIndex = nextIndex;
        return s;
    }

    public int readInt() {
        long nextIndex = readIndex + NativeUtil.getIntSize();
        if(nextIndex > size) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        int i = NativeUtil.getInt(segment, readIndex);
        readIndex = nextIndex;
        return i;
    }

    public long readLong() {
        long nextIndex = readIndex + NativeUtil.getLongSize();
        if(nextIndex > size) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        long l = NativeUtil.getLong(segment, readIndex);
        readIndex = nextIndex;
        return l;
    }

    /**
     *   Read until target several separators occurred in the readBuffer
     *   If no separators were found in the sequence, no bytes would be read and null would be returned
     */
    public byte[] readUntil(byte... separators) {
        for(long cur = readIndex; cur <= size - separators.length; cur++) {
            if(NativeUtil.matches(segment, cur, separators)) {
                byte[] result = cur == readIndex ? Constants.EMPTY_BYTES : segment.asSlice(readIndex, cur - readIndex).toArray(ValueLayout.JAVA_BYTE);
                readIndex = cur + separators.length;
                return result;
            }
        }
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

    /**
     *   Read multiple C style string from current readBuffer, there must be an external NUT to indicate the end
     */
    public List<String> readMultipleCStr() {
        List<String> result = new ArrayList<>();
        for( ; ; ) {
            byte[] bytes = readUntil(Constants.NUT);
            if(bytes == null || bytes.length == 0) {
                return result;
            }else {
                result.add(new String(bytes, StandardCharsets.UTF_8));
            }
        }
    }

    /**
     *   Read the rest part of current ReadBuffer
     */
    public byte[] readAll() {
        if(readIndex == size) {
            return Constants.EMPTY_BYTES;
        }else {
            return segment.asSlice(readIndex, size - readIndex).toArray(ValueLayout.JAVA_BYTE);
        }
    }

    @Override
    public String toString() {
        return new String(segment.toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8);
    }
}
