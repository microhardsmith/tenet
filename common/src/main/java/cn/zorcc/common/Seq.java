package cn.zorcc.common;


import java.lang.foreign.MemorySegment;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public final class Seq {
    private static final int TIMESTAMP_BYTES = 8;
    private static final int APP_ID_BYTES = 3;
    private static final int NODE_ID_BYTES = 2;
    private static final int SEQ_BYTES = 4;
    private static final int IDENTIFIER_BYTES = 2;
    private static final int TOTAL_BYTES = TIMESTAMP_BYTES + (APP_ID_BYTES + NODE_ID_BYTES) << 1 + SEQ_BYTES + IDENTIFIER_BYTES;
    private static final AtomicInteger sequence = new AtomicInteger(new Random().nextInt());

    public MemorySegment create() {
        return create(Clock.current());
    }

    public MemorySegment create(long timestamp) {
        // TODO
        return null;
    }
}
