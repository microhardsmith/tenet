package cc.zorcc.tenet.core;

import cc.zorcc.tenet.core.bindings.SysBinding;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;

/**
 *   WriteBuffer represents a bunk of heap or non-heap memory, which could be resized and written into
 */
public final class WriteBuffer implements AutoCloseable {
    /**
     *   WriteBuffer resizing policy control
     */
    sealed interface WriteBufferPolicy permits NativeWriteBufferPolicy, ReservedWriteBufferPolicy, HeapWriteBufferPolicy {
        /**
         *   Resize target writeBuffer to contain more bytes than nextIndex, this function will not change the writeIndex of the writeBufferData
         */
        MemorySegment resize(MemorySegment segment, long currentIndex, long nextIndex);

        /**
         *   Close current writeBuffer after using
         */
        void close(MemorySegment segment);
    }

    private MemorySegment segment;
    private long writeIndex;
    private final WriteBufferPolicy policy;

    /**
     *   Exposed by static methods
     */
    private WriteBuffer(MemorySegment segment, WriteBufferPolicy policy) {
        this.segment = segment;
        this.writeIndex = 0L;
        this.policy = policy;
    }

    /**
     *   Create a writeBuffer using native memory
     */
    public static WriteBuffer newNativeWriteBuffer (Mem mem, long initialSize) {
        return new WriteBuffer(MemorySegment.NULL, new NativeWriteBufferPolicy(mem, initialSize));
    }

