package cn.zorcc.common;


import cn.zorcc.common.structure.Allocator;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *   Global seq generator, default representation would be using 16 bytes, so it can be simplified as two long value writing
 */
public final class Seq {
    private static final int TIMESTAMP_BYTES = 6;
    private static final int APP_ID_BYTES = 2;
    private static final int NODE_ID_BYTES = 1;
    private static final int SEQ_BYTES = 4;
    private static final AtomicInteger sequence = new AtomicInteger(new Random().nextInt());

    private Seq() {
        throw new UnsupportedOperationException();
    }

    public static MemorySegment create(int currentAppId, int currentNodeId, int targetAppId, int targetNodeId) {
        return create(Clock.current(), currentAppId, currentNodeId, targetAppId, targetNodeId);
    }

    public static MemorySegment create(long timestamp, long currentAppId, long currentNodeId, long targetAppId, long targetNodeId) {
        MemorySegment segment = Allocator.HEAP.allocate(ValueLayout.JAVA_LONG, 2);
        long l1 = (timestamp << 16) | currentAppId;
        long l2 = (currentNodeId << 56) | (targetAppId << 40) | (targetNodeId << 32) | sequence.getAndIncrement();
        segment.set(ValueLayout.JAVA_LONG, 0L, l1);
        segment.set(ValueLayout.JAVA_LONG, 8L, l2);
        return segment;
    }
}
