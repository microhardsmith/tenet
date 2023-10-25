package cn.zorcc.common;

import cn.zorcc.common.util.ThreadUtil;

import java.util.concurrent.atomic.AtomicReference;

/**
 *  Representing a carrier task executing by a virtual thread
 */
public record Carrier(
        Thread thread,
        AtomicReference<Object> target
) {
    public static final Object HOLDER = new Object();

    public static Carrier create() {
        return new Carrier(ThreadUtil.checkVirtualThread(), new AtomicReference<>(HOLDER));
    }
}
