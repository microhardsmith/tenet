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
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于时间轮算法实现的定时任务调度器
 * 调度器将定时任务划分为两种,一种是时间期限较近的、需要频繁执行的任务,一种是时间期限较远,执行频率低的任务
 * 时间线较近的任务会被直接添加至时间轮中进行调度,时间线较远的任务会在等待队列中先等待,直到期限接近时才会被加入至时间轮
 */
@Slf4j
public final class TimeWheelImpl implements TimeWheel {
    /**
     *  virtual thread name
     */
    private static final String NAME = "Job";
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
     * 时间轮刻度
     */
    private final AtomicReference<Scale> scale = new AtomicReference<>(Scale.DOWN);
    /**
     *  时间轮线程
     */
    private Thread coreThread = null;
    /**
     *  等待队列线程
     */
    private Thread waitThread = null;

    /**
     * 时间刻度记录，每次进入时间轮将更新当前TimerStatus为下次运行状态
     * @param nano  应进入槽位的纳秒时间 用于计算时间间隔
     * @param milli 应进入槽位的毫秒时间戳 用于获取当前时间
     * @param slot  应运行槽位
     */
    private record Scale(long nano, long milli, int slot) {
        public static Scale DOWN = new Scale(Long.MIN_VALUE, Long.MIN_VALUE, Integer.MIN_VALUE);
    }

    private TimeWheelImpl(int slots, long tick, long boundary) {
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
            timeWheel[i] = TimerJob.HEAD; // empty head node
            timeWheelLocks[i] = new ReentrantLock();
        }
    }

    public static final TimeWheel instance = new TimeWheelImpl(TimeWheel.slots, TimeWheel.tick, TimeWheel.boundary);

    @Override
    public void start() {
        if (!scale.compareAndSet(Scale.DOWN, new Scale(Clock.nano(), Clock.current(), 0))) {
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
        scale.set(Scale.DOWN);
    }

    @SuppressWarnings({"BusyWait"})
    private void inspectingTimeWheel() {
        final Thread currentThread = Thread.currentThread();
        while (!currentThread.isInterrupted()) {
            final Scale currentScale = scale.get();
            final long nano = currentScale.nano();
            final long nextNano = nano + TimeUnit.MILLISECONDS.toNanos(tick);
            final long milli = currentScale.milli();
            final long nextMilli = milli + tick;
            final int slot = currentScale.slot();
            final int nextSlot = (slot + 1) & mask;
            Scale nextScale = new Scale(nextNano, nextMilli, nextSlot);
            if(!scale.compareAndSet(currentScale, nextScale)) {
                throw new FrameworkException(ExceptionType.CONTEXT, "TimeWheel corrupted");
            }
            final TimerJob head = timeWheel[slot];
            final Lock lock = timeWheelLocks[slot];
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
                            execute(current);
                            long period = current.period();
                            if (period != TimerJob.ONCE) {
                                // period job, re-add to wheel
                                current.setExecMilli(current.execMilli() + period);
                                add(period, current);
                            }
                        }
                    }
                    current = current.next();
                }
            } finally {
                lock.unlock();
            }
            final long sleepNano = nextNano - Clock.nano();
            System.out.println(sleepNano);
            try{
                TimeUnit.NANOSECONDS.sleep(sleepNano);
            }catch (InterruptedException e) {
                currentThread.interrupt();
                break;
            }
        }
    }

    private void execute(TimerJob timerJob) {
        ThreadUtil.virtual(NAME, timerJob.job()).start();
    }

    @SuppressWarnings({"BusyWait"})
    private void inspectingWaitingTreeSet() {
        final Thread currentThread = Thread.currentThread();
        while (!currentThread.isInterrupted()) {
            long currentMilli = Clock.current();
            Iterator<TimerJob> iterator = waitingTreeSet.iterator();
            waitingTreeSetLock.lock();
            try {
                while (iterator.hasNext()) {
                    TimerJob timerJob = iterator.next();
                    long delay = timerJob.execMilli() - currentMilli;
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
    private void addNode(final TimerJob head, final TimerJob target) {
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
    private void removeNode(final TimerJob target) {
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
    private TimerJob toWaitingSet(final TimerJob timerJob) {
        waitingTreeSetLock.lock();
        try {
            waitingTreeSet.add(timerJob);
            return timerJob;
        } finally {
            waitingTreeSetLock.unlock();
        }
    }

    /**
     * 将任务添加到时间轮,如果任务需要立刻执行且为周期任务则将其下一个周期添加至时间轮
     */
    private TimerJob toTimeWheel(TimerJob timerJob) {
        final Scale currentScale = scale.get();
        long execMilli = timerJob.execMilli();
        // round down
        long ticks = (execMilli - currentScale.milli()) / tick;
        if(ticks <= 1L) {
            // too close to add to the wheel
            execute(timerJob);
            long period = timerJob.period();
            if(period != TimerJob.ONCE) {
                timerJob.setExecMilli(execMilli + period);
                ticks += period / tick;
            }else {
                return timerJob;
            }
        }
        timerJob.setPos((int) ((ticks + currentScale.slot()) & mask));
        timerJob.setRounds((int) ticks / slots);
        insertTimerJob(timerJob);
        return timerJob;
    }

    @Override
    public TimerJob addJob(Runnable job, long delay, TimeUnit timeUnit) {
        long delayMillis = timeUnit.toMillis(delay);
        TimerJob timerJob = new TimerJob(Clock.current() + delayMillis, TimerJob.ONCE, job);
        return add(delayMillis, timerJob);
    }

    @Override
    public TimerJob addPeriodicJob(Runnable job, long delay, long periodDelay, TimeUnit timeUnit) {
        long delayMillis = timeUnit.toMillis(delay);
        long periodDelayMillis = timeUnit.toMillis(periodDelay);
        if (periodDelayMillis < tick) {
            throw new FrameworkException(ExceptionType.CONTEXT, "TimeWheel tick is too large for current task, might consider tuning TimeWheel's default parameters");
        }
        TimerJob timerJob = new TimerJob(Clock.current() + delayMillis, periodDelayMillis, job);
        return add(delayMillis, timerJob);
    }

    /**
     *  根据等待时间长短选择添加至等待队列或时间轮中
     */
    private TimerJob add(long delayMillis, TimerJob timerJob) {
        return delayMillis >= boundary ? toWaitingSet(timerJob) : toTimeWheel(timerJob);
    }
}
