package cn.zorcc.common.structure;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 *   Multiple producer, single consumer, unbounded array queue
 */
public record MpscArrayQueue<T> (
        int chunkSize,
        MemorySegment segment,
        long mask,
        BufferArea bufferArea
) {
    private static final long PADDING_SIZE = 64L;
    private static final VarHandle LONG_HANDLE = ValueLayout.JAVA_LONG.varHandle();
    private static final VarHandle OBJECT_HANDLE = MethodHandles.arrayElementVarHandle(Object[].class);
    private static final Object LINKAGE = new Object();
    private static final long producerLimitOffset = PADDING_SIZE;
    private static final long producerIndexOffset = 2 * PADDING_SIZE + ValueLayout.JAVA_LONG.byteSize();
    private static final long consumerIndexOffset = 3 * PADDING_SIZE + 2 * ValueLayout.JAVA_LONG.byteSize();
    private static final long totalOffset = 4 * PADDING_SIZE + 3 * ValueLayout.JAVA_LONG.byteSize();

    private static final class BufferArea {
        private Object[] producerBuffer;
        private Object[] consumerBuffer;

        BufferArea(int size) {
            this.producerBuffer = this.consumerBuffer = new Object[size];
        }

        public Object[] getProducerBuffer() {
            return producerBuffer;
        }

        public void setProducerBuffer(Object[] producerBuffer) {
            this.producerBuffer = producerBuffer;
        }

        public Object[] getConsumerBuffer() {
            return consumerBuffer;
        }

        public void setConsumerBuffer(Object[] consumerBuffer) {
            this.consumerBuffer = consumerBuffer;
        }
    }

    public static <T> MpscArrayQueue<T> create(int chunkSize) {
        if(chunkSize < 16) {
            throw new FrameworkException(ExceptionType.CONTEXT, "ChunkSize are too small");
        }
        if(chunkSize > 65535) {
            throw new FrameworkException(ExceptionType.CONTEXT, "ChunkSize are too large");
        }
        if(Integer.bitCount(chunkSize) != 1) {
            throw new FrameworkException(ExceptionType.CONTEXT, "ChunkSize must be power of 2");
        }
        MemorySegment segment = Allocator.HEAP.allocate(ValueLayout.JAVA_LONG, totalOffset / ValueLayout.JAVA_LONG.byteSize());
        segment.fill(Constants.NUT);
        long mask = (long) (chunkSize - 1) << 1;
        LONG_HANDLE.setRelease(segment, producerLimitOffset, mask);
        BufferArea bufferArea = new BufferArea(chunkSize + 1);
        return new MpscArrayQueue<>(chunkSize, segment, mask, bufferArea);
    }

    public void offer(T element) {
        if(element == null) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
        }
        Object[] buffer;
        long pIndex;
        for( ; ; ) {
            long producerLimit = (long) LONG_HANDLE.getVolatile(segment, producerLimitOffset);
            pIndex = (long) LONG_HANDLE.getVolatile(segment, producerIndexOffset);
            if ((pIndex & 1L) > 0) {
                continue;
            }
            buffer = bufferArea.getProducerBuffer();
            if (producerLimit <= pIndex) {
                long cIndex = (long) LONG_HANDLE.get(segment, consumerIndexOffset);
                if (cIndex + mask > pIndex) {
                    if (!LONG_HANDLE.compareAndSet(segment, producerLimitOffset, producerLimit, cIndex + mask)) {
                        continue;
                    }
                }else if(LONG_HANDLE.compareAndSet(segment, producerIndexOffset, pIndex, pIndex + 1)) {
                    Object[] newProducerBuffer = new Object[chunkSize + 1];
                    bufferArea.setProducerBuffer(newProducerBuffer);
                    final int offset = (int) (pIndex & mask) >> 1;
                    OBJECT_HANDLE.setRelease(newProducerBuffer, offset, element);
                    OBJECT_HANDLE.setRelease(buffer, chunkSize, newProducerBuffer);
                    LONG_HANDLE.setRelease(segment, producerLimitOffset, pIndex + mask);
                    LONG_HANDLE.setRelease(segment, producerIndexOffset, pIndex + 2);
                    OBJECT_HANDLE.setRelease(buffer, offset, LINKAGE);
                    return ;
                }else {
                    continue;
                }
            }
            if (LONG_HANDLE.compareAndSet(segment, producerIndexOffset, pIndex, pIndex + 2)) {
                break;
            }else {
                Thread.onSpinWait();
            }
        }
        OBJECT_HANDLE.setRelease(buffer, (int) (pIndex & mask) >> 1, element);
    }

    @SuppressWarnings("unchecked")
    public T poll() {
        Object[] buffer = bufferArea.getConsumerBuffer();
        long cIndex = (long) LONG_HANDLE.getVolatile(segment, consumerIndexOffset);
        int offset = (int) (cIndex & mask) >> 1;
        Object element = OBJECT_HANDLE.getVolatile(buffer, offset);
        if (element == null) {
            long pIndex = (long) LONG_HANDLE.getVolatile(segment, producerIndexOffset);
            if (((cIndex - pIndex) & 1L) == 0) {
                return null;
            }
            for( ; ; ) {
                element = OBJECT_HANDLE.getVolatile(buffer, offset);
                if(element == null) {
                    Thread.onSpinWait();
                }else {
                    break;
                }
            }
        }
        if (element == LINKAGE) {
            Object[] consumerBuffer = (Object[]) OBJECT_HANDLE.getVolatile(buffer, chunkSize);
            bufferArea.setConsumerBuffer(consumerBuffer);
            OBJECT_HANDLE.setRelease(buffer, chunkSize, null);
            int nextIndex = (int) (cIndex & mask) >> 1;
            element = OBJECT_HANDLE.getVolatile(consumerBuffer, nextIndex);
            OBJECT_HANDLE.setRelease(consumerBuffer, nextIndex, null);
        }
        OBJECT_HANDLE.setRelease(buffer, offset, null);
        LONG_HANDLE.setRelease(segment, consumerIndexOffset, cIndex + 2);
        return (T) element;
    }

}
