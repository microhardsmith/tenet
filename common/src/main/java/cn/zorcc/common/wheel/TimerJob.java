package cn.zorcc.common.wheel;

import cn.zorcc.common.Clock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public final class TimerJob implements Comparable<TimerJob> {
    /**
     * 任务指定执行毫秒时间戳
     */
    private final AtomicLong execMilli = new AtomicLong();
    /**
     * 剩余循环轮数
     */
    private final AtomicInteger rounds = new AtomicInteger();
    /**
     * 时间轮槽位
     */
    private final AtomicInteger pos = new AtomicInteger();
    /**
     * 执行次数,每次任务执行后都会递增
     */
    private final AtomicInteger count = new AtomicInteger(0);
    /**
     * 循环周期,单位毫秒,如果不参与循环则为-1
     */
    private final long period;
    /**
     * 是否已经取消,单次执行的任务在执行之后会被置为取消状态
     */
    private final AtomicBoolean cancel = new AtomicBoolean(false);
    /**
     * 定时任务体
     */
    private final Runnable job;
    /**
     * 双向链表之后的任务
     */
    private TimerJob next = null;
    /**
     * 双向链表之前的任务
     */
    private TimerJob prev = null;

    /**
     * 用于构建头结点,不存储具体任务
     */
    private TimerJob() {
        this.period = Long.MIN_VALUE;
        this.job = null;
    }
    public static final TimerJob HEAD = new TimerJob();
    public static final long ONCE = -1L;

    /**
     * 用于构建任务结点
     */
    public TimerJob(long execMilli, long period, Runnable runnable) {
        this.execMilli.set(execMilli);
        this.period = period;
        this.job = () -> {
            // self calibration
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(execMilli - Clock.current()));
            runnable.run();
            count.incrementAndGet();
        };
    }

    /**
     *  返回当前任务已执行次数
     */
    public int count() {
        return count.get();
    }

    /**
     *  取消任务,返回是否已取消成功
     */
    public boolean cancel() {
        return cancel.compareAndSet(false, true);
    }

    public long execMilli() {
        return execMilli.get();
    }

    public void setExecMilli(long milli) {
        execMilli.set(milli);
    }

    public int pos() {
        return pos.get();
    }

    public void setPos(int pos) {
        this.pos.set(pos);
    }

    public int rounds() {
        return rounds.get();
    }

    public void setRounds(int rounds) {
        this.rounds.set(rounds);
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
        return Long.compare(execMilli.get(), job.execMilli());
    }
}
