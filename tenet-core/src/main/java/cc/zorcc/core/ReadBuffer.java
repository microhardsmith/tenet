package cc.zorcc.core;

import cc.zorcc.core.bindings.SysBinding;
import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorSpecies;

import java.lang.foreign.MemorySegment;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.function.LongToIntFunction;

/**
 *  ReadBuffer is an abstraction for memorySegment to read, it couldn't be written or modified, and it's not thread-safe
 */
public final class ReadBuffer {
    private final MemorySegment segment;
    private long readIndex;

    public ReadBuffer(MemorySegment segment) {
        // change the scope to read only prevents potential illegal access
        this.segment = segment.isReadOnly() ? segment : segment.asReadOnly();
    }

    /**
     *   Get current readIndex
     */
    public long readIndex() {
        return readIndex;
    }

    /**
     *   Reset current readIndex
     */
    public void setIndex(long nextIndex) {
        if(nextIndex < 0L || nextIndex >= segment.byteSize()) {
            throw new FrameworkException(ExceptionType.NATIVE, "Index out of bounds");
        }
        readIndex = nextIndex;
    }

    /**
     *   Get current readBuffer size
     */
    public long size() {
        return segment.byteSize();
    }

    /*
     *   Below are some accessing methods with different layouts
     *   Boundary checking are performed by memorySegment itself, there is no need for us to do double-checking
     */

    /**
     *   Read a byte value from current ReadBuffer
     */
    public byte readByte() {
        byte b = Std.getByte(segment, readIndex);
        readIndex += Byte.BYTES;
        return b;
    }

    /**
     *   Read a short value from current ReadBuffer
     */
    public short readShort() {
        short s = Std.getShort(segment, readIndex);
        readIndex += Short.BYTES;
        return s;
    }

    /**
     *   Read a int value from current ReadBuffer
     */
    public int readInt() {
        int i = Std.getInt(segment, readIndex);
        readIndex += Integer.BYTES;
        return i;
    }

    /**
     *   Read a long value from current ReadBuffer
     */
    public long readLong() {
        long l = Std.getLong(segment, readIndex);
        readIndex += Long.BYTES;
        return l;
    }

    /**
     *   Read a float value from current ReadBuffer
     */
    public float readFloat() {
        float f = Std.getFloat(segment, readIndex);
        readIndex += Float.BYTES;
        return f;
    }

    /**
     *   Read a double value from current ReadBuffer
     */
    public double readDouble() {
        double d = Std.getDouble(segment, readIndex);
        readIndex += Double.BYTES;
        return d;
    }

    /**
     *   Read a memorySegment from current ReadBuffer, the returned memorySegment is also read-only
     */
    public MemorySegment readSegment(long size) {
        MemorySegment slice = segment.asSlice(readIndex, size);
        readIndex += size;
        return slice;
    }

    /**
     *   Read a byte array from current ReadBuffer, here we could just use MemorySegment.toArray()
     *   however, using memcpy() would provide more performance for avoiding boundary checking
     */
    public byte[] readBytes(int size) {
        MemorySegment slice = segment.asSlice(readIndex, size);
        byte[] r = new byte[size];
        SysBinding.memcpy(MemorySegment.ofArray(r), slice, size);
        readIndex += size;
        return r;
    }

    /**
     *   Read a UTF-8 string from current ReadBuffer, throw a IndexOutOfBoundsException if terminator was not found
     *   The performance of this function should be as compatible as memorySegment.getString()
     */
    public String readUtf8Str() {
        long available = segment.byteSize() - readIndex;
        MemorySegment slice = segment.asSlice(readIndex, available);
        long index = SysBinding.strlen(slice, available);
        if(index == available) {
            throw new IllegalArgumentException();
        }
        byte[] r = new byte[Math.toIntExact(index)];
        SysBinding.memcpy(MemorySegment.ofArray(r), slice, index);
        readIndex += (index + 1);
        return new String(r);
    }

