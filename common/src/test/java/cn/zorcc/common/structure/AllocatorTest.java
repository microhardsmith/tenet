package cn.zorcc.common.structure;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public class AllocatorTest {
    @Test
    public void testHeapAllocator() {
        MemorySegment segment = Allocator.HEAP.allocate(ValueLayout.JAVA_INT);
        Assertions.assertEquals(segment.address() % 8, 0);
    }

    @Test
    public void testDirectAllocator() {
        try(Allocator allocator = Allocator.newDirectAllocator()) {
            for(int i = 0; i < 100; i++) {
                MemorySegment segment = allocator.allocate(ValueLayout.JAVA_INT);
                Assertions.assertEquals(segment.address() % 8, 0);
            }
        }
    }
}
