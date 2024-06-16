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
 *   TODO There could be some vectorized-version searching algorithm implemented by using ByteVector, after Vector-API got finalized
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

    public long currentIndex() {
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
    private MemorySegment shiftData(long searchIndex, long shift) {
        MemorySegment result = searchIndex == readIndex ? MemorySegment.NULL : segment.asSlice(readIndex, searchIndex - readIndex);
        readIndex = searchIndex + shift;
        return result;
    }

    /**
     *   Creating a byte search pattern with SIMD inside a register algorithm, in short, SWAR search algorithm
     */
    public static long compilePattern(byte b) {
        long pattern = Byte.toUnsignedLong(b);
        return pattern
                | (pattern << 8)
                | (pattern << 16)
                | (pattern << 24)
                | (pattern << 32)
                | (pattern << 40)
                | (pattern << 48)
                | (pattern << 56);
    }

    /**
     *   Using pattern search for target byte in current ReadBuffer without branch prediction, return the target index, Long.Bytes if not found
     */
    public static int searchPattern(long data, long pattern, ByteOrder byteOrder) {
        long mask = data ^ pattern; // this single line is the major cost that we are about 10% slower than the JDK version
        long match = (mask - 0x0101010101010101L) & ~mask & 0x8080808080808080L;
        if(byteOrder == ByteOrder.BIG_ENDIAN) {
            return Long.numberOfLeadingZeros(match) >>> 3;
        }else if(byteOrder == ByteOrder.LITTLE_ENDIAN) {
            return Long.numberOfTrailingZeros(match) >>> 3;
        }else {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED);
        }
    }

    /**
     *   LinearSearch the target memorySegment, from startIndex to endIndex, of target byte, return its index, or -1 if not found
     */
    public static long linearSearch(MemorySegment segment, long startIndex, long endIndex, byte target) {
        final int start = Math.toIntExact(startIndex);
        final int end = Math.toIntExact(endIndex);
        for(int cur = start; cur < end; cur++) {
            if(NativeUtil.getByte(segment, cur) == target) {
                return cur;
            }
        }
        return -1L;
    }

    /**
     *   Linear search target sep in current readBuffer
     */
    public MemorySegment readUntil(byte sep) {
        long searchIndex = linearSearch(segment, readIndex, size, sep);
        return searchIndex < 0 ? null : shiftData(searchIndex, 1L);
    }

    /**
     *   LinearSearch the target memorySegment, from startIndex to endIndex, of target byte1 and byte2, return its index, or -1 if not found
     */
    public static long linearSearch(MemorySegment segment, long startIndex, long endIndex, byte target1, byte target2) {
        final int start = Math.toIntExact(startIndex);
        final int end = Math.toIntExact(endIndex);
        for(int cur = start; cur < end; cur++) {
            if(NativeUtil.getByte(segment, cur) == target1 && cur < endIndex - 1 && NativeUtil.getByte(segment, cur + 1) == target2) {
                return cur;
            }
        }
        return -1L;
    }

    /**
     *   Linear search target firstSep and secondSep in current readBuffer
     */
    public MemorySegment readUntil(byte firstSep, byte secondSep) {
        long searchIndex = linearSearch(segment, readIndex, size, firstSep, secondSep);
        return searchIndex < 0 ? null : shiftData(searchIndex, 2L);
    }

    /**
     *   Search target segment for target byte using SIMD inside a register algorithm, return -1 if not found or startIndex > endIndex
     */
    public static long swarSearchWithByteOrder(MemorySegment segment, long startIndex, long endIndex, long pattern, byte target, ByteOrder byteOrder) {
        long available = endIndex - startIndex;
        if(available < Long.BYTES) {
            return linearSearch(segment, startIndex, endIndex, target);
        }
        // check the first part
        int r = searchPattern(NativeUtil.getLong(segment, startIndex), pattern, byteOrder);
        if(r < Long.BYTES) {
            return startIndex + r;
        }
        // check the middle part
        int index = Math.toIntExact(Long.BYTES - ((segment.address() + startIndex) & (Long.BYTES - 1)) + startIndex);
        int end = Math.toIntExact(endIndex - Long.BYTES);
        for( ; index <= end; index += Long.BYTES) {
            r = searchPattern(NativeUtil.getLong(segment, index), pattern, byteOrder);
            if(r < Long.BYTES) {
                return index + r;
            }
        }
        // check the last part
        if(index < endIndex) {
            r = searchPattern(NativeUtil.getLong(segment, end), pattern, byteOrder);
            if(r < Long.BYTES) {
                return end + r;
            }
        }
        return -1L;
    }

    public static long swarSearch(MemorySegment segment, long startIndex, long endIndex, long pattern, byte target) {
        return swarSearchWithByteOrder(segment, startIndex, endIndex, pattern, target, ByteOrder.nativeOrder());
    }

    /**
     *   Search target segment for target byte1 and byte2 using SIMD inside a register algorithm, return -1 if not found or startIndex > endIndex
     */
    public static long swarSearchWithByteOrder(MemorySegment segment, long startIndex, long endIndex, long pattern, byte target1, byte target2, ByteOrder byteOrder) {
        long s = startIndex;
        for( ; ; ) {
            long index = swarSearchWithByteOrder(segment, s, endIndex, pattern, target1, byteOrder);
            if(index < 0) {
                return -1;
            }else if(index < endIndex - 1 && NativeUtil.getByte(segment, index + 1) == target2) {
                return index;
            }else {
                s = index + 1;
            }
        }
    }

    public static long swarSearch(MemorySegment segment, long startIndex, long endIndex, long pattern, byte target1, byte target2) {
        return swarSearchWithByteOrder(segment, startIndex, endIndex, pattern, target1, target2, ByteOrder.nativeOrder());
    }

    /**
     *   Find target sep using SIMD inside a register algorithm, return null if not found
     */
    public MemorySegment swarReadUntil(long pattern, byte sep, ByteOrder byteOrder) {
        long searchIndex = swarSearchWithByteOrder(segment, readIndex, size, pattern, sep, byteOrder);
        return searchIndex < 0L ? null : shiftData(searchIndex, 1L);
    }

    public MemorySegment swarReadUntil(long pattern, byte sep) {
        return swarReadUntil(pattern, sep, ByteOrder.nativeOrder());
    }

    /**
     *   Find target firstSep and secondSep using SIMD inside a register algorithm, return null if not found
     */
    public MemorySegment swarReadUntil(long pattern, byte firstSep, byte secondSep, ByteOrder byteOrder) {
        long searchIndex = swarSearchWithByteOrder(segment, readIndex, size, pattern, firstSep, secondSep, byteOrder);
        return searchIndex < 0L ? null : shiftData(searchIndex, 2L);
    }

    public MemorySegment swarReadUntil(long pattern, byte firstSep, byte secondSep) {
        return swarReadUntil(pattern, firstSep, secondSep, ByteOrder.nativeOrder());
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

    /**
     *   Creating a snapshot for current ReadBuffer, which could be used in vector-search or some other situations
     *   TODO this is a helper method to avoid loading incubator module for vector API, and could be removed when vector api become preview
     */
    public ReadBufferSnapshot snapshot() {
        return new ReadBufferSnapshot(segment.asReadOnly(), readIndex);
    }

    @Override
    public String toString() {
        return new String(segment.toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8);
    }
}
