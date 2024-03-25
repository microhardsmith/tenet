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
public sealed interface Allocator extends SegmentAllocator, AutoCloseable permits Allocator.HeapAllocator, Allocator.DirectAllocator, Allocator.SlicingAllocator {

    /**
     *   Global heap allocator
     */
    Allocator HEAP = new HeapAllocator();

    /**
     *   Creating a new Direct memory Allocator
     */
    static Allocator newDirectAllocator() {
        return new DirectAllocator();
    }

    /**
     *   Creating a new Slicing memory Allocator
     */
    static Allocator newSlicingAllocator(MemorySegment memorySegment) {
        return new SlicingAllocator(memorySegment);
    }

    boolean isNative();

    @Override
    void close();

    /**
     *   Allocator for heap memory usage, The global HEAP instance should be used instead of creating a new one
     */
    final class HeapAllocator implements Allocator {

        @Override
        public MemorySegment allocate(long byteSize, long byteAlignment) {
            int size = Math.toIntExact(byteSize);
            MemorySegment memorySegment = switch (Math.toIntExact(byteAlignment)) {
                case Byte.BYTES -> MemorySegment.ofArray(new byte[size]);
                case Short.BYTES -> MemorySegment.ofArray(new short[calculateSize(byteSize, 1)]);
                case Integer.BYTES -> MemorySegment.ofArray(new int[calculateSize(byteSize, 2)]);
                case Long.BYTES -> MemorySegment.ofArray(new long[calculateSize(byteSize, 3)]);
                default -> throw new FrameworkException(ExceptionType.NATIVE, STR."Unexpected alignment : \{byteAlignment}");
            };
            return memorySegment.byteSize() == byteSize ? memorySegment.asSlice(0L, byteSize) : memorySegment;
        }

        @Override
        public boolean isNative() {
            return false;
        }

        @Override
        public void close() {
            // No external operations needed for heap allocator
        }

        private static int calculateSize(long byteSize, long shift) {
            long size = (byteSize + 1) >> shift;
            return Math.toIntExact(size);
        }
    }

    /**
     *   DirectAllocator using malloc and free for native memory allocation, each allocation were recorded into the long array
     */
    final class DirectAllocator implements Allocator {
        private static final int SIZE = 8;
        private long[] pointers;
        private int index = 0;

        @Override
        public MemorySegment allocate(long byteSize, long byteAlignment) {
            switch (Math.toIntExact(byteAlignment)) {
                case Byte.BYTES, Short.BYTES, Integer.BYTES, Long.BYTES -> {
                    MemorySegment ptr = NativeUtil.malloc(byteSize).reinterpret(byteSize);
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
                NativeUtil.free(MemorySegment.ofAddress(pointers[i]));
            }
        }
    }

    /**
     *   Slicing allocator is a very dangerous allocator which should be directly used by developers
     *   It always allocates memory by slicing the initial segment, and exception would be thrown if there are no enough places for that
     *   So, it's the developers duty to make sure each allocation should be tiny and fast given back
     */
    record SlicingAllocator(
            MemorySegment segment,
            LongHolder indexHolder
    ) implements Allocator {
        SlicingAllocator(MemorySegment memorySegment) {
            this(memorySegment, new LongHolder(0L));
        }

        @Override
        public MemorySegment allocate(long byteSize, long byteAlignment) {
            switch (Math.toIntExact(byteAlignment)) {
                case Byte.BYTES, Short.BYTES, Integer.BYTES, Long.BYTES -> {
                    long currentIndex = indexHolder.getValue();
                    long address = segment.address();
                    long offset = ((address + currentIndex + byteAlignment - 1) & (-byteAlignment)) - address;
                    long nextIndex = offset + byteSize;
                    if(nextIndex > segment.byteSize()) {
                        throw new FrameworkException(ExceptionType.NATIVE, STR."SlicingAllocator overflow : \{nextIndex}");
                    }
                    indexHolder.setValue(nextIndex);
                    return segment.asSlice(offset, byteSize);
                }
                default -> throw new FrameworkException(ExceptionType.NATIVE, STR."Unexpected alignment : \{byteAlignment}");
            }
        }

        @Override
        public boolean isNative() {
            return segment.isNative();
        }

        @Override
        public void close() {
            // No external operations needed for slicing allocator
        }
    }


}
