package cn.zorcc.common.wheel;

import cn.zorcc.common.LifeCycle;

import java.util.concurrent.TimeUnit;

public sealed interface Wheel extends LifeCycle permits WheelImpl {
    /**
     * 时间轮槽位,更大的时间轮会使每个槽位的任务链表的长度更短,提高执行效率,但也会消耗更多内存
     */
    int slots = 1024;
    /**
     * 时间轮tick大小,单位毫秒,默认精度为25ms
     * 如果启用了集群,时间轮精度需要满足raft算法的需求,单机情况下可适当放大该值以减少cpu占用率
     */
    long tick = 25L;

    static Wheel wheel() {
        return WheelImpl.instance;
    }

    /**
     * 添加定时任务
     */
    Job addJob(Runnable job, long delay, TimeUnit timeUnit);

    /**
     * 添加周期性定时任务
     */
    Job addPeriodicJob(Runnable job, long delay, long periodDelay, TimeUnit timeUnit);
}
