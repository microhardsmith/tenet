package cc.zorcc.tenet.core;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *  Allocator is a substitute for JDK provided Arena, using malloc, realloc and free, which avoids internal checking and thus having much better performance
 */
public sealed interface Allocator extends SegmentAllocator, AutoCloseable permits Allocator.HeapAllocator, Allocator.DirectAllocator {

    /**
     *   Create a new Heap memory allocator, relies on array for allocation
     */
    static Allocator newHeapAllocator() {
        final class Holder {
            private static final Allocator HEAP = new HeapAllocator();
        }
        return Holder.HEAP;
    }

    /**
     *   Create a new Direct memory Allocator using target MemApi, using DirectAllocator is a dangerous move, and should always be used as try-with-resources term
     */
    static Allocator newDirectAllocator(Mem mem) {
        return new DirectAllocator(mem);
    }

    /**
     *   check allocated byteSize, must be within the range of Integer, which is 2GB for 64bits operating system
     */
    static void checkByteSize(long byteSize) {
        if(byteSize <= 0L || byteSize > Integer.MAX_VALUE) {
            throw new TenetException(ExceptionType.NATIVE, "ByteSize overflow");
        }
    }

    /**
     *   Return if current allocator using native memory
     */
    boolean isNative();

    /**
     *   Release all the memory allocated by current allocator
     */
    @Override
    void close();

    /**
     *   Allocating memorySegment using JVM heap memory
     */
    final class HeapAllocator implements Allocator {
        private static final AtomicBoolean initialized = new AtomicBoolean(false);
        private static final int BYTE_SHIFT = Integer.numberOfTrailingZeros(Byte.BYTES);
        private static final int SHORT_SHIFT = Integer.numberOfTrailingZeros(Short.BYTES);
        private static final int INTEGER_SHIFT = Integer.numberOfTrailingZeros(Integer.BYTES);
        private static final int LONG_SHIFT = Integer.numberOfTrailingZeros(Long.BYTES);

        private HeapAllocator() {
            if(!initialized.compareAndSet(false, true)) {
                throw new TenetException(ExceptionType.NATIVE, "HeapAllocator shouldn't be created, use Allocator.HEAP instead");
            }
        }

        @Override
        public boolean isNative() {
            return false;
        }

        @Override
        public MemorySegment allocate(long byteSize, long byteAlignment) {
            checkByteSize(byteSize);
            switch (Math.toIntExact(byteAlignment)) {
                case Byte.BYTES -> {
                    MemorySegment m = MemorySegment.ofArray(new byte[Math.toIntExact(byteSize + Byte.BYTES - 1) >> BYTE_SHIFT]);
                    return m.byteSize() == byteSize ? m : m.asSlice(0L, byteSize);
                }
                case Short.BYTES -> {
                    MemorySegment m = MemorySegment.ofArray(new short[Math.toIntExact(byteSize + Short.BYTES - 1) >> SHORT_SHIFT]);
                    return m.byteSize() == byteSize ? m : m.asSlice(0L, byteSize);
                }
                case Integer.BYTES -> {
                    MemorySegment m = MemorySegment.ofArray(new int[Math.toIntExact(byteSize + Integer.BYTES - 1) >> INTEGER_SHIFT]);
                    return m.byteSize() == byteSize ? m : m.asSlice(0L, byteSize);
                }
                case Long.BYTES -> {
                    MemorySegment m = MemorySegment.ofArray(new long[Math.toIntExact((byteSize + Long.BYTES - 1) >> LONG_SHIFT)]);
                    return m.byteSize() == byteSize ? m : m.asSlice(0L, byteSize);
                }
                default -> throw new TenetException(ExceptionType.NATIVE, "Unexpected alignment : %d".formatted(byteAlignment));
            }
        }

        @Override
        public void close() {
            // No external operations needed for heap allocator
        }
    }

    /**
     *   Allocating memorySegment using target Mem
     */
    final class DirectAllocator implements Allocator {
        /**
         *  Normally, an allocator won't create many chunks at once, so let's start at a small value first, this value could be tuned
         */
        private static final int INITIAL_CAPACITY = 4;
        private final Mem mem;
        private MemorySegment[] ptrs;
        private int index = 0;

        private DirectAllocator(Mem mem) {
            this.mem = mem;
        }

        @Override
        public boolean isNative() {
            return true;
        }

        @Override
        public MemorySegment allocate(long byteSize, long byteAlignment) {
            checkByteSize(byteSize);
            switch (Math.toIntExact(byteAlignment)) {
                case Byte.BYTES, Short.BYTES, Integer.BYTES, Long.BYTES -> {
                    // if malloc returns a NULL pointer, reinterpret would still work
                    MemorySegment ptr = Std.requireNonNull(mem.allocateMemory(byteSize).reinterpret(byteSize), OutOfMemoryError::new);
                    // lazy initialization
                    if(ptrs == null) {
                        ptrs = new MemorySegment[INITIAL_CAPACITY];
                    }
                    // resize on expansion
                    if(index == ptrs.length) {
                        ptrs = Arrays.copyOf(ptrs, Std.grow(ptrs.length));
                    }
                    ptrs[index++] = ptr;
                    return ptr;
                }
                default -> throw new TenetException(ExceptionType.NATIVE, "Unexpected alignment : %d".formatted(byteAlignment));
            }
        }

        @Override
        public void close() {
            if(ptrs != null) {
                final int len = index;
                for(int i = 0; i < len; i++) {
                    mem.freeMemory(ptrs[i]);
                }
            }
        }
    }
}
