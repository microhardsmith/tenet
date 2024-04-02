package cn.zorcc.common.structure;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.bindings.SystemBinding;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 *   Direct memory ReadBuffer, not thread-safe, ReadBuffer is read-only, shouldn't be modified directly
 *   Note that writeIndex is a mutable field, because
 */
public final class ReadBuffer {

    private final MemorySegment segment;
    private final long size;
    private long readIndex;

    public ReadBuffer(MemorySegment segment) {
        this.segment = segment;
        this.size = segment.byteSize();
        this.readIndex = 0L;
    }


    public long readIndex() {
        return readIndex;
    }

    public void setReadIndex(long nextIndex) {
        if(nextIndex < 0L || nextIndex > size) {
            throw new FrameworkException(ExceptionType.NATIVE, "ReadIndex out of bound");
        }
        readIndex = nextIndex;
    }

    public long size() {
        return size;
    }

    public long available() {
        return size - readIndex;
    }

    public byte readByte() {
        long nextIndex = readIndex + Constants.BYTE_SIZE;
        if(nextIndex > size) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        byte b = NativeUtil.getByte(segment, readIndex);
        readIndex = nextIndex;
        return b;
    }

    /**
     *   The returned segment would have the same scope as current ReadBuffer
     */
    public MemorySegment readSegment(long count) {
        long nextIndex = readIndex + count * Constants.BYTE_SIZE;
        if(nextIndex > size) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        MemorySegment result = segment.asSlice(readIndex, count);
        readIndex = nextIndex;
        return result;
    }

    /**
     *   The returning segment would be converted to on-heap memorySegment TODO removal
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
        return readSegment(count).toArray(ValueLayout.JAVA_BYTE);
    }

    public short readShort() {
        long nextIndex = readIndex + Constants.SHORT_SIZE;
        if(nextIndex > size) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        short s = NativeUtil.getShort(segment, readIndex);
        readIndex = nextIndex;
        return s;
    }

    public int readInt() {
        long nextIndex = readIndex + Constants.INT_SIZE;
        if(nextIndex > size) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        int i = NativeUtil.getInt(segment, readIndex);
        readIndex = nextIndex;
        return i;
    }

    public long readLong() {
        long nextIndex = readIndex + Constants.LONG_SIZE;
        if(nextIndex > size) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        long l = NativeUtil.getLong(segment, readIndex);
        readIndex = nextIndex;
        return l;
    }

    public float readFloat() {
        long nextIndex = readIndex + Constants.FLOAT_SIZE;
        if(nextIndex > size) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        float f = NativeUtil.getFloat(segment, readIndex);
        readIndex = nextIndex;
        return f;
    }

    public double readDouble() {
        long nextIndex = readIndex + Constants.DOUBLE_SIZE;
        if(nextIndex > size) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        double d = NativeUtil.getDouble(segment, readIndex);
        readIndex = nextIndex;
        return d;
    }

    /**
     *   Shift current readIndex to the searchIndex with offset, return the searched bytes
     */
    private byte[] shiftData(long searchIndex, long shift) {
        byte[] result = searchIndex == readIndex ? Constants.EMPTY_BYTES : segment.asSlice(readIndex, searchIndex - readIndex).toArray(ValueLayout.JAVA_BYTE);
        readIndex = searchIndex + shift;
        return result;
    }

    /**
     *   Creating a byte search pattern with SIMD inside a register algorithm
     */
    public static long compilePattern(byte b) {
        long pattern = b & 0xFFL;
        return pattern
                | (pattern << 8)
                | (pattern << 16)
                | (pattern << 24)
                | (pattern << 32)
                | (pattern << 40)
                | (pattern << 48)
                | (pattern << 56);
    }

    private static int searchPattern(long data, long pattern) {
        long input = data ^ pattern;
        long tmp = (input & 0x7F7F7F7F7F7F7F7FL) + 0x7F7F7F7F7F7F7F7FL;
        tmp = ~(tmp | input | 0x7F7F7F7F7F7F7F7FL);
        return (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN ? Long.numberOfLeadingZeros(tmp) : Long.numberOfTrailingZeros(tmp)) >>> 3;
    }

