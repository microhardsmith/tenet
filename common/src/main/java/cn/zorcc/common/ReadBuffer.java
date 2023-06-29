package cn.zorcc.common;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;

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
        this.readIndex = Constants.ZERO;
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

    public long readIndex() {
        return readIndex;
    }

    public byte readByte() {
        long nextIndex = readIndex + 1;
        if(nextIndex > size) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        byte b = NativeUtil.getByte(segment, readIndex);
        readIndex = nextIndex;
        return b;
    }

    public byte[] readBytes(int count) {
        long nextIndex = readIndex + count;
        if(nextIndex > size) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        byte[] result = segment.asSlice(readIndex, count).toArray(ValueLayout.JAVA_BYTE);
        readIndex = nextIndex;
        return result;
    }

    public short readShort() {
        long nextIndex = readIndex + 2;
        if(nextIndex > size) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        short s = NativeUtil.getShort(segment, readIndex);
        readIndex = nextIndex;
        return s;
    }

    public int readInt() {
        long nextIndex = readIndex + 4;
        if(nextIndex > size) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        int i = NativeUtil.getInt(segment, readIndex);
        readIndex = nextIndex;
        return i;
    }

    public long readLong() {
        long nextIndex = readIndex + 8;
        if(nextIndex > size) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        long l = NativeUtil.getByte(segment, readIndex);
        readIndex = nextIndex;
        return l;
    }

    /**
     *   read until sep occurred, if not sep found in the following bytes, no bytes would be read and null would be returned
     */
    public byte[] readUntil(byte sep) {
        ResizableByteArray resizableByteArray = new ResizableByteArray();
        byte b;
        for (long cur = readIndex; cur < size; cur++, resizableByteArray.write(b)) {
            b = NativeUtil.getByte(segment, cur);
            if(b == sep) {
                readIndex = cur + 1;
                return resizableByteArray.toArray();
            }
        }
        return null;
    }

    /**
     *   read until several separators occurred, if not sep found in the following bytes, no bytes would be read and null would be returned
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
     *   Return the rest part of current ReadBuffer, before calling this method, readerIndex should be checked if there are still some data remaining
     */
    public MemorySegment rest() {
        return readIndex == Constants.ZERO ? segment : segment.asSlice(readIndex, size - readIndex);
    }
}
