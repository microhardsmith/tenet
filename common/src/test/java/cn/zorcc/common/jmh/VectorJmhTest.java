package cn.zorcc.common.jmh;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.RunnerException;

import java.util.concurrent.ThreadLocalRandom;

public class VectorJmhTest extends JmhTest {
    private static final VectorSpecies<Byte> species = ByteVector.SPECIES_PREFERRED;

    private static final byte UPPER_BOUND = (byte) '2';
    private static final byte LOWER_BOUND = (byte) '0';
    @Param({"1", "20", "100", "1000"})
    private int BATCH_SIZE;
    private byte[] bytes;

    @FunctionalInterface
    interface ToMask {
        VectorMask<Byte> mask(ByteVector vector);
    }

    private static final ToMask t = vector -> {
        VectorMask<Byte> eq = vector.compare(VectorOperators.GT, LOWER_BOUND);
        VectorMask<Byte> ne = vector.compare(VectorOperators.LT, UPPER_BOUND);
        return eq.and(ne);
    };

    @Setup(value = Level.Iteration)
    public void setup() {
        int fixed = BATCH_SIZE * species.length();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int padding = species.length() >> 1;
        bytes = new byte[fixed + padding];
        for(int i = 0; i < fixed; i++) {
            byte b;
            do {
                b = (byte) r.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE);
            } while (b < UPPER_BOUND && b > LOWER_BOUND);
            bytes[i] = b;
        }
        for(int i = fixed; i < fixed + padding; i++) {
            bytes[i] = (byte) r.nextInt(LOWER_BOUND, UPPER_BOUND);
        }
    }

    @CompilerControl(CompilerControl.Mode.INLINE)
    public static int normalSearch(byte[] bytes, int offset) {
        for(int i = offset; i < bytes.length; i++) {
            byte b = bytes[i];
            if(b > LOWER_BOUND && b < UPPER_BOUND) {
                return i;
            }
        }
        return -1;
    }

    @CompilerControl(CompilerControl.Mode.INLINE)
    public static int vectorSearch(byte[] bytes, int offset) {
        for(int start = offset; start < bytes.length; start += species.length()) {
            VectorMask<Byte> mask = species.indexInRange(start, bytes.length);
            ByteVector vector = ByteVector.fromArray(species, bytes, start, mask);
            int searchIndex = t.mask(vector).firstTrue();
            if(searchIndex < species.length()) {
                return start + searchIndex;
            }
        }
        return -1;
    }

    @Benchmark
    public void testNormal(Blackhole bh) {
        bh.consume(normalSearch(bytes, 0));
    }

    @Benchmark
    public void testVector(Blackhole bh) {
        bh.consume(vectorSearch(bytes, 0));
    }


    void main() throws RunnerException {
        runTest(VectorJmhTest.class);
    }
}
