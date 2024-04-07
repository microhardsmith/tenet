package cn.zorcc.common.context;

import cn.zorcc.common.structure.Allocator;
import cn.zorcc.common.structure.MemApi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;

public class AllocatorTest {
    @Test
    public void testAllocator() {
        try(Allocator allocator = Allocator.newDirectAllocator(MemApi.DEFAULT)) {
            MemorySegment abc = allocator.allocateFrom("abc");
            Assertions.assertEquals(abc.byteSize(), 4);
        }
    }
}
