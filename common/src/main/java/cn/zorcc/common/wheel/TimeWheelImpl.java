package cn.zorcc.common.wheel;

import cn.zorcc.common.Clock;
import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于时间轮算法实现的定时任务调度器
 * 调度器将定时任务划分为两种,一种是时间期限较近的、需要频繁执行的任务,一种是时间期限较远,执行频率低的任务
 * 时间线较近的任务会被直接添加至时间轮中进行调度,时间线较远的任务会在等待队列中先等待,直到期限接近时才会被加入至时间轮
 */
@Slf4j
public class TimeWheelImpl implements TimeWheel {
    /**
     *  虚拟线程名前缀
     */
    private static final String prefix = "Job-";
    /**
     *  虚拟线程计数器
     */
    private static final AtomicLong counter = new AtomicLong(0L);
    /**
     *  单例标识
     */
    private static final AtomicBoolean instanceFlag = new AtomicBoolean(false);
    /**
     * 长任务与短任务的分隔界限
     */
    private final long boundary;
    /**
     * 时间轮槽位数
     */
    private final int slots;
    /**
     * 掩码
     */
    private final int mask;
    /**
     * 时间轮最小刻度,单位毫秒
     */
    private final long tick;
    /**
     * 时间轮数组,数组存放任务双向链表的头结点,头结点不包含数据
     */
    private final TimerJob[] timeWheel;
    /**
     * 时间轮每个槽位对应锁
     */
    private final Lock[] timeWheelLocks;
    /**
     * 长时间任务等待队列,执行期限超过1分钟的任务会先进入队列等待
     */
    private final TreeSet<TimerJob> waitingTreeSet = new TreeSet<>(TimerJob::compareTo);
    /**
     * 等待队列互斥锁
     */
    private final Lock waitingTreeSetLock = new ReentrantLock();
    /**
     * 时间轮运行信息
     */
    private final AtomicReference<TimerStatus> status = new AtomicReference<>(TimerStatus.DEFAULT);
    private Thread coreThread = null;
    private Thread waitThread = null;

    /**
     * 时间刻度记录
     * @param lastNano 上一个应进入运行槽位的纳秒时间
     * @param lastSlot 上一个运行槽位
     */
    record TimerStatus(long lastNano, int lastSlot) {
        public static TimerStatus DEFAULT = new TimerStatus(-1L, -1);
    }