    /*
     *  Below are some searching mechanisms, including linearSearch, swarSearch and vectorSearch
     *  LinearSearch is the slowest one, simplest however
     *  SwarSearch could provide much better performance than LinearSearch, but it could only search for specific byte
     *  VectorSearch is the most powerful one, but it requires the CPU to have the ability for SIMD and masking operations
     *  All the methods would return the searched index, return -1L if not found, readIndex are only modified if target were found
     */
    private static final long NOT_FOUND = -1L;

    /**
     *   Linear search using target byteMatcher, return the searched index, return -1 if not found
     */
    public long linearSearch(ByteMatcher byteMatcher) {
        final int start = Math.toIntExact(readIndex);
        final int end = Math.toIntExact(segment.byteSize());
        for(int i = start; i < end; i++) {
            byte b = Std.getByte(segment, i);
            if(byteMatcher.match(b)) {
                readIndex += (i + 1);
                return readIndex;
            }
        }
        return NOT_FOUND;
    }

    private static final LongToIntFunction SWAR_FUNC = swarFunc();

    private static LongToIntFunction swarFunc() {
        ByteOrder o = ByteOrder.nativeOrder();
        if(o == ByteOrder.BIG_ENDIAN) {
            return match -> (Long.numberOfLeadingZeros(match) >>> 3);
        }else if(o == ByteOrder.LITTLE_ENDIAN) {
            return match -> (Long.numberOfTrailingZeros(match) >>> 3);
        }else {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED);
        }
    }

    /**
     *  Find the target pattern in given data and return the searched index, return Long.Bytes if not found
     */
    private static int swarCheck(long data, long pattern) {
        long mask = data ^ pattern; // this single line is the major cost that we are about 10% slower than the JDK version
        long match = (mask - 0x0101010101010101L) & ~mask & 0x8080808080808080L;
        return swarFunc().applyAsInt(match);
    }

    /**
     *   Swar search using target byteMatcher, return the searched index, return -1 if not found
     */
    public long swarSearch(SwarMatcher swarMatcher) {
        final int start = Math.toIntExact(readIndex);
        final int end = Math.toIntExact(segment.byteSize());
        final byte target = swarMatcher.getTarget();
        final long pattern = swarMatcher.getPattern();
        // fallback to linearSearch if data is too small
        if(end - start < Long.BYTES) {
            return linearSearch(b -> b == target);
        }
        // check the first part
        int r = swarCheck(Std.getLong(segment, start), pattern);
        if(r < Long.BYTES) {
            readIndex += (r + 1);
            return readIndex;
        }
        // check the middle part
        int index = Math.toIntExact(Long.BYTES - ((segment.address() + start) & (Long.BYTES - 1)) + start);
        int tail = Math.toIntExact(end - Long.BYTES);
        for( ; index <= tail; index += Long.BYTES) {
            r = swarCheck(Std.getLong(segment, index), pattern);
            if(r < Long.BYTES) {
                readIndex += (r + 1);
                return readIndex;
            }
        }
        // check the tail part
        if(index < end) {
            r = swarCheck(Std.getLong(segment, tail), pattern);
            if(r < Long.BYTES) {
                readIndex += (r + 1);
                return readIndex;
            }
        }
        return NOT_FOUND;
    }

    /**
     *   When using vector search, we will always use the SPECIES_PREFERRED length of vector operation, this value could be tuned for specific workloads
     */
    private static final VectorSpecies<Byte> species = ByteVector.SPECIES_PREFERRED;

    /**
     *   Vector search using target vectorMatcher, return the searched index, return -1 if not found
     */
    public long vectorSearch(VectorMatcher vectorMatcher) {
        final int start = Math.toIntExact(readIndex);
        final int end = Math.toIntExact(segment.byteSize());
        for(int index = start; index < end; index += species.length()) {
            VectorMask<Byte> mask = species.indexInRange(index, end);
            ByteVector vec = ByteVector.fromMemorySegment(species, segment, index, ByteOrder.nativeOrder(), mask);
            int r = vectorMatcher.match(vec).firstTrue();
            if(r < species.length()) {
                readIndex += (index + r + 1);
                return readIndex;
            }
        }
        return NOT_FOUND;
    }

}