package cc.zorcc.tenet.core;

/**
 *   SwarMatcher is used for swarSearching in ReadBuffer
 *   SwarMatcher has its own limitations, it could only be used in searching exactly the target byte, without complicated matching mechanism
 */
public record SwarMatcher(
        byte target,
        long pattern
) {
    /**
     *   Explicit check pattern matching target byte
     */
    public SwarMatcher {
        if(pattern != compilePattern(target)) {
            throw new TenetException(ExceptionType.MISUSE, "Pattern mismatch");
        }
    }

    /**
     *   Use this one instead of the default constructor
     *   here we would like to use record types just for better constant folding, because records are trusted by the VM
     */
    public SwarMatcher(byte target) {
        this(target, compilePattern(target));
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
}
