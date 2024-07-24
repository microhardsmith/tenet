package cc.zorcc.tenet.core;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorMask;

/**
 *   VectorMatcher is a matcher pattern for vectorSearch in ReadBuffer
 */
@FunctionalInterface
public interface VectorMatcher {
    /**
     *  Constructing a VectorMask based on the given vector for filtering value
     */
    VectorMask<Byte> match(ByteVector vector);
}
