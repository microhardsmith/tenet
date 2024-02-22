package cn.zorcc.common.context;

import cn.zorcc.common.structure.Allocator;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;

public class AllocatorTest {
    @Test
    public void testAllocator() {
        try(Allocator allocator = Allocator.newDirectAllocator()) {
            MemorySegment abc = allocator.allocateFrom("abc");
            System.out.println(abc.byteSize());
        }
    }
}
