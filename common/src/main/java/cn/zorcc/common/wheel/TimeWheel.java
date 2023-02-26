package cn.zorcc.common.wheel;

import java.util.concurrent.TimeUnit;

/**
 * 时间轮任务调度器接口
 */
public interface TimeWheel {
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
