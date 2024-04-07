package cn.zorcc.common.structure;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;

public class MemApiTest {
    @Test
    public void testDefaultMemApi() {
        MemorySegment memorySegment = MemApi.DEFAULT.allocateMemory(8L).reinterpret(8L);
        try{
            Assertions.assertEquals(memorySegment.byteSize(), 8L);
            Assertions.assertTrue(memorySegment.isNative());
        }finally {
            MemApi.DEFAULT.freeMemory(memorySegment);
        }
    }
}
