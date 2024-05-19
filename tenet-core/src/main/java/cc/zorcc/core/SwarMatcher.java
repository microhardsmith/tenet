package cc.zorcc.core;

/**
 *   SwarMatcher is used for swarSearching in ReadBuffer
 */
public final class SwarMatcher {
    private final byte target;
    private final long pattern;

    public SwarMatcher(byte target) {
        this.target = target;
        this.pattern = compilePattern(target);
    }

    /**
     *   Creating a byte search pattern with SIMD inside a register algorithm, in short, SWAR search algorithm
     */
    private static long compilePattern(byte b) {
        long pattern = b & 0xFFL;
        return pattern
                | (pattern << 8)
                | (pattern << 16)
                | (pattern << 24)
                | (pattern << 32)
                | (pattern << 40)
                | (pattern << 48)
                | (pattern << 56);
    }

    public byte getTarget() {
        return target;
    }

    public long getPattern() {
        return pattern;
    }
}
