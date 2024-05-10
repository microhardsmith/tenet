package cn.zorcc.common.util;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class VectorTest {
    private static final VectorSpecies<Byte> species = ByteVector.SPECIES_PREFERRED;
    private static final byte PADDING = (byte) '0';
    private static final byte TARGET = (byte) '1';

    public static byte[] createArray(int len) {
        byte[] bytes = new byte[len];
        Arrays.fill(bytes, PADDING);
        bytes[len - 1] = TARGET;
        return bytes;
    }

    public static int normalSearch(byte[] bytes, int offset) {
        for(int i = offset; i < bytes.length; i++) {
            if(bytes[i] == TARGET) {
                return i;
            }
        }
        return -1;
    }

    public static int vectorSearch(byte[] bytes, int offset) {
        for(int start = offset; start < bytes.length; start += species.length()) {
            VectorMask<Byte> mask = species.indexInRange(start, bytes.length);
            ByteVector vector = ByteVector.fromArray(species, bytes, start, mask);
            VectorMask<Byte> eq = vector.compare(VectorOperators.EQ, TARGET);
            int searchIndex = eq.firstTrue();
            if(searchIndex < species.length()) {
                return start + searchIndex;
            }
        }
        return -1;
    }

    @Test
    public void testVectorSearchNonFind() {
        for(int i = 1; i < 4 * species.length(); i++) {
            byte[] arr = new byte[i];
            Arrays.fill(arr, PADDING);
            for(int j = 0; j < i; j++) {
                Assertions.assertEquals(vectorSearch(arr, j), -1);
            }
        }
    }

    @Test
    public void testVectorSearchFind() {
        for(int i = 1; i < 4 * species.length(); i++) {
            byte[] arr = createArray(i);
            for(int j = 0; j < i; j++) {
                Assertions.assertEquals(vectorSearch(arr, j), i - 1);
            }
        }
    }

    @Test
    public void testNormalNonFind() {
        for(int i = 1; i < 4 * species.length(); i++) {
            byte[] arr = new byte[i];
            Arrays.fill(arr, PADDING);
            for(int j = 0; j < i; j++) {
                Assertions.assertEquals(normalSearch(arr, j), -1);
            }
        }
    }

    @Test
    public void testNormalSearch() {
        for(int i = 1; i < 4 * species.length(); i++) {
            byte[] arr = createArray(i);
            for(int j = 0; j < i; j++) {
                Assertions.assertEquals(normalSearch(arr, j), i - 1);
            }
        }
    }
}
