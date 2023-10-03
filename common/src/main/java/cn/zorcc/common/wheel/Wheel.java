package cn.zorcc.common.wheel;

import cn.zorcc.common.LifeCycle;

import java.time.Duration;
import java.util.function.LongConsumer;

public sealed interface Wheel extends LifeCycle permits WheelImpl {
    /**
     * 时间轮槽位,更大的时间轮会使每个槽位的任务链表的长度更短,提高执行效率,但也会消耗更多内存
     */
    int slots = 4096;
    /**
     * 时间轮tick大小,单位毫秒,默认精度为10ms,tick也是任务执行时间的精度
     * 如果启用了集群,时间轮精度需要满足raft算法的需求,单机情况下可适当放大该值以减少cpu占用率
     */
    long tick = 10L;

    static Wheel wheel() {
        return WheelImpl.instance;
    }

    Job addJob(Runnable job, Duration delay);

    Job addPeriodicJob(Runnable job, Duration delay, Duration period);

    Job addJob(LongConsumer job, Duration delay);

    Job addPeriodicJob(LongConsumer job, Duration delay, Duration period);
}
