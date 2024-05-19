package cn.zorcc.common.structure;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 *   Direct memory WriteBuffer, not thread-safe, custom Resizer could be chosen to modify the default expansion mechanism
 */
public final class WriteBuffer implements AutoCloseable {

    sealed interface WriteBufferPolicy permits NativeWriteBufferPolicy, ReservedWriteBufferPolicy, HeapWriteBufferPolicy {
        /**
         *   Resize target writeBuffer to contain more bytes than nextIndex, this function will not change the writeIndex of the writeBufferData
         */
        void resize(WriteBuffer writeBuffer, long nextIndex);

        /**
         *   Close current writeBuffer after using
         */
        void close(WriteBuffer writeBuffer);
    }

    private static final long MINIMAL_WRITE_BUFFER_SIZE = 8;
    private static final int DEFAULT_HEAP_BUFFER_SIZE = 32;
    private MemorySegment segment;
    private long writeIndex;
    private final WriteBufferPolicy policy;

    private WriteBuffer(MemorySegment segment, WriteBufferPolicy policy) {
        this.segment = segment;
        this.writeIndex = 0L;
        this.policy = policy;
    }

    public static WriteBuffer newNativeWriteBuffer(MemApi memApi, long initialSize) {
        if(initialSize < MINIMAL_WRITE_BUFFER_SIZE) {
            throw new FrameworkException(ExceptionType.NATIVE, "DefaultWriteBuffer exceeding minimal size");
        }
        return new WriteBuffer(MemorySegment.NULL, new NativeWriteBufferPolicy(memApi, initialSize));
    }

    public static WriteBuffer newReservedWriteBuffer(MemApi memApi, MemorySegment segment) {
        if(NativeUtil.checkNullPointer(segment)) {
            throw new FrameworkException(ExceptionType.NATIVE, "ReservedWriteBuffer couldn't be allocated with NULL pointer");
        }
        if(!segment.isNative()) {
            throw new FrameworkException(ExceptionType.NATIVE, "ReservedWriteBuffer must be native");
        }
        return new WriteBuffer(segment, new ReservedWriteBufferPolicy(memApi, segment));
    }

    private static final MemorySegment EMPTY_HEAP = MemorySegment.ofArray(new byte[0]);

    public static WriteBuffer newHeapWriteBuffer(long initialSize) {
        if(initialSize <= MINIMAL_WRITE_BUFFER_SIZE) {
            throw new FrameworkException(ExceptionType.NATIVE, "HeapWriteBuffer exceeding minimal size");
        }
        return new WriteBuffer(EMPTY_HEAP, new HeapWriteBufferPolicy(initialSize));
    }

    public static WriteBuffer newHeapWriteBuffer() {
        return newHeapWriteBuffer(DEFAULT_HEAP_BUFFER_SIZE);
    }

    public long writeIndex() {
        return writeIndex;
    }

    public void resize(long nextIndex) {
        if(nextIndex > segment.byteSize()) {
            policy.resize(this, nextIndex);
        }
    }

    public void writeByte(byte b) {
        long nextIndex = writeIndex + Byte.BYTES;
        resize(nextIndex);
        NativeUtil.setByte(segment, writeIndex, b);
        writeIndex = nextIndex;
    }

    public void writeByte(byte b1, byte b2) {
        long nextIndex = writeIndex + Byte.BYTES * 2;
        resize(nextIndex);
        NativeUtil.setByte(segment, writeIndex, b1);
        NativeUtil.setByte(segment, writeIndex + Byte.BYTES, b2);
        writeIndex = nextIndex;
    }

    public void writeBytes(byte[] b, int off, int len) {
        if(len < 0 || off + len > b.length) {
            throw new FrameworkException(ExceptionType.NATIVE, "Index out of bound");
        }
        long nextIndex = writeIndex + len;
        resize(nextIndex);
        MemorySegment.copy(b, off, segment, ValueLayout.JAVA_BYTE, writeIndex, len);
        writeIndex = nextIndex;
    }

    public void writeBytes(byte[] bytes) {
        writeBytes(bytes, 0, bytes.length);
    }

    public void writeShort(short s) {
        long nextIndex = writeIndex + Short.BYTES;
        resize(nextIndex);
        NativeUtil.setShort(segment, writeIndex, s);
        writeIndex = nextIndex;
    }

    public void writeInt(int i) {
        long nextIndex = writeIndex + Integer.BYTES;
        resize(nextIndex);
        NativeUtil.setInt(segment, writeIndex, i);
        writeIndex = nextIndex;
    }