    private static long linearSearch(MemorySegment segment, long startIndex, long endIndex, byte target) {
        for(long cur = startIndex; cur < endIndex; cur++) {
            if(NativeUtil.getByte(segment, cur) == target) {
                return cur;
            }
        }
        return Long.MIN_VALUE;
    }

    public byte[] readUntil(byte sep) {
        long searchIndex = linearSearch(segment, readIndex, size, sep);
        return searchIndex < 0 ? null : shiftData(searchIndex, 1L);
    }

    private static long linearSearch(MemorySegment segment, long startIndex, long endIndex, byte target1, byte target2) {
        for(long cur = startIndex; cur < endIndex; cur++) {
            if(NativeUtil.getByte(segment, cur) == target1 && cur < endIndex - 1 && NativeUtil.getByte(segment, cur + 1) == target2) {
                return cur;
            }
        }
        return Long.MIN_VALUE;
    }

    public byte[] readUntil(byte firstSep, byte secondSep) {
        long searchIndex = linearSearch(segment, readIndex, size, firstSep, secondSep);
        return searchIndex < 0 ? null : shiftData(searchIndex, 2L);
    }

    private static long searchFirstByte(MemorySegment segment, long startIndex, long endIndex, long pattern, byte target) {
        final long available = endIndex - startIndex;
        if(available < Long.BYTES) {
            return linearSearch(segment, startIndex, endIndex, target);
        }
        // check the first part
        int r = searchPattern(NativeUtil.getLong(segment, startIndex), pattern);
        if(r < Long.BYTES) {
            return startIndex + r;
        }
        // check the middle part
        final long address = segment.address();
        long index = Long.BYTES - ((address + startIndex) & (Long.BYTES - 1)) + startIndex;
        for( ; index <= endIndex - Long.BYTES; index += Long.BYTES) {
            r = searchPattern(NativeUtil.getLong(segment, index), pattern);
            if(r < Long.BYTES) {
                return index + r;
            }
        }
        // check the last part
        if(index < endIndex) {
            r = searchPattern(NativeUtil.getLong(segment, endIndex - Long.BYTES), pattern);
            if(r < Long.BYTES) {
                return endIndex - Long.BYTES + r;
            }
        }
        return Long.MIN_VALUE;
    }

    public byte[] readPattern(long pattern, byte sep) {
        long searchIndex = searchFirstByte(segment, readIndex, size, pattern, sep);
        return searchIndex < 0 ? null : shiftData(searchIndex, 1L);
    }

    public byte[] readPattern(long pattern, byte firstSep, byte secondSep) {
        long startIndex = readIndex;
        for( ; ; ) {
            long index = searchFirstByte(segment, startIndex, size, pattern, firstSep);
            if(index < 0) {
                return null;
            }else if(index < size - 1 && NativeUtil.getByte(segment, index + 1) == secondSep) {
                return shiftData(index, 2L);
            }else {
                startIndex = index + 1;
            }
        }
    }

    /**
     *   Read a C style UTF-8 string from the current readBuffer
     */
    public String readStr(Charset charset) {
        long available = size - readIndex;
        long r = SystemBinding.strlen(segment.asSlice(readIndex, available), available);
        if(r == available) {
            return null;
        }
        int len = Math.toIntExact(r);
        byte[] bytes = new byte[len];
        MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, readIndex, bytes, 0, len);
        readIndex += len + 1;
        return new String(bytes, charset);
    }

    public String readStr() {
        return readStr(StandardCharsets.UTF_8);
    }

    /**
     *   Read multiple C style string from current readBuffer, there must be an external NUT to indicate the end
     */
    public List<String> readMultipleStr(Charset charset) {
        List<String> result = new ArrayList<>();
        for( ; ; ) {
            String s = readStr(charset);
            if(s == null) {
                return result;
            }else {
                result.add(s);
            }
        }
    }

    public List<String> readMultipleStr() {
        return readMultipleStr(StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return new String(segment.toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8);
    }
}
