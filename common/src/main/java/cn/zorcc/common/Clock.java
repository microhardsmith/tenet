package cn.zorcc.common;

/**
 * 系统时钟类
 */
public class Clock {

    private Clock() {
        throw new UnsupportedOperationException();
    }

    /**
     * 获取当前毫秒时间
     * @return 当前时间戳
     */
    public static long current() {
        return System.currentTimeMillis();
    }

    /**
     *  获取当前系统纳秒基准时间
     * @return 当前jvm时间
     */
    public static long nano() {
        return System.nanoTime();
    }

    /**
     *  计算时间间隔,单位纳秒
     * @return 自nano开始经过的纳秒时间
     */
    public static long elapsed(long nano) {
        return nano() - nano;
    }
}