    public void writeLong(long l) {
        long nextIndex = writeIndex + Long.BYTES;
        resize(nextIndex);
        NativeUtil.setLong(segment, writeIndex, l);
        writeIndex = nextIndex;
    }

    public void writeFloat(float f) {
        long nextIndex = writeIndex + Float.BYTES;
        resize(nextIndex);
        NativeUtil.setFloat(segment, writeIndex, f);
        writeIndex = nextIndex;
    }

    public void writeDouble(double d) {
        long nextIndex = writeIndex + Double.BYTES;
        resize(nextIndex);
        NativeUtil.setDouble(segment, writeIndex, d);
        writeIndex = nextIndex;
    }

    public void writeStr(String str, Charset charset) {
        byte[] strBytes = str.getBytes(charset);
        long nextIndex = writeIndex + strBytes.length + 1;
        resize(nextIndex);
        MemorySegment.copy(strBytes, 0, segment, ValueLayout.JAVA_BYTE, writeIndex, strBytes.length);
        NativeUtil.setByte(segment, writeIndex + strBytes.length, Constants.NUT);
        writeIndex = nextIndex;
    }

    public void writeStr(String str) {
        writeStr(str, StandardCharsets.UTF_8);
    }

    public void writeSegment(MemorySegment memorySegment) {
        long len = memorySegment.byteSize();
        long nextIndex = writeIndex + len;
        resize(nextIndex);
        MemorySegment.copy(memorySegment, 0L, segment, writeIndex, len);
        writeIndex = nextIndex;
    }

    public void writeSegmentWithPadding(MemorySegment memorySegment, long minWidth, byte padding) {
        long len = memorySegment.byteSize();
        if(minWidth <= len) {
            writeSegment(memorySegment);
        }else {
            long nextIndex = writeIndex + minWidth;
            resize(nextIndex);
            MemorySegment.copy(memorySegment, 0L, segment, writeIndex, len);
            segment.asSlice(writeIndex + len, minWidth - len).fill(padding);
            writeIndex = nextIndex;
        }
    }

    public void writeUtf8Data(int data) {
        if(data < 0x80) {
            writeByte((byte) data);
        }else if(data < 0x800) {
            writeByte((byte) (0xC0 | (data >> 6)));
            writeByte((byte) (0x80 | (data & 0x3F)));
        }else {
            writeByte((byte) (0xE0 | (data >> 12)));
            writeByte((byte) (0x80 | ((data >> 6) & 0x3F)));
            writeByte((byte) (0x80 | (data & 0x3F)));
        }
    }

