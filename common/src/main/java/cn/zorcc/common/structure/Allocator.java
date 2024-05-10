package cn.zorcc.common.structure;

import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.util.Arrays;

/**
 *   Allocator is a substitute for Arena, using malloc, realloc and free, which avoids internal checking and thus having much better performance
 */
public sealed interface Allocator extends SegmentAllocator, AutoCloseable permits Allocator.HeapAllocator, Allocator.DirectAllocator {

    /**
     *   Global heap allocator which delegates to free action to GC, relies on array for allocation
     */
    Allocator HEAP = new HeapAllocator();

    /**
     *   Create a new Direct memory Allocator using target MemApi, using DirectAllocator is a dangerous move, and should always be used as try-with-resources term
     */
    static Allocator newDirectAllocator(MemApi memApi) {
        return new DirectAllocator(memApi);
    }

    /**
     *   check allocated byteSize, must be within the range of Integer, which is 2GB for 64bits operating system
     */
    static void checkByteSize(long byteSize) {
        if(byteSize <= 0L || byteSize > Integer.MAX_VALUE) {
            throw new FrameworkException(ExceptionType.NATIVE, "ByteSize overflow");
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
     *   Allocator for heap memory usage, The global HEAP instance should be used instead of creating a new one
     *   Heap allocator is generally much slower than the non-heap one, because it's always requires to be zero-out
     *   However, escape analyse may work and allocation goes to stack rather than heap, which makes the overall performance much better
     */
    final class HeapAllocator implements Allocator {

        private HeapAllocator() {
            // external usage are forbidden
        }

        @Override
        public MemorySegment allocate(long byteSize, long byteAlignment) {
            checkByteSize(byteSize);
            switch (Math.toIntExact(byteAlignment)) {
                case Byte.BYTES -> {
                    // This is quite important, which makes the byte allocation will always be backed by a byte[] which could be fetched by heapBase()
                    return MemorySegment.ofArray(new byte[Math.toIntExact(byteSize)]);
                }
                case Short.BYTES, Integer.BYTES, Long.BYTES -> {
                    // A fair amount of waste wouldn't harm the system
                    MemorySegment m = MemorySegment.ofArray(new long[Math.toIntExact((byteSize + 7) >> 3)]);
                    return m.byteSize() == byteSize ? m : m.asSlice(0L, byteSize);
                }
                default -> throw new FrameworkException(ExceptionType.NATIVE, "Unexpected alignment : %d".formatted(byteAlignment));
            }
        }

        @Override
        public boolean isNative() {
            return false;
        }

        @Override
        public void close() {
            // No external operations needed for heap allocator
        }
    }

    final class DirectAllocator implements Allocator {
        /**
         *   Normally, an allocator won't create many chunks at once, so let's start at a small value first
         */
        private static final int SIZE = 8;
        private final MemApi memApi;
        private MemorySegment[] pointers;
        private int index = 0;

        public DirectAllocator(MemApi memApi) {
            this.memApi = memApi;
        }

        @Override
        public MemorySegment allocate(long byteSize, long byteAlignment) {
            checkByteSize(byteSize);
            switch (Math.toIntExact(byteAlignment)) {
                case Byte.BYTES, Short.BYTES, Integer.BYTES, Long.BYTES -> {
                    // if malloc returns a NULL pointer, reinterpret would still work
                    MemorySegment ptr = memApi.allocateMemory(byteSize).reinterpret(byteSize);
                    if(NativeUtil.checkNullPointer(ptr)) {
                        throw new OutOfMemoryError();
                    }
                    // lazy initialization
                    if(pointers == null) {
                        pointers = new MemorySegment[SIZE];
                    }
                    if(index == pointers.length) {
                        pointers = Arrays.copyOf(pointers, NativeUtil.grow(pointers.length));
                    }
                    pointers[index++] = ptr;
                    return ptr;
                }
                default -> throw new FrameworkException(ExceptionType.NATIVE, "Unexpected alignment : %d".formatted(byteAlignment));
            }
        }

        @Override
        public boolean isNative() {
            return true;
        }

        @Override
        public void close() {
            if(pointers != null) {
                final int len = index;
                for(int i = 0; i < len; i++) {
                    memApi.freeMemory(pointers[i]);
                }
            }
        }
    }


}
