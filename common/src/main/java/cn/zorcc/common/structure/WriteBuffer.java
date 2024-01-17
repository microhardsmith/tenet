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
@SuppressWarnings("unused")
public record WriteBuffer(
        WriteBufferData data,
        WriteBufferPolicy policy
) implements AutoCloseable {
    private static final int DEFAULT_HEAP_BUFFER_SIZE = 32;

    static class WriteBufferData {
        private MemorySegment segment;
        private long writeIndex;

        WriteBufferData(MemorySegment segment) {
            this.segment = segment;
            this.writeIndex = 0L;
        }
    }

    public static WriteBuffer newDefaultWriteBuffer(long initialSize) {
        if(initialSize <= 0L) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED);
        }
        return new WriteBuffer(new WriteBufferData(MemorySegment.NULL), new DefaultWriteBufferPolicy(initialSize));
    }

    public static WriteBuffer newReservedWriteBuffer(MemorySegment segment, boolean onHeap) {
        if(NativeUtil.checkNullPointer(segment)) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED);
        }
        return new WriteBuffer(new WriteBufferData(segment), new ReservedWriteBufferPolicy(segment, onHeap));
    }

    public static WriteBuffer newHeapWriteBuffer(long initialSize) {
        if(initialSize <= 0L) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED);
        }
        return new WriteBuffer(new WriteBufferData(MemorySegment.NULL), new HeapWriteBufferPolicy(initialSize));
    }

    public static WriteBuffer newHeapWriteBuffer() {
        return newHeapWriteBuffer(DEFAULT_HEAP_BUFFER_SIZE);
    }

    public long writeIndex() {
        return data.writeIndex;
    }

    public void resize(long nextIndex) {
        if(nextIndex > data.segment.byteSize()) {
            policy.resize(data, nextIndex);
        }
    }

    public void writeByte(byte b) {
        long nextIndex = data.writeIndex + Byte.BYTES;
        resize(nextIndex);
        data.segment.set(ValueLayout.JAVA_BYTE, data.writeIndex, b);
        data.writeIndex = nextIndex;
    }

    public void writeBytes(byte[] b, int off, int len) {
        if(len < 0 || off + len > b.length) {
            throw new FrameworkException(ExceptionType.NATIVE, "Index out of bound");
        }
        long nextIndex = data.writeIndex + len;
        resize(nextIndex);
        MemorySegment.copy(b, off, data.segment, ValueLayout.JAVA_BYTE, data.writeIndex, len);
        data.writeIndex = nextIndex;
    }

    public void writeBytes(byte[] bytes) {
        writeBytes(bytes, 0, bytes.length);
    }

    public void writeShort(short s) {
        long nextIndex = data.writeIndex + Short.BYTES;
        resize(nextIndex);
        data.segment.set(ValueLayout.JAVA_SHORT_UNALIGNED, data.writeIndex, s);
        data.writeIndex = nextIndex;
    }

    public void writeInt(int i) {
        long nextIndex = data.writeIndex + Integer.BYTES;
        resize(nextIndex);
        data.segment.set(ValueLayout.JAVA_INT_UNALIGNED, data.writeIndex, i);
        data.writeIndex = nextIndex;
    }

    public void writeLong(long l) {
        long nextIndex = data.writeIndex + Long.BYTES;
        resize(nextIndex);
        data.segment.set(ValueLayout.JAVA_LONG_UNALIGNED, data.writeIndex, l);
        data.writeIndex = nextIndex;
    }

    public void writeFloat(float f) {
        long nextIndex = data.writeIndex + Float.BYTES;
        resize(nextIndex);
        data.segment.set(ValueLayout.JAVA_FLOAT_UNALIGNED, data.writeIndex, f);
        data.writeIndex = nextIndex;
    }

    public void writeDouble(double d) {
        long nextIndex = data.writeIndex + Double.BYTES;
        resize(nextIndex);
        data.segment.set(ValueLayout.JAVA_DOUBLE_UNALIGNED, data.writeIndex, d);
        data.writeIndex = nextIndex;
    }

    public void writeStr(String str, Charset charset) {
        byte[] strBytes = str.getBytes(charset);
        long nextIndex = data.writeIndex + strBytes.length + 1;
        resize(nextIndex);
        MemorySegment.copy(strBytes, 0, data.segment, ValueLayout.JAVA_BYTE, data.writeIndex, strBytes.length);
        data.segment.set(ValueLayout.JAVA_BYTE, data.writeIndex + strBytes.length, Constants.NUT);
        data.writeIndex = nextIndex;
    }

    public void writeStr(String str) {
        writeStr(str, StandardCharsets.UTF_8);
    }

    public void writeSegment(MemorySegment memorySegment) {
        long len = memorySegment.byteSize();
        long nextIndex = data.writeIndex + len;
        resize(nextIndex);
        MemorySegment.copy(memorySegment, 0, data.segment, data.writeIndex, len);
        data.writeIndex = nextIndex;
    }

    public void writeSegmentWithPadding(MemorySegment memorySegment, long minWidth, byte padding) {
        long len = memorySegment.byteSize();
        if(minWidth <= len) {
            writeSegment(memorySegment);
        }else {
            long nextIndex = data.writeIndex + minWidth;
            resize(nextIndex);
            MemorySegment.copy(memorySegment, 0, data.segment, data.writeIndex, len);
            data.segment.asSlice(data.writeIndex + len, minWidth - len).fill(padding);
            data.writeIndex = nextIndex;
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
        if(index < 0L || index + Byte.BYTES > data.writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "Index out of bound");
        }
        data.segment.set(ValueLayout.JAVA_BYTE, index, value);
    }

    public void setShort(long index, short value) {
        if(index < 0L || index + Short.BYTES > data.writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "Index out of bound");
        }
        data.segment.set(ValueLayout.JAVA_SHORT_UNALIGNED, index, value);
    }

    public void setInt(long index, int value) {
        if(index < 0L || index + Integer.BYTES > data.writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "Index out of bound");
        }
        data.segment.set(ValueLayout.JAVA_INT_UNALIGNED, index, value);
    }

    public void setLong(long index, long value) {
        if(index < 0L || index + Long.BYTES > data.writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "Index out of bound");
        }
        data.segment.set(ValueLayout.JAVA_LONG_UNALIGNED, index, value);
    }

    public void setFloat(long index, float value) {
        if(index < 0L || index + Float.BYTES > data.writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "Index out of bound");
        }
        data.segment.set(ValueLayout.JAVA_FLOAT_UNALIGNED, index, value);
    }

    public void setDouble(long index, double value) {
        if(index < 0L || index + Double.BYTES > data.writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "Index out of bound");
        }
        data.segment.set(ValueLayout.JAVA_DOUBLE_UNALIGNED, index, value);
    }

    /**
     *   Return current written segment, from 0 ~ writeIndex, calling content() will not modify current writeBuffer's writeIndex
     */
    public MemorySegment content() {
        return data.writeIndex == data.segment.byteSize() ? data.segment : data.segment.asSlice(0L, data.writeIndex);
    }

    /**
     *   Truncate from the beginning of the segment to the offset, return a new WriteBuffer
     */
    public WriteBuffer truncate(long offset) {
        if(offset > data.writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "Truncate index overflow");
        }
        WriteBufferData newData = new WriteBufferData(data.segment.asSlice(offset, data.segment.byteSize() - offset));
        newData.writeIndex = data.writeIndex - offset;
        return new WriteBuffer(newData, policy);
    }

    @Override
    public void close() {
        policy.close(data);
    }

    /**
     *   Converts current writeBuffer to an array
     */
    public byte[] toArray() {
        int size = Math.toIntExact(data.writeIndex);
        data.writeIndex = 0L;
        if(data.segment.isNative()) {
            byte[] bytes = new byte[size];
            MemorySegment.copy(data.segment, ValueLayout.JAVA_BYTE, data.writeIndex, bytes, 0, size);
            return bytes;
        }else {
            byte[] bytes = (byte[]) data.segment.heapBase().orElseThrow(() -> new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED));
            return Arrays.copyOfRange(bytes, 0, size);
        }
    }

    /**
     *   Converts current writeBuffer to a segment, sharing the same scope
     */
    public MemorySegment toSegment() {
        MemorySegment m = content();
        data.writeIndex = 0L;
        return m;
    }

    /**
     *   Default writeBufferPolicy using malloc and free, double the memory allocated to contain the grown bytes
     *   Memory were allocated when first resizing the buffer
     */
    record DefaultWriteBufferPolicy(
            long initialSize
    ) implements WriteBufferPolicy {
        @Override
        public void resize(WriteBufferData writeBufferData, long nextIndex) {
            MemorySegment newNativeSegment;
            if(writeBufferData.segment == MemorySegment.NULL) {
                newNativeSegment = NativeUtil.malloc(Math.max(nextIndex, initialSize));
            }else {
                long newLen = Math.max(nextIndex, writeBufferData.segment.byteSize() << 1);
                if(newLen < 0L) {
                    throw new FrameworkException(ExceptionType.NATIVE, "MemorySize overflow");
                }
                newNativeSegment = NativeUtil.realloc(writeBufferData.segment, newLen);
            }
            if(NativeUtil.checkNullPointer(newNativeSegment)) {
                throw new OutOfMemoryError();
            }
            writeBufferData.segment = newNativeSegment;
        }

        @Override
        public void close(WriteBufferData writeBufferData) {
            if(writeBufferData.segment != MemorySegment.NULL) {
                NativeUtil.free(writeBufferData.segment);
            }
        }
    }

    /**
     *   Reserved writeBufferPolicy, the initial memorySegment must be native memory, will be reserved and never released
     *   if onHeap, the expanded memory will be allocated on heap, or it will be realloc()
     */
    record ReservedWriteBufferPolicy(
            MemorySegment initialSegment,
            boolean onHeap
    ) implements WriteBufferPolicy {

        @Override
        public void resize(WriteBufferData writeBufferData, long nextIndex) {
            long newLen = Math.max(nextIndex, writeBufferData.segment.byteSize() << 1);
            if(newLen < 0) {
                throw new FrameworkException(ExceptionType.NATIVE, "MemorySize overflow");
            }
            if(onHeap) {
                MemorySegment newHeapSegment = Allocator.HEAP.allocate(ValueLayout.JAVA_BYTE, newLen);
                MemorySegment.copy(writeBufferData.segment, 0L, newHeapSegment, 0L, writeBufferData.writeIndex);
                writeBufferData.segment = newHeapSegment;
            }else {
                MemorySegment newNativeSegment = NativeUtil.realloc(writeBufferData.segment, newLen);
                if(NativeUtil.checkNullPointer(newNativeSegment)) {
                    throw new OutOfMemoryError();
                }
                writeBufferData.segment = newNativeSegment;
            }
        }

        @Override
        public void close(WriteBufferData writeBufferData) {
            if(writeBufferData.segment != initialSegment && writeBufferData.segment.isNative()) {
                NativeUtil.free(writeBufferData.segment);
            }
        }
    }

    record HeapWriteBufferPolicy(
            long initialSize
    ) implements WriteBufferPolicy {
        @Override
        public void resize(WriteBufferData writeBufferData, long nextIndex) {
            MemorySegment newSegment;
            if(writeBufferData.segment == MemorySegment.NULL) {
                newSegment = Allocator.HEAP.allocate(ValueLayout.JAVA_BYTE, initialSize);
            }else {
                long newLen = Math.max(nextIndex, writeBufferData.segment.byteSize() << 1);
                if(newLen < 0L) {
                    throw new FrameworkException(ExceptionType.NATIVE, "MemorySize overflow");
                }
                newSegment = Allocator.HEAP.allocate(ValueLayout.JAVA_BYTE, newLen);
                MemorySegment.copy(writeBufferData.segment, 0L, newSegment, 0L, writeBufferData.writeIndex);
            }
            writeBufferData.segment = newSegment;
        }

        @Override
        public void close(WriteBufferData writeBufferData) {
            // No external close operation needed for heapWriteBuffer
        }
    }

    sealed interface WriteBufferPolicy permits DefaultWriteBufferPolicy, ReservedWriteBufferPolicy, HeapWriteBufferPolicy {
        /**
         *   Resize target writeBuffer to contain more bytes than nextIndex, this function will not change the writeIndex of the writeBufferData
         */
        void resize(WriteBufferData writeBufferData, long nextIndex);

        /**
         *   Close current writeBuffer after using
         */
        void close(WriteBufferData writeBufferData);
    }
}