    public void setByte(long index, byte value) {
        if(index < 0L || index + Byte.BYTES > writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "Index out of bound");
        }
        NativeUtil.setByte(segment, index, value);
    }

    public void setShort(long index, short value) {
        if(index < 0L || index + Short.BYTES > writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "Index out of bound");
        }
        NativeUtil.setShort(segment, index, value);
    }

    public void setInt(long index, int value) {
        if(index < 0L || index + Integer.BYTES > writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "Index out of bound");
        }
        NativeUtil.setInt(segment, index, value);
    }

    public void setLong(long index, long value) {
        if(index < 0L || index + Long.BYTES > writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "Index out of bound");
        }
        NativeUtil.setLong(segment, index, value);
    }

    public void setFloat(long index, float value) {
        if(index < 0L || index + Float.BYTES > writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "Index out of bound");
        }
        NativeUtil.setFloat(segment, index, value);
    }

    public void setDouble(long index, double value) {
        if(index < 0L || index + Double.BYTES > writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "Index out of bound");
        }
        NativeUtil.setDouble(segment, index, value);
    }

    /**
     *   Return current written segment
     */
    public MemorySegment content() {
        if(writeIndex == 0L) {
            return MemorySegment.NULL;
        }else if(writeIndex == segment.byteSize()) {
            return segment;
        }else {
            return segment.asSlice(0L, writeIndex);
        }
    }

    /**
     *   Truncate from the beginning of the segment to the offset, return a new WriteBuffer
     */
    public WriteBuffer truncate(long offset) {
        if(offset < 0L || offset > writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "Truncate index overflow");
        }
        WriteBuffer trucatedWriteBuffer = new WriteBuffer(segment.asSlice(offset, segment.byteSize() - offset), policy);
        trucatedWriteBuffer.writeIndex = writeIndex - offset;
        return trucatedWriteBuffer;
    }

    @Override
    public void close() {
        policy.close(this);
    }

    /**
     *   Converts current writeBuffer to an array
     */
    public byte[] asByteArray() {
        final int currentSize = Math.toIntExact(writeIndex);
        if(currentSize == 0) {
            return Constants.EMPTY_BYTES;
        }
        writeIndex = 0L;
        if(segment.isNative()) {
            byte[] bytes = new byte[currentSize];
            MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, 0L, bytes, 0, currentSize);
            return bytes;
        }else {
            byte[] bytes = (byte[]) segment.heapBase().orElseThrow(() -> new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED));
            return Arrays.copyOfRange(bytes, 0, currentSize);
        }
    }

    /**
     *   Converts current writeBuffer to a segment, sharing the same scope
     */
    public MemorySegment asSegment() {
        MemorySegment m = content();
        writeIndex = 0L;
        return m;
    }

    /**
     *   Native writeBufferPolicy using malloc, realloc and free, double the memory allocated to contain the grown bytes
     *   Memory were allocated when first resizing the buffer
     */
    record NativeWriteBufferPolicy(
            MemApi memApi,
            long initialSize
    ) implements WriteBufferPolicy {
        @Override
        public void resize(WriteBuffer writeBufferData, long nextIndex) {
            final MemorySegment current = writeBufferData.segment;
            MemorySegment newNativeSegment;
            if(NativeUtil.checkNullPointer(current)) {
                newNativeSegment = memApi.allocateMemory(Math.max(nextIndex, initialSize)).reinterpret(initialSize);
            }else {
                long newLen = Math.max(nextIndex, current.byteSize() << 1);
                if(newLen < 0L) {
                    throw new FrameworkException(ExceptionType.NATIVE, "MemorySize overflow");
                }
                newNativeSegment = memApi.reallocateMemory(current, newLen).reinterpret(newLen);
            }
            if(NativeUtil.checkNullPointer(newNativeSegment)) {
                throw new OutOfMemoryError();
            }
            writeBufferData.segment = newNativeSegment;
        }

        @Override
        public void close(WriteBuffer writeBufferData) {
            final MemorySegment current = writeBufferData.segment;
            if(!NativeUtil.checkNullPointer(current)) {
                memApi.freeMemory(writeBufferData.segment);
            }
        }
    }

    /**
     *   Reserved writeBufferPolicy, the initial memorySegment must be native memory, which would be reserved and never released
     */
    record ReservedWriteBufferPolicy(
            MemApi memApi,
            MemorySegment initialSegment
    ) implements WriteBufferPolicy {

        @Override
        public void resize(WriteBuffer writeBufferData, long nextIndex) {
            final MemorySegment current = writeBufferData.segment;
            long newLen = Math.max(nextIndex, current.byteSize() << 1);
            if(newLen < 0L) {
                throw new FrameworkException(ExceptionType.NATIVE, "MemorySize overflow");
            }
            MemorySegment newSegment = current.address() == initialSegment.address() ?
                    memApi.allocateMemory(newLen).reinterpret(newLen) :
                    memApi.reallocateMemory(current, newLen).reinterpret(newLen);
            if(NativeUtil.checkNullPointer(newSegment)) {
                throw new OutOfMemoryError();
            }
            writeBufferData.segment = newSegment;
        }

        @Override
        public void close(WriteBuffer writeBufferData) {
            final MemorySegment current = writeBufferData.segment;
            if(current.address() != initialSegment.address()) {
                memApi.freeMemory(current);
            }
        }
    }

    /**
     *   Allocating all the segments on heap, using byte[] abstraction
     */
    record HeapWriteBufferPolicy(
            long initialSize
    ) implements WriteBufferPolicy {
        @Override
        public void resize(WriteBuffer writeBufferData, long nextIndex) {
            final MemorySegment current = writeBufferData.segment;
            MemorySegment newSegment;
            if(current == EMPTY_HEAP) {
                newSegment = Allocator.HEAP.allocate(ValueLayout.JAVA_BYTE, Math.max(nextIndex, initialSize));
            }else {
                long newLen = Math.max(nextIndex, current.byteSize() << 1);
                if(newLen < 0L) {
                    throw new FrameworkException(ExceptionType.NATIVE, "MemorySize overflow");
                }
                newSegment = Allocator.HEAP.allocate(ValueLayout.JAVA_BYTE, newLen);
                MemorySegment.copy(current, 0L, newSegment, 0L, writeBufferData.writeIndex);
            }
            writeBufferData.segment = newSegment;
        }

        @Override
        public void close(WriteBuffer writeBufferData) {
            // No external close operation needed for heapWriteBuffer
        }
    }
}
