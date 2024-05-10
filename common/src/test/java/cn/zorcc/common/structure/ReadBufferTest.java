package cn.zorcc.common.structure;

import cn.zorcc.common.Constants;
import cn.zorcc.common.util.NativeUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.util.concurrent.ThreadLocalRandom;

public class ReadBufferTest {

    private static final int BATCH = 1000;

    private static byte r(byte... sep) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        loop : for( ; ; ) {
            byte b = (byte) random.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE);
            for(byte s : sep) {
                if(b == s) {
                    continue loop;
                }
            }
            return b;
        }
    }

    private static ReadBuffer createReadBufferWithOneSep(long size, byte sep) {
        MemorySegment segment = Allocator.HEAP.allocate(size + 1);
        for(int i = 0; i < size; i++) {
            NativeUtil.setByte(segment, i, r(sep));
        }
        NativeUtil.setByte(segment, size, sep);
        return new ReadBuffer(segment);
    }

    private static ReadBuffer createReadBufferWithOneSep(long size, long index, byte sep) {
        MemorySegment segment = Allocator.HEAP.allocate(size + 1);
        for(int i = 0; i < index; i++) {
            NativeUtil.setByte(segment, i, r(sep));
        }
        for(long i = index; i <= size; i++) {
            NativeUtil.setByte(segment, i, sep);
        }
        return new ReadBuffer(segment);
    }

    private static ReadBuffer createReadBufferWithTwoSep(long size, byte sep, byte sep2) {
        MemorySegment segment = Allocator.HEAP.allocate(size + 2);
        for(int i = 0; i < size; i++) {
            NativeUtil.setByte(segment, i, r(sep, sep2));
        }
        NativeUtil.setByte(segment, size, sep);
        NativeUtil.setByte(segment, size + 1, sep2);
        return new ReadBuffer(segment);
    }

    private static ReadBuffer createReadBufferWithTwoSep(long size, long index, byte sep, byte sep2) {
        MemorySegment segment = Allocator.HEAP.allocate(size + 2);
        for(int i = 0; i < index; i++) {
            NativeUtil.setByte(segment, i, r(sep, sep2));
        }
        for(long i = index; i <= size - 1; i += 2) {
            NativeUtil.setByte(segment, i, sep);
            NativeUtil.setByte(segment, i + 1, sep2);
        }
        return new ReadBuffer(segment);
    }

    @Test
    public void testReadNull() {
        MemorySegment memorySegment = MemorySegment.ofArray(new byte[BATCH]);
        memorySegment.fill((byte) '1');
        ReadBuffer readBuffer = new ReadBuffer(memorySegment);
        Assertions.assertNull(readBuffer.readUntil((byte) '2'));
        long pattern = ReadBuffer.compilePattern(Constants.NUT);
        Assertions.assertNull(readBuffer.swarReadUntil(pattern, Constants.NUT));
    }

    @Test
    public void testReadUntil() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for(int i = 0; i < BATCH; i++) {
            byte b = (byte) random.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE);
            ReadBuffer r = createReadBufferWithOneSep(i, b);
            MemorySegment segment = r.readUntil(b);
            Assertions.assertNotNull(segment);
            Assertions.assertEquals(segment.byteSize(), i);
        }
        for(int i = 0; i < BATCH; i++) {
            byte b = (byte) random.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE);
            ReadBuffer r = createReadBufferWithOneSep(BATCH, i, b);
            MemorySegment segment = r.readUntil(b);
            Assertions.assertNotNull(segment);
            Assertions.assertEquals(segment.byteSize(), i);
        }
        for(int i = 0; i < BATCH; i++) {
            byte b1 = (byte) random.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE);
            byte b2 = r(b1);
            ReadBuffer r = createReadBufferWithTwoSep(i, b1, b2);
            MemorySegment segment = r.readUntil(b1, b2);
            Assertions.assertNotNull(segment);
            Assertions.assertEquals(segment.byteSize(), i);
        }
        for(int i = 0; i < BATCH; i++) {
            byte b1 = (byte) random.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE);
            byte b2 = r(b1);
            ReadBuffer r = createReadBufferWithTwoSep(BATCH, i, b1, b2);
            MemorySegment segment = r.readUntil(b1, b2);
            Assertions.assertNotNull(segment);
            Assertions.assertEquals(segment.byteSize(), i);
        }
    }

    @Test
    public void testSwarReadUntil() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for(int i = 0; i < BATCH; i++) {
            byte b = (byte) random.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE);
            long pattern = ReadBuffer.compilePattern(b);
            ReadBuffer r = createReadBufferWithOneSep(i, b);
            MemorySegment segment = r.swarReadUntil(pattern, b);
            Assertions.assertNotNull(segment);
            Assertions.assertEquals(segment.byteSize(), i);
        }
        for(int i = 0; i < BATCH; i++) {
            byte b = (byte) random.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE);
            long pattern = ReadBuffer.compilePattern(b);
            ReadBuffer r = createReadBufferWithOneSep(BATCH, i, b);
            MemorySegment segment = r.swarReadUntil(pattern, b);
            Assertions.assertNotNull(segment);
            Assertions.assertEquals(segment.byteSize(), i);
        }
        for(int i = 0; i < BATCH; i++) {
            byte b1 = (byte) random.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE);
            byte b2 = r(b1);
            long pattern = ReadBuffer.compilePattern(b1);
            ReadBuffer r = createReadBufferWithTwoSep(i, b1, b2);
            MemorySegment segment = r.swarReadUntil(pattern, b1, b2);
            Assertions.assertNotNull(segment);
            Assertions.assertEquals(segment.byteSize(), i);
        }
        for(int i = 0; i < BATCH; i++) {
            byte b1 = (byte) random.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE);
            byte b2 = r(b1);
            long pattern = ReadBuffer.compilePattern(b1);
            ReadBuffer r = createReadBufferWithTwoSep(BATCH, i, b1, b2);
            MemorySegment segment = r.swarReadUntil(pattern, b1, b2);
            Assertions.assertNotNull(segment);
            Assertions.assertEquals(segment.byteSize(), i);
        }
    }

    @Test
    public void testDuplicatePattern() {
        long pattern = ReadBuffer.compilePattern(Constants.CR);
        MemorySegment segment = Allocator.HEAP.allocate(11);
        segment.asSlice(0, 10).fill(Constants.CR);
        NativeUtil.setByte(segment, 10, Constants.LF);
        MemorySegment s = new ReadBuffer(segment).swarReadUntil(pattern, Constants.CR, Constants.LF);
        Assertions.assertNotNull(s);
        Assertions.assertEquals(s.byteSize(), 9);
        MemorySegment s2 = new ReadBuffer(segment).readUntil(Constants.CR, Constants.LF);
        Assertions.assertNotNull(s2);
        Assertions.assertEquals(s2.byteSize(), 9);
    }

    private static MemorySegment generatePatternSearchData(int index, byte target) {
        long[] bytes = new long[1];
        MemorySegment memorySegment = MemorySegment.ofArray(bytes);
        for (int i = 0; i < 8; i++) {
            memorySegment.set(ValueLayout.JAVA_BYTE, i, r(target));
        }
        if(index >= 0 && index < 8) {
            memorySegment.set(ValueLayout.JAVA_BYTE, index, target);
        }
        return memorySegment;
    }

    @Test
    public void testPatternSearch() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        byte b = (byte) random.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE);
        long pattern = ReadBuffer.compilePattern(b);
        for(int i = 0; i < 16; i++) {
            MemorySegment m1 = generatePatternSearchData(i, b);
            long d1 = m1.get(ValueLayout.JAVA_LONG, 0L);
            int i1 = ReadBuffer.searchPattern(d1, pattern, ByteOrder.LITTLE_ENDIAN);
            Assertions.assertEquals(Math.min(i, 8), i1);

            MemorySegment m2 = generatePatternSearchData(i, b);
            long d2 = m2.get(ValueLayout.JAVA_LONG.withOrder(ByteOrder.BIG_ENDIAN), 0L);
            int i2 = ReadBuffer.searchPattern(d2, pattern, ByteOrder.BIG_ENDIAN);
            Assertions.assertEquals(Math.min(i, 8), i2);
        }
    }
}
