package cn.zorcc.common;


import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.MemorySegment;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *   Global seq generator, default representation would use 16 bytes
 */
public final class Seq {
    private static final int TIMESTAMP_BYTES = 6;
    private static final int APP_ID_BYTES = 2;
    private static final int NODE_ID_BYTES = 1;
    private static final int SEQ_BYTES = 4;
    private static final int TOTAL_BYTES = TIMESTAMP_BYTES + ((APP_ID_BYTES + NODE_ID_BYTES) << 1) + SEQ_BYTES;
    private static final AtomicInteger sequence = new AtomicInteger(new Random().nextInt());

    public static int len() {
        return SEQ_BYTES;
    }

    public static MemorySegment create(int currentAppId, int currentNodeId, int targetAppId, int targetNodeId) {
        return create(Clock.current(), currentAppId, currentNodeId, targetAppId, targetNodeId);
    }

    public static MemorySegment create(long timestamp, long currentAppId, long currentNodeId, long targetAppId, long targetNodeId) {
        MemorySegment memorySegment = MemorySegment.ofArray(new byte[TOTAL_BYTES]);
        long currentIndex = Constants.ZERO;
        currentIndex = write(memorySegment, currentIndex, timestamp, TIMESTAMP_BYTES);
        currentIndex = write(memorySegment, currentIndex, currentAppId, APP_ID_BYTES);
        currentIndex = write(memorySegment, currentIndex, currentNodeId, NODE_ID_BYTES);
        currentIndex = write(memorySegment, currentIndex, targetAppId, APP_ID_BYTES);
        currentIndex = write(memorySegment, currentIndex, targetNodeId, NODE_ID_BYTES);
        write(memorySegment, currentIndex, sequence.getAndIncrement(), SEQ_BYTES);
        return memorySegment;
    }

    private static long write(MemorySegment memorySegment, long currentIndex, long value, int bytes) {
        long nextIndex = currentIndex + bytes;
        while (currentIndex < nextIndex) {
            NativeUtil.setByte(memorySegment, currentIndex, (byte) value);
            value = value >> 8;
            currentIndex++;
        }
        return nextIndex;
    }
}
