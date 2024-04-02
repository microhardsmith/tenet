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
     *   Global heap allocator
     */
    Allocator HEAP = new HeapAllocator();

    /**
     *   Create a new Direct memory Allocator using target MemApi
     */
    static Allocator newDirectAllocator(MemApi memApi) {
        return new DirectAllocator(memApi);
    }

    /**
     *   Create a new Direct memory Allocator using System MemApi
     */
    static Allocator newDirectAllocator() {
        return newDirectAllocator(MemApi.DEFAULT);
    }

    static void checkByteSize(long byteSize) {
        if(byteSize <= 0L) {
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
     */
    final class HeapAllocator implements Allocator {
        // A fair amount of waste wouldn't harm the system
        @Override
        public MemorySegment allocate(long byteSize, long byteAlignment) {
            checkByteSize(byteSize);
            MemorySegment memorySegment = switch (Math.toIntExact(byteAlignment)) {
                case Byte.BYTES, Short.BYTES, Integer.BYTES, Long.BYTES -> MemorySegment.ofArray(new long[Math.toIntExact((byteSize + 7) >> 3)]);
                default -> throw new FrameworkException(ExceptionType.NATIVE, STR."Unexpected alignment : \{byteAlignment}");
            };
            return memorySegment.byteSize() == byteSize ? memorySegment : memorySegment.asSlice(0L, byteSize);
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

    /**
     *   DirectAllocator using malloc and free for native memory allocation, each allocation were recorded into the long array
     */
    final class DirectAllocator implements Allocator {
        private static final int SIZE = 8;
        private final MemApi memApi;
        private long[] pointers;
        private int index = 0;

        public DirectAllocator(MemApi memApi) {
            this.memApi = memApi;
        }

        @Override
        public MemorySegment allocate(long byteSize, long byteAlignment) {
            checkByteSize(byteSize);
            switch (Math.toIntExact(byteAlignment)) {
                case Byte.BYTES, Short.BYTES, Integer.BYTES, Long.BYTES -> {
                    // if malloc returns a NULL pointer, reinterpret still works
                    MemorySegment ptr = memApi.allocateMemory(byteSize).reinterpret(byteSize);
                    if(NativeUtil.checkNullPointer(ptr)) {
                        throw new OutOfMemoryError();
                    }
                    if(pointers == null) {
                        pointers = new long[SIZE];
                    }
                    if(index == pointers.length) {
                        int newCapacity = pointers.length + (pointers.length >> 1);
                        if(newCapacity < 0) {
                            throw new FrameworkException(ExceptionType.CONTEXT, "Size overflow");
                        }
                        pointers = Arrays.copyOf(pointers, newCapacity);
                    }
                    pointers[index++] = ptr.address();
                    return ptr;
                }
                default -> throw new FrameworkException(ExceptionType.NATIVE, STR."Unexpected alignment : \{byteAlignment}");
            }
        }

        @Override
        public boolean isNative() {
            return true;
        }

        @Override
        public void close() {
            final int len = index;
            for(int i = 0; i < len; i++) {
                memApi.freeMemory(MemorySegment.ofAddress(pointers[i]));
            }
        }
    }


}
