package cn.zorcc.common;


public final class Clock {

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
     *  Calculates the nanoseconds elapsed since the specified timestamp
     */
    public static long elapsed(long nano) {
        return nano() - nano;
    }
}
