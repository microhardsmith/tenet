package cc.zorcc.tenet.core.test.junit;

import cc.zorcc.tenet.core.Allocator;
import cc.zorcc.tenet.core.Mem;
import cc.zorcc.tenet.core.RpMem;
import cc.zorcc.tenet.core.Std;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 *   This is a test class for Allocator class
 */
public final class AllocatorTest {
    /**
     *   Test simple heap allocation strategy
     */
    @Test
    public void testHeapAllocation() {
        MemorySegment segment = Allocator.HEAP.allocate(ValueLayout.JAVA_INT, 2);
        Std.setInt(segment, 0L, 1);
        Std.setInt(segment, 4L, 2);
        Assertions.assertEquals(Std.getInt(segment, 0L), 1);
        Assertions.assertEquals(Std.getInt(segment, 4L), 2);
        Assertions.assertFalse(segment.isNative());
    }

    /**
     *   Test simple direct memory allocation strategy
     */
    @Test
    public void testDirectAllocation() {
        try(Allocator allocator = Allocator.newDirectAllocator(Mem.DEFAULT)) {
            MemorySegment segment = allocator.allocate(ValueLayout.JAVA_INT, 2);
            Std.setInt(segment, 0L, 1);
            Std.setInt(segment, 4L, 2);
            Assertions.assertEquals(Std.getInt(segment, 0L), 1);
            Assertions.assertEquals(Std.getInt(segment, 4L), 2);
            Assertions.assertTrue(segment.isNative());
        }
    }

    @Test
    public void testRpAllocation() {
        try (Allocator allocator = Allocator.newDirectAllocator(RpMem.acquire())) {
            MemorySegment segment = allocator.allocate(ValueLayout.JAVA_INT, 2);
            Std.setInt(segment, 0L, 1);
            Std.setInt(segment, 4L, 2);
            Assertions.assertEquals(Std.getInt(segment, 0L), 1);
            Assertions.assertEquals(Std.getInt(segment, 4L), 2);
            Assertions.assertTrue(segment.isNative());
        } finally {
            RpMem.release();
        }
    }
}
