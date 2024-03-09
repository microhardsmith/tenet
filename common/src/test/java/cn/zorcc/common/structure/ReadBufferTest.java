package cn.zorcc.common.structure;

import cn.zorcc.common.Constants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;

public class ReadBufferTest {
    private static final ReadBuffer readBuffer;

    static {
        MemorySegment segment = Allocator.HEAP.allocate(Constants.KB);
        segment.setString(0L, STR."\{"0".repeat(100)}12345", StandardCharsets.UTF_8);
        readBuffer = new ReadBuffer(segment);
    }
    @Test
    public void testReadBuffer() {
        Assertions.assertEquals(readBuffer.readByte(), (byte) '0');
        byte[] bytes = readBuffer.readUntil((byte) '1');
        assert bytes != null;
        for(byte b : bytes) {
            Assertions.assertEquals(b, (byte) '0');
        }
        Assertions.assertEquals(bytes.length, 99);
    }
}
