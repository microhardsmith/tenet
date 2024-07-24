package cc.zorcc.tenet.core.test.junit;

import cc.zorcc.tenet.core.ReadBuffer;
import cc.zorcc.tenet.core.SearchedResult;
import cc.zorcc.tenet.core.SwarMatcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.concurrent.ThreadLocalRandom;

public final class ReadBufferTest {
    private static final int LENGTH = 10000;
    private static final byte SEP = Byte.MAX_VALUE;

    /**
     *   Creating a readBuffer which could be searched with target length
     */
    private static ReadBuffer createSearchableReadBuffer(int index, int len, byte target) {
        if(index < 0 || index >= len) {
            throw new IndexOutOfBoundsException();
        }
        MemorySegment segment = MemorySegment.ofArray(new byte[len]);
        for(int i = 0; i < index; i++) {
            segment.set(ValueLayout.JAVA_BYTE, i, createRandomByte(target));
        }
        segment.set(ValueLayout.JAVA_BYTE, index, target);
        return new ReadBuffer(segment);
    }

    /**
     *   Creating a random byte to fill the readBuffer, different from target
     */
    private static byte createRandomByte(byte target) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for( ; ; ) {
            byte b = (byte) r.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE);
            if(b != target) {
                return b;
            }
        }
    }

    /**
     *   Test simple linear search algorithm
     */
    @Test
    public void testLinearSearch() {
        for(int index = 0; index < LENGTH; index++) {
            ReadBuffer r = createSearchableReadBuffer(index, LENGTH, SEP);
            Assertions.assertEquals(r.linearSearch(b -> b == SEP), SearchedResult.of(SEP));
            Assertions.assertEquals(index + Byte.BYTES, r.readIndex());
        }
    }

    /**
     *   Test swar search algorithm
     */
    @Test
    public void testSwarSearch() {
        for(int index = 0; index < LENGTH; index++) {
            ReadBuffer r = createSearchableReadBuffer(index, LENGTH, SEP);
            Assertions.assertEquals(r.swarSearch(new SwarMatcher(SEP)), SearchedResult.of(SEP));
            Assertions.assertEquals(index + Byte.BYTES, r.readIndex());
        }
    }

    /**
     *   Test vector search algorithm
     */
    @Test
    public void testVectorSearch() {
        for(int index = 0; index < LENGTH; index++) {
            ReadBuffer r = createSearchableReadBuffer(index, LENGTH, SEP);
            Assertions.assertEquals(r.vectorSearch(vector -> vector.eq(SEP)), SearchedResult.of(SEP));
            Assertions.assertEquals(index + Byte.BYTES, r.readIndex());
        }
    }
}