    /**
     *   Create a writeBuffer using native memory, first chunk reused
     */
    public static WriteBuffer newReservedWriteBuffer(Mem mem, MemorySegment segment) {
        if(segment == null || !segment.isNative() || segment.address() == 0L) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED);
        }
        return new WriteBuffer(segment, new ReservedWriteBufferPolicy(mem, segment));
    }

    /**
     *   Create a writeBuffer using heap memory
     */
    public static WriteBuffer newHeapWriteBuffer(long initialSize) {
        return new WriteBuffer(MemorySegment.NULL, new HeapWriteBufferPolicy(initialSize));
    }

    /**
     *   Get current writeIndex
     */
    public long writeIndex() {
        return writeIndex;
    }

    @Override
    public void close() {
        policy.close(segment);
    }

    /**
     *   Write a byte value
     */
    public void writeByte(byte b) {
        long nextIndex = writeIndex + Byte.BYTES;
        if(nextIndex > segment.byteSize()) {
            segment = policy.resize(segment, writeIndex, nextIndex);
        }
        Std.setByte(segment, writeIndex, b);
        writeIndex = nextIndex;
    }

    public void writeBytes(byte b1, byte b2) {
        long nextIndex = writeIndex + Byte.BYTES * 2;
        if(nextIndex > segment.byteSize()) {
            segment = policy.resize(segment, writeIndex, nextIndex);
        }
        Std.setByte(segment, writeIndex, b1);
        Std.setByte(segment, writeIndex + Byte.BYTES, b2);
        writeIndex = nextIndex;
    }

    public void writeBytes(byte b1, byte b2, byte b3) {
        long nextIndex = writeIndex + Byte.BYTES * 3;
        if(nextIndex > segment.byteSize()) {
            segment = policy.resize(segment, writeIndex, nextIndex);
        }
        Std.setByte(segment, writeIndex, b1);
        Std.setByte(segment, writeIndex + Byte.BYTES, b2);
        Std.setByte(segment, writeIndex + Byte.BYTES * 2, b3);
        writeIndex = nextIndex;
    }

    /**
     *   Write bytes into current writeBuffer, from target offset with target length
     *   JDK would do boundary checking for us internally
     */
    public void writeBytes(byte[] bytes, int off, int len) {
        long nextIndex = writeIndex + len;
        if(nextIndex > segment.byteSize()) {
            segment = policy.resize(segment, writeIndex, nextIndex);
        }
        MemorySegment.copy(bytes, off, segment, ValueLayout.JAVA_BYTE, writeIndex, len);
        writeIndex = nextIndex;
    }

    public void writeBytes(byte[] bytes) {
        writeBytes(bytes, 0, bytes.length);
    }

    public void writeShort(short s) {
        long nextIndex = writeIndex + Short.BYTES;
        if(nextIndex > segment.byteSize()) {
            segment = policy.resize(segment, writeIndex, nextIndex);
        }
        Std.setShort(segment, writeIndex, s);
        writeIndex = nextIndex;
    }

    public void writeInt(int i) {
        long nextIndex = writeIndex + Integer.BYTES;
        if(nextIndex > segment.byteSize()) {
            segment = policy.resize(segment, writeIndex, nextIndex);
        }
        Std.setInt(segment, writeIndex, i);
        writeIndex = nextIndex;
    }

    public void writeLong(long l) {
        long nextIndex = writeIndex + Long.BYTES;
        if(nextIndex > segment.byteSize()) {
            segment = policy.resize(segment, writeIndex, nextIndex);
        }
        Std.setLong(segment, writeIndex, l);
        writeIndex = nextIndex;
    }

    public void writeFloat(float f) {
        long nextIndex = writeIndex + Float.BYTES;
        if(nextIndex > segment.byteSize()) {
            segment = policy.resize(segment, writeIndex, nextIndex);
        }
        Std.setFloat(segment, writeIndex, f);
        writeIndex = nextIndex;
    }

    public void writeDouble(double d) {
        long nextIndex = writeIndex + Double.BYTES;
        if(nextIndex > segment.byteSize()) {
            segment = policy.resize(segment, writeIndex, nextIndex);
        }
        Std.setDouble(segment, writeIndex, d);
        writeIndex = nextIndex;
    }

    /**
     *   Writing a UTF-8 string to current writeBuffer
     *   Note that this function is provided for ease of use, it's not as performant as JDK's memorySegment.writeString() function, but we could get the precise writeIndex
     *   Currently we could only provide this, because accessing String's internal byte array from outside of JDK is completely unsafe
     */
    public void writeUtf8Str(String str) {
        MemorySegment m = MemorySegment.ofArray(str.getBytes(StandardCharsets.UTF_8));
        long nextIndex = writeIndex + m.byteSize() + 1;
        if(nextIndex > segment.byteSize()) {
            segment = policy.resize(segment, writeIndex, nextIndex);
        }
        SysBinding.memcpy(segment.asSlice(writeIndex), m, m.byteSize());
        Std.setByte(segment, writeIndex + m.byteSize(), (byte) '\0');
        writeIndex = nextIndex;
    }

    public void writeSegment(MemorySegment m) {
        long nextIndex = writeIndex + m.byteSize();
        if(nextIndex > segment.byteSize()) {
            segment = policy.resize(segment, writeIndex, nextIndex);
        }
        SysBinding.memcpy(segment.asSlice(writeIndex), m, m.byteSize());
        writeIndex = nextIndex;
    }

    public void writeSegmentWithPadding(MemorySegment m, long minWidth, byte padding) {
        if(minWidth <= m.byteSize()) {
            writeSegment(m);
        }else {
            long nextIndex = writeIndex + minWidth;
            if(nextIndex > segment.byteSize()) {
                segment = policy.resize(segment, writeIndex, nextIndex);
            }
            SysBinding.memcpy(segment.asSlice(writeIndex), m, m.byteSize());
            segment.asSlice(writeIndex + m.byteSize(), minWidth - m.byteSize()).fill(padding);
            writeIndex = nextIndex;
        }
    }

    public void writeUtf8Data(int data) {
        if(data < 0x80) {
            writeByte((byte) data);
        }else if(data < 0x800) {
            writeBytes((byte) (0xC0 | (data >> 6)), (byte) (0x80 | (data & 0x3F)));
        }else {
            writeBytes((byte) (0xE0 | (data >> 12)), (byte) (0x80 | ((data >> 6) & 0x3F)), (byte) (0x80 | (data & 0x3F)));
        }
    }

    /**
     *   Return current written segment, note that this function won't reset the writeIndex
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
     *   Native writeBufferPolicy using malloc, realloc and free
     */
    record NativeWriteBufferPolicy(
            Mem mem,
            long initialSize
    ) implements WriteBufferPolicy {
        @Override
        public MemorySegment resize(MemorySegment segment, long currentIndex, long nextIndex) {
            MemorySegment newSegment = segment == MemorySegment.NULL ?
                    mem.allocateMemory(Math.max(initialSize, Std.grow(nextIndex))) :
                    mem.reallocateMemory(segment, Std.grow(nextIndex));
            if(newSegment.address() == 0L) {
                throw new OutOfMemoryError();
            }
            return newSegment;
        }

        @Override
        public void close(MemorySegment segment) {
            if(segment != MemorySegment.NULL) {
                mem.freeMemory(segment);
            }
        }
    }

    /**
     *  Reserved writeBufferPolicy, the initial memorySegment must be native memory, which would be reserved and never released
     */
    record ReservedWriteBufferPolicy(
            Mem mem,
            MemorySegment initialSegment
    ) implements WriteBufferPolicy {

        @Override
        public MemorySegment resize(MemorySegment segment, long currentIndex, long nextIndex) {
            MemorySegment newSegment = segment.address() == initialSegment.address() ?
                    mem.allocateMemory(Std.grow(nextIndex)) :
                    mem.reallocateMemory(segment, Std.grow(nextIndex));
            if(newSegment.address() == 0L) {
                throw new OutOfMemoryError();
            }
            return newSegment;
        }

        @Override
        public void close(MemorySegment segment) {
            if(segment.address() != initialSegment.address()) {
                mem.freeMemory(segment);
            }
        }
    }


    /**
     *   HeapWriteBufferPolicy writes all the data to the heap, let GC worry about the clean-up
     */
    record HeapWriteBufferPolicy(
            long initialSize
    ) implements WriteBufferPolicy {
        @Override
        public MemorySegment resize(MemorySegment segment, long currentIndex, long nextIndex) {
            if(segment == MemorySegment.NULL) {
                return Allocator.HEAP.allocate(Math.max(initialSize, Std.grow(nextIndex)));
            }else {
                MemorySegment newSegment = Allocator.HEAP.allocate(Std.grow(nextIndex));
                byte[] b1 = (byte[]) segment.heapBase().orElseThrow(() -> new TenetException(ExceptionType.NATIVE, "Not a heap segment"));
                byte[] b2 = (byte[]) newSegment.heapBase().orElseThrow(() -> new TenetException(ExceptionType.NATIVE, "Not a heap segment"));
                System.arraycopy(b1, 0, b2, 0, Math.toIntExact(currentIndex));
                return newSegment;
            }
        }

        @Override
        public void close(MemorySegment segment) {
            // No external close operation needed for heapWriteBuffer
        }
    }
}