    public TimeWheelImpl(int slots, long tick, long boundary) {
        if(!instanceFlag.compareAndSet(false, true)) {
            throw new FrameworkException(ExceptionType.CONTEXT, "TimeWheel could only have a single instance");
        }
        int mask = slots - 1;
        if(slots < 2 || (slots & slots - 1) != 0) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Slots must be power of 2");
        }
        this.slots = slots;
        this.mask = mask;
        this.tick = tick;
        this.boundary = boundary;
        this.timeWheel = new TimerJob[slots];
        this.timeWheelLocks = new Lock[slots];
        for (int i = 0; i < slots; i++) {
            timeWheel[i] = new TimerJob(); // empty head
            timeWheelLocks[i] = new ReentrantLock();
        }
    }

    @Override
    public void start() {
        if (!status.compareAndSet(TimerStatus.DEFAULT, new TimerStatus(Clock.nano(), -1))) {
            throw new FrameworkException(ExceptionType.CONTEXT, "TimeWheel already started");
        }
        this.coreThread = ThreadUtil.virtual(Constants.WHEEL_CORE, this::inspectingTimeWheel);
        this.waitThread = ThreadUtil.virtual(Constants.WHEEL_WAIT, this::inspectingWaitingTreeSet);
        waitThread.start();
        coreThread.start();
    }

    @Override
    public void shutdown() {
        coreThread.interrupt();
        waitThread.interrupt();
        status.set(TimerStatus.DEFAULT);
    }

    @SuppressWarnings({"BusyWait"})
    private void inspectingTimeWheel() {
        final Thread currentThread = this.coreThread;
        if(currentThread == null) {
            throw new FrameworkException(ExceptionType.CONTEXT, "TimeWheel is not initialized");
        }
        while (!currentThread.isInterrupted()) {
            TimerStatus lastTimeStatus = status.get();
            int slot = (lastTimeStatus.lastSlot() + 1) & mask;
            long nano = lastTimeStatus.lastNano() + tick;
            status.set(new TimerStatus(nano, slot));
            TimerJob head = timeWheel[slot];
            Lock lock = timeWheelLocks[slot];
            lock.lock();
            try {
                TimerJob current = head.next();
                while (current != null) {
                    if (current.isCancel()) {
                        // 已经被取消的任务,可直接移除
                        removeNode(current);
                    } else {
                        int rounds = current.rounds();
                        if (rounds > 0) {
                            current.setRounds(rounds - 1);
                        } else {
                            removeNode(current);
                            // 如果是循环任务,重新添加到时间轮中
                            long period = current.period();
                            if (period != -1) {
                                long executionTime = current.executionTime();
                                long due = current.resetTime() + period;
                                if(due > executionTime) {
                                    current.setExecutionTime(due);
                                    add(due - executionTime, current);
                                }else {
                                    execute(current);
                                    current.setExecutionTime(executionTime + period);
                                    add(period, current);
                                }
                            } else {
                                execute(current);
                            }
                        }
                    }
                    current = current.next();
                }
            } finally {
                lock.unlock();
            }
            long sleepTime = tick - Clock.elapsed(nano);
            if (sleepTime > 0) {
                try{
                    Thread.sleep(sleepTime);
                }catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void execute(TimerJob timerJob) {
        ThreadUtil.virtual(prefix + counter.getAndIncrement(), timerJob.job()).start();
    }

    @SuppressWarnings({"BusyWait"})
    private void inspectingWaitingTreeSet() {
        final Thread currentThread = this.coreThread;
        if(currentThread == null) {
            throw new FrameworkException(ExceptionType.CONTEXT, "TimeWheel is not initialized");
        }
        while (!currentThread.isInterrupted()) {
            long current = Clock.current();
            Iterator<TimerJob> iterator = waitingTreeSet.iterator();
            waitingTreeSetLock.lock();
            try {
                while (iterator.hasNext()) {
                    TimerJob timerJob = iterator.next();
                    long delay = timerJob.executionTime() - current;
                    if (delay < boundary) {
                        iterator.remove();
                        long ticks = delay / tick;
                        timerJob.setPos((int) ticks % slots);
                        timerJob.setRounds((int) ticks / slots);
                        insertTimerJob(timerJob);
                    } else {
                        break;
                    }
                }
            } finally {
                waitingTreeSetLock.unlock();
            }
            try{
                Thread.sleep(boundary << 1);
            }catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * 将任务插入到时间轮中,针对每个槽位的双向链表使用头插法
     */
    private void insertTimerJob(TimerJob timerJob) {
        int pos = timerJob.pos();
        Lock lock = timeWheelLocks[pos];
        lock.lock();
        try {
            TimerJob head = timeWheel[pos];
            addNode(head, timerJob);
        } finally {
            lock.unlock();
        }
    }

    /**
     *  使用头插法添加节点
     */
    private void addNode(TimerJob head, TimerJob target) {
        TimerJob next = head.next();
        head.setNext(target);
        target.setPrev(head);
        if(next != null) {
            next.setPrev(target);
            target.setNext(next);
        }
    }

    /**
     *  从链表中移除节点
     */
    private void removeNode(TimerJob target) {
        TimerJob prev = target.prev();
        TimerJob next = target.next();
        prev.setNext(next);
        if (next != null) {
            next.setPrev(prev);
        }
    }

    /**
     * 将任务添加到等待队列中
     */
    private TimerJob toWaitingSet(TimerJob timerJob) {
        waitingTreeSetLock.lock();
        try {
            waitingTreeSet.add(timerJob);
            return timerJob;
        } finally {
            waitingTreeSetLock.unlock();
        }
    }

    /**
     * 将任务添加到时间轮,如果任务需要立刻执行则将其下一个周期添加至时间轮
     */
    private TimerJob toTimeWheel(TimerJob timerJob) {
        TimerStatus timerStatus = status.get();
        long executionTime = timerJob.executionTime();
        long ticks = (executionTime - timerStatus.lastNano()) / tick;
        if(ticks <= 1L) {
            execute(timerJob);
            long period = timerJob.period();
            if(period != -1L) {
                timerJob.setExecutionTime(executionTime + period);
                ticks += period / tick;
            }else {
                return timerJob;
            }
        }
        timerJob.setPos((int) ((ticks + timerStatus.lastSlot()) & mask));
        timerJob.setRounds((int) ticks / slots);
        insertTimerJob(timerJob);
        return timerJob;
    }

    @Override
    public TimerJob addJob(Runnable job, long delay, TimeUnit timeUnit) {
        long delayMillis = timeUnit.toMillis(delay);
        TimerJob timerJob = new TimerJob(Clock.current() + delayMillis, -1, job);
        return add(delayMillis, timerJob);
    }

    @Override
    public TimerJob addPeriodicJob(Runnable job, long delay, long periodDelay, TimeUnit timeUnit) {
        long delayMillis = timeUnit.toMillis(delay);
        long periodDelayMillis = timeUnit.toMillis(periodDelay);
        if (periodDelayMillis < tick) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Timer tick is too large");
        }
        TimerJob timerJob = new TimerJob(Clock.current() + delayMillis, periodDelayMillis, job);
        return add(delayMillis, timerJob);
    }

    private TimerJob add(long delayMillis, TimerJob timerJob) {
        return delayMillis > boundary ? toWaitingSet(timerJob) : toTimeWheel(timerJob);
    }
}
