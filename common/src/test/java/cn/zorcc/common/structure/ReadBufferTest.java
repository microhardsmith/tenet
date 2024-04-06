package cn.zorcc.common.structure;

import cn.zorcc.common.Constants;
import cn.zorcc.common.util.NativeUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.ThreadLocalRandom;

public class ReadBufferTest {
    private ReadBuffer createReadBufferWithOneSep(long size, byte sep) {
        MemorySegment segment = Allocator.HEAP.allocate(size + 1);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for(int i = 0; i < size; i++) {
            NativeUtil.setByte(segment, i, (byte) random.nextInt(48, 58));
        }
        NativeUtil.setByte(segment, size, sep);
        return new ReadBuffer(segment);
    }

    private ReadBuffer createReadBufferWithOneSep(long size, long index, byte sep) {
        MemorySegment segment = Allocator.HEAP.allocate(size + 1);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for(int i = 0; i < index; i++) {
            NativeUtil.setByte(segment, i, (byte) random.nextInt(48, 58));
        }
        for(long i = index; i <= size; i++) {
            NativeUtil.setByte(segment, i, sep);
        }
        return new ReadBuffer(segment);
    }

    private ReadBuffer createReadBufferWithTwoSep(long size, byte sep, byte sep2) {
        MemorySegment segment = Allocator.HEAP.allocate(size + 2);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for(int i = 0; i < size; i++) {
            NativeUtil.setByte(segment, i, (byte) random.nextInt(48, 58));
        }
        NativeUtil.setByte(segment, size, sep);
        NativeUtil.setByte(segment, size + 1, sep2);
        return new ReadBuffer(segment);
    }

    private ReadBuffer createReadBufferWithTwoSep(long size, long index, byte sep, byte sep2) {
        MemorySegment segment = Allocator.HEAP.allocate(size + 2);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for(int i = 0; i < index; i++) {
            NativeUtil.setByte(segment, i, (byte) random.nextInt(48, 58));
        }
        for(long i = index; i <= size - 1; i += 2) {
            NativeUtil.setByte(segment, i, sep);
            NativeUtil.setByte(segment, i + 1, sep2);
        }
        return new ReadBuffer(segment);
    }

    @Test
    public void testReadNull() {
        MemorySegment memorySegment = MemorySegment.ofArray(new byte[1000]);
        memorySegment.fill((byte) '1');
        ReadBuffer readBuffer = new ReadBuffer(memorySegment);
        Assertions.assertNull(readBuffer.readUntil(Constants.NUT));
        long pattern = ReadBuffer.compilePattern(Constants.NUT);
        Assertions.assertNull(readBuffer.readPattern(pattern, Constants.NUT));
    }

    @Test
    public void testReadUntil() {
        for(int i = 0; i < 1000; i++) {
            ReadBuffer r = createReadBufferWithOneSep(i, Constants.NUT);
            MemorySegment segment = r.readUntil(Constants.NUT);
            Assertions.assertNotNull(segment);
            Assertions.assertEquals(segment.byteSize(), i);
        }
        for(int i = 0; i < 1000; i++) {
            ReadBuffer r = createReadBufferWithOneSep(1000, i, Constants.NUT);
            MemorySegment segment = r.readUntil(Constants.NUT);
            Assertions.assertNotNull(segment);
            Assertions.assertEquals(segment.byteSize(), i);
        }
        for(int i = 0; i < 1000; i++) {
            ReadBuffer r = createReadBufferWithTwoSep(i, Constants.CR, Constants.LF);
            MemorySegment segment = r.readUntil(Constants.CR, Constants.LF);
            Assertions.assertNotNull(segment);
            Assertions.assertEquals(segment.byteSize(), i);
        }
        for(int i = 0; i < 1000; i++) {
            ReadBuffer r = createReadBufferWithTwoSep(1000, i, Constants.CR, Constants.LF);
            MemorySegment segment = r.readUntil(Constants.CR, Constants.LF);
            Assertions.assertNotNull(segment);
            Assertions.assertEquals(segment.byteSize(), i);
        }
    }

    @Test
    public void testReadPattern() {
        long pattern = ReadBuffer.compilePattern(Constants.NUT);
        for(int i = 0; i < 1000; i++) {
            ReadBuffer r = createReadBufferWithOneSep(i, Constants.NUT);
            MemorySegment segment = r.readPattern(pattern, Constants.NUT);
            Assertions.assertNotNull(segment);
            Assertions.assertEquals(segment.byteSize(), i);
        }
        for(int i = 0; i < 1000; i++) {
            ReadBuffer r = createReadBufferWithOneSep(1000, i, Constants.NUT);
            MemorySegment segment = r.readPattern(pattern, Constants.NUT);
            Assertions.assertNotNull(segment);
            Assertions.assertEquals(segment.byteSize(), i);
        }
        long pattern2 = ReadBuffer.compilePattern(Constants.CR);
        for(int i = 0; i < 1000; i++) {
            ReadBuffer r = createReadBufferWithTwoSep(i, Constants.CR, Constants.LF);
            MemorySegment segment = r.readPattern(pattern2, Constants.CR, Constants.LF);
            Assertions.assertNotNull(segment);
            Assertions.assertEquals(segment.byteSize(), i);
        }
        for(int i = 0; i < 1000; i++) {
            ReadBuffer r = createReadBufferWithTwoSep(1000, i, Constants.CR, Constants.LF);
            MemorySegment segment = r.readPattern(pattern2, Constants.CR, Constants.LF);
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
        MemorySegment s = new ReadBuffer(segment).readPattern(pattern, Constants.CR, Constants.LF);
        Assertions.assertNotNull(s);
        Assertions.assertEquals(s.byteSize(), 9);
    }
}
