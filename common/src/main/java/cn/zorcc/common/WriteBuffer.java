package cn.zorcc.common;

import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.io.OutputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 *   Direct memory WriteBuffer, not thread-safe, custom Resizer could be chosen to modify the default expansion mechanism
 */
public final class WriteBuffer extends OutputStream {
    private static final int DEFAULT_HEAP_BUFFER_SIZE = 32;
    private MemorySegment segment;
    private long size;
    private long writeIndex;
    private final WriteBufferPolicy policy;

    private WriteBuffer(MemorySegment segment, WriteBufferPolicy policy) {
        this.segment = segment;
        this.size = segment.byteSize();
        this.writeIndex = 0;
        this.policy = policy;
    }

    public static WriteBuffer newDefaultWriteBuffer(Arena arena, long size) {
        MemorySegment memorySegment = arena.allocateArray(ValueLayout.JAVA_BYTE, size);
        return new WriteBuffer(memorySegment, new DefaultWriteBufferPolicy(arena));
    }

    public static WriteBuffer newFixedWriteBuffer(Arena arena, long size) {
        MemorySegment memorySegment = arena.allocateArray(ValueLayout.JAVA_BYTE, size);
        return new WriteBuffer(memorySegment, new FixedWriteBufferPolicy(arena));
    }

    public static WriteBuffer newReservedWriteBuffer(MemorySegment segment) {
        return new WriteBuffer(segment, new ReservedWriteBufferPolicy());
    }

    public static WriteBuffer newHeapWriteBuffer(int size) {
        byte[] data = new byte[size];
        return new WriteBuffer(MemorySegment.ofArray(data), new HeapWriteBufferPolicy(data));
    }

    public static WriteBuffer newHeapWriteBuffer() {
        return newHeapWriteBuffer(DEFAULT_HEAP_BUFFER_SIZE);
    }

    public long size() {
        return size;
    }

    public long writeIndex() {
        return writeIndex;
    }

    public void resize(long nextIndex) {
        if(nextIndex < 0) {
            throw new FrameworkException(ExceptionType.NATIVE, "Index overflow");
        }else if(nextIndex > size) {
            policy.resize(this, nextIndex);
        }
    }

    public void writeByte(byte b) {
        long nextIndex = writeIndex + NativeUtil.getByteSize();
        resize(nextIndex);
        NativeUtil.setByte(segment, writeIndex, b);
        writeIndex = nextIndex;
    }

    @Override
    public void write(int b) {
        writeByte((byte) b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        if(len < 0 || off + len > b.length) {
            throw new FrameworkException(ExceptionType.NATIVE, "Index out of bound");
        }
        long nextIndex = writeIndex + len;
        resize(nextIndex);
        MemorySegment.copy(MemorySegment.ofArray(b), off, segment, writeIndex, len);
        writeIndex = nextIndex;
    }

    @Override
    public void write(byte[] b) {
        writeBytes(b);
    }

    public void writeBytes(byte... bytes) {
        long nextIndex = writeIndex + bytes.length;
        resize(nextIndex);
        MemorySegment.copy(MemorySegment.ofArray(bytes), 0, segment, writeIndex, bytes.length);
        writeIndex = nextIndex;
    }

    public void writeShort(short s) {
        long nextIndex = writeIndex + NativeUtil.getShortSize();
        resize(nextIndex);
        NativeUtil.setShort(segment, writeIndex, s);
        writeIndex = nextIndex;
    }

    public void writeInt(int i) {
        long nextIndex = writeIndex + NativeUtil.getIntSize();
        resize(nextIndex);
        NativeUtil.setInt(segment, writeIndex, i);
        writeIndex = nextIndex;
    }

    public void writeLong(long l) {
        long nextIndex = writeIndex + NativeUtil.getLongSize();
        resize(nextIndex);
        NativeUtil.setLong(segment, writeIndex, l);
        writeIndex = nextIndex;
    }

    public void writeCStr(String str) {
        MemorySegment m = MemorySegment.ofArray(str.getBytes(StandardCharsets.UTF_8));
        long len = m.byteSize();
        long nextIndex = writeIndex + len + 1;
        resize(nextIndex);
        MemorySegment.copy(m, 0, segment, writeIndex, len);
        NativeUtil.setByte(segment, writeIndex + len, Constants.NUT);
        writeIndex = nextIndex;
    }

    public void writeSegment(MemorySegment memorySegment) {
        long len = memorySegment.byteSize();
        long nextIndex = writeIndex + len;
        resize(nextIndex);
        MemorySegment.copy(memorySegment, 0, segment, writeIndex, len);
        writeIndex = nextIndex;
    }

    public void writeSegmentWithPadding(MemorySegment memorySegment, long minWidth, byte padding) {
        long len = memorySegment.byteSize();
        if(minWidth <= len) {
            writeSegment(memorySegment);
        }else {
            long nextIndex = writeIndex + minWidth;
            resize(nextIndex);
            MemorySegment.copy(memorySegment, 0, segment, writeIndex, len);
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
        if(index + NativeUtil.getByteSize() > writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "Index out of bound");
        }
        NativeUtil.setByte(segment, index, value);
    }

    public void setShort(long index, short value) {
        if(index + NativeUtil.getShortSize() > writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "Index out of bound");
        }
        NativeUtil.setShort(segment, index, value);
    }

