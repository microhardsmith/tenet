package cn.zorcc.common.wheel;

import java.util.concurrent.TimeUnit;

/**
 * 时间轮任务调度器接口,实现为单例
 * 时间轮的精度仍然依赖于操作系统，目前参数是固定的，可满足绝大部分需求，如果用户需要可以手动调整slot tick和boundary的取值
 */
public sealed interface TimeWheel permits TimeWheelImpl {
    /**
     * 时间轮槽位,更大的时间轮会使每个槽位的任务链表的长度更短,提高执行效率,但也会消耗更多内存
     */
    int slots = 1024;
    /**
     * 时间轮tick大小,单位毫秒,默认精度为25ms
     * 如果启用了集群,时间轮精度需要满足raft算法的需求,单机情况下可适当放大该值以减少cpu占用率
     */
    long tick = 25L;
    /**
     * 时间轮界限值,超过该值的任务会被添加至等待队列,单位毫秒
     */
    long boundary = 30000L;

    static TimeWheel instance() {
        return TimeWheelImpl.instance;
    }

    /**
     * 启动时间轮
     */
    void start();

    /**
     * 添加定时任务
     */
    TimerJob addJob(Runnable job, long delay, TimeUnit timeUnit);

    /**
     * 添加周期性定时任务
     */
    TimerJob addPeriodicJob(Runnable job, long delay, long periodDelay, TimeUnit timeUnit);

    /**
     *  关闭时间轮，丢弃当前所有未执行的任务
     */
    void shutdown();
}
