package cn.zorcc.common.network;

import java.util.concurrent.TimeUnit;

public record Shutdown(
        long timeout,
        TimeUnit timeUnit
) {
    /**
     *   Default close operation would be invoked after shutdown for 5 seconds
     */
    public static final Shutdown DEFAULT = new Shutdown(5, TimeUnit.SECONDS);
}