    public void setInt(long index, int value) {
        if(index + NativeUtil.getIntSize() > writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "Index out of bound");
        }
        NativeUtil.setInt(segment, index, value);
    }

    public void setLong(long index, long value) {
        if(index + NativeUtil.getLongSize() > writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "Index out of bound");
        }
        NativeUtil.setLong(segment, index, value);
    }

    /**
     *   Return current written segment, from 0 ~ writeIndex, calling content() will not modify current writeBuffer's writeIndex
     */
    public MemorySegment content() {
        return writeIndex == size ? segment : segment.asSlice(0, writeIndex);
    }

    /**
     *   Truncate from the beginning of the segment to the offset, return a new WriteBuffer
     */
    public WriteBuffer truncate(long offset) {
        if(offset > writeIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "Truncate index overflow");
        }
        WriteBuffer w = new WriteBuffer(segment.asSlice(offset, size - offset), policy);
        w.writeIndex = writeIndex - offset;
        return w;
    }

    @Override
    public void close() {
        policy.close(this);
    }

    @Override
    public String toString() {
        if(writeIndex > Integer.MAX_VALUE) {
            throw new FrameworkException(ExceptionType.NATIVE, "String size overflow");
        }
        final int index = (int) writeIndex;
        writeIndex = 0;
        if(policy instanceof HeapWriteBufferPolicy heapWriteBufferPolicy) {
            return new String(heapWriteBufferPolicy.data, 0, index, StandardCharsets.UTF_8);
        }else {
            return new String(segment.toArray(ValueLayout.JAVA_BYTE), 0, index, StandardCharsets.UTF_8);
        }
    }

    public byte[] toArray() {
        if(writeIndex > Integer.MAX_VALUE) {
            throw new FrameworkException(ExceptionType.NATIVE, "String size overflow");
        }
        final int index = (int) writeIndex;
        writeIndex = 0;
        if(policy instanceof HeapWriteBufferPolicy heapWriteBufferPolicy) {
            return Arrays.copyOfRange(heapWriteBufferPolicy.data, 0, index);
        }else {
            byte[] bytes = new byte[index];
            MemorySegment m = MemorySegment.ofArray(bytes);
            MemorySegment.copy(segment, 0, m, 0, index);
            return bytes;
        }
    }

    public MemorySegment toSegment() {
        MemorySegment m = content();
        writeIndex = 0;
        return m;
    }

    /**
     *   Default writeBufferPolicy, double the memory allocated to contain the grown bytes
     */
    static final class DefaultWriteBufferPolicy implements WriteBufferPolicy {
        private final Arena arena;

        public DefaultWriteBufferPolicy(Arena arena) {
            this.arena = arena;
        }

        @Override
        public void resize(WriteBuffer writeBuffer, long nextIndex) {
            long newLen = Math.max(nextIndex, writeBuffer.size() << 1);
            if(newLen < 0) {
                throw new FrameworkException(ExceptionType.NATIVE, "MemorySize overflow");
            }
            MemorySegment newSegment = arena.allocateArray(ValueLayout.JAVA_BYTE, newLen);
            MemorySegment.copy(writeBuffer.segment, 0, newSegment, 0, writeBuffer.writeIndex);
            writeBuffer.segment = newSegment;
            writeBuffer.size = newLen;
        }

        @Override
        public void close(WriteBuffer writeBuffer) {
            arena.close();
        }
    }

    /**
     *   Ignore writeBufferPolicy, throw an exception if current WriteBuffer wants to resize
     */
    static final class FixedWriteBufferPolicy implements WriteBufferPolicy {
        private final Arena arena;

        public FixedWriteBufferPolicy(Arena arena) {
            this.arena = arena;
        }

        @Override
        public void resize(WriteBuffer writeBuffer, long nextIndex) {
            throw new FrameworkException(ExceptionType.NATIVE, "Current writeBuffer shouldn't be resized");
        }

        @Override
        public void close(WriteBuffer writeBuffer) {
            arena.close();
        }
    }

    /**
     *   Reserved writeBufferPolicy, the initial memorySegment will be reserved and never released, following resize operation is the same as DefaultWriteBufferPolicy
     */
    static final class ReservedWriteBufferPolicy implements WriteBufferPolicy {
        private Arena arena = null;

        @Override
        public void resize(WriteBuffer writeBuffer, long nextIndex) {
            long newLen = Math.max(nextIndex, writeBuffer.size() << 1);
            if(newLen < 0) {
                throw new FrameworkException(ExceptionType.NATIVE, "MemorySize overflow");
            }
            if(arena == null) {
                arena = Arena.ofConfined();
            }
            MemorySegment newSegment = arena.allocateArray(ValueLayout.JAVA_BYTE, newLen);
            MemorySegment.copy(writeBuffer.segment, 0, newSegment, 0, writeBuffer.writeIndex);
            writeBuffer.segment = newSegment;
            writeBuffer.size = newLen;
        }

        @Override
        public void close(WriteBuffer writeBuffer) {
            if(arena != null) {
                arena.close();
            }
        }
    }

    static final class HeapWriteBufferPolicy implements WriteBufferPolicy {
        private byte[] data;

        public HeapWriteBufferPolicy(byte[] data) {
            this.data = data;
        }
        @Override
        public void resize(WriteBuffer writeBuffer, long nextIndex) {
            if(nextIndex > Integer.MAX_VALUE) {
                throw new FrameworkException(ExceptionType.NATIVE, "Heap writeBuffer size overflow");
            }
            int newLen = Math.max((int) nextIndex, data.length << 1);
            if(newLen < 0) {
                throw new FrameworkException(ExceptionType.NATIVE, "MemorySize overflow");
            }
            byte[] newData = new byte[newLen];
            System.arraycopy(data, 0, newData, 0, (int) writeBuffer.writeIndex);
            data = newData;
            writeBuffer.segment = MemorySegment.ofArray(newData);
            writeBuffer.size = newLen;
        }

        @Override
        public void close(WriteBuffer writeBuffer) {
            // No external close operation needed for heapWriteBuffer
        }
    }
}
