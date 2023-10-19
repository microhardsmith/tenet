package cn.zorcc.common;

import cn.zorcc.common.util.ThreadUtil;

import java.util.concurrent.atomic.AtomicReference;

public abstract class Carrier {
    public static final Object HOLDER = new Object();
    private final Thread thread;
    private final AtomicReference<Object> target;

    protected Carrier() {
        this.thread = ThreadUtil.checkVirtualThread();
        this.target = new AtomicReference<>(HOLDER);
    }

    public Thread getThread() {
        return thread;
    }

    public AtomicReference<Object> getTarget() {
        return target;
    }
}
