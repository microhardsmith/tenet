package cn.zorcc.common.structure;

import cn.zorcc.common.LifeCycle;

import java.time.Duration;

public sealed interface Wheel extends LifeCycle permits WheelImpl {
    int slots = Integer.getInteger("wheel.slots", 4096);
    long tick = Long.getLong("wheel.tick", 10L);

    static Wheel wheel() {
        return WheelImpl.INSTANCE;
    }

    Runnable addJob(Runnable mission, Duration delay);

    Runnable addPeriodicJob(Runnable mission, Duration delay, Duration period);
}
