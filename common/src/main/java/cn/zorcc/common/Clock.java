package cn.zorcc.common;

/**
 *   Helper clock class
 *   Currently we would just use the JDK implementation for time retrieving, it might be modified in the future
 */
public final class Clock {

    /**
     *   Never initialize it
     */
    private Clock() {
        throw new UnsupportedOperationException();
    }

    /**
     *  Get current milliseconds timestamp
     */
    public static long current() {
        return System.currentTimeMillis();
    }

    /**
     *  Get current nanoseconds timestamp
     */
    public static long nano() {
        return System.nanoTime();
    }

    /**
     *  Calculates the nanoseconds elapsed since the specified nanosecond timestamp
     */
    public static long elapsed(long nano) {
        return nano() - nano;
    }
}
