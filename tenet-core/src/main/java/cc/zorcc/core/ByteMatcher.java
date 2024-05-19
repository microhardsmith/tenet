package cc.zorcc.core;

/**
 *   ByteMatcher is a matcher pattern for linearSearch in ReadBuffer
 */
@FunctionalInterface
public interface ByteMatcher {
    /**
     *   Return if current byte matches the condition
     */
    boolean match(byte b);
}
