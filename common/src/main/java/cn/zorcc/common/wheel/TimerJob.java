package cn.zorcc.common.wheel;

import cn.zorcc.common.Clock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public final class TimerJob implements Comparable<TimerJob> {
    /**
     * 任务指定执行时间
     */
    private long executionTime;
    /**
     * 剩余循环轮数
     */
    private int rounds = -1;
    /**
     * 时间轮槽位
     */
    private int pos = -1;
    /**
     * 执行次数,每次任务执行后都会递增
     */
    private int count;
    /**
     * 循环周期,单位毫秒,如果不参与循环则为-1
     */
    private final long period;
    /**
     * 是否已经取消,单次执行的任务在执行之后会被置为取消状态
     */
    private final AtomicBoolean cancel = new AtomicBoolean(false);
    /**
     * 任务体
     */
    private final Runnable job;
    /**
     * 双向链表之后的任务
     */
    private TimerJob next;
    /**
     * 双向链表之前的任务
     */
    private TimerJob prev;
    /**
     * 是否重置定时任务,如果已重置,则将任务的下一个执行时间点置为resetTime + period
     * 需要保证多线程可见性
     */
    private final AtomicLong resetTime = new AtomicLong(-1L);

    /**
     * 用于构建头结点,不存储具体任务
     */
    public TimerJob() {
        this.executionTime = -1L;
        this.count = 0;
        this.period = -1L;
        this.job = null;
    }

    /**
     * 用于构建任务结点
     */
    public TimerJob(long executionTime, long period, Runnable runnable) {
        this.executionTime = executionTime;
        this.period = period;
        this.count = 0;
        this.job = () -> {
            // self calibration
            long sleepTime = executionTime - Clock.current();
            if (sleepTime > 0) {
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(sleepTime));
            }
            runnable.run();
            count = count + 1;
        };
    }

    /**
     *  更新重置时间为当前时间
     */
    public void reset() {
        reset(Clock.current());
    }

    public void reset(long timestamp) {
        resetTime.updateAndGet(t -> Math.max(timestamp, t));
    }

    /**
     *  返回当前任务已执行次数
     */
    public int count() {
        return count;
    }

    /**
     *  取消任务
     */
    public boolean cancel() {
        return cancel.compareAndSet(false, true);
    }

    public long resetTime() {
        return resetTime.get();
    }

    public long executionTime() {
        return executionTime;
    }

    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }

    public int pos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public int rounds() {
        return rounds;
    }

    public void setRounds(int rounds) {
        this.rounds = rounds;
    }

    public Runnable job() {
        return job;
    }

    public long period() {
        return period;
    }

    public TimerJob next() {
        return next;
    }

    public TimerJob prev() {
        return prev;
    }

    public void setNext(TimerJob next) {
        this.next = next;
    }

    public void setPrev(TimerJob prev) {
        this.prev = prev;
    }

    public boolean isCancel() {
        return cancel.get();
    }

    @Override
    public int compareTo(TimerJob job) {
        return Long.compare(executionTime, job.executionTime());
    }
}
