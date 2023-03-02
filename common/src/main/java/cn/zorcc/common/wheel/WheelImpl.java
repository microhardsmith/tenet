package cn.zorcc.common.wheel;

import cn.zorcc.common.Clock;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ThreadUtil;
import org.jctools.queues.atomic.MpscUnboundedAtomicArrayQueue;

import java.util.Iterator;
import java.util.Queue;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public final class WheelImpl implements Wheel {
    private static final String NAME = "wheel";
    private static final AtomicBoolean instanceFlag = new AtomicBoolean(false);
    private static final AtomicBoolean startFlag = new AtomicBoolean(false);
    private final int mask;
    private final long tick;
    private final long bound;
    private final int cMask;
    private final Queue<JobImpl> queue;
    private final JobImpl[] wheel;
    private final TreeSet<JobImpl> waitSet = new TreeSet<>(JobImpl::compareTo);
    private final Thread wheelThread;
    private WheelImpl(int slots, long tick) {
        if(!instanceFlag.compareAndSet(false, true)) {
            throw new FrameworkException(ExceptionType.WHEEL, "Wheel could only have a single instance");
        }
        if(slots < 256) {
            throw new FrameworkException(ExceptionType.WHEEL, "Slots are too small for a wheel");
        }
        if((slots & (slots - 1)) != 0) {
            throw new FrameworkException(ExceptionType.WHEEL, "Slots must be power of two");
        }
        this.mask = slots - 1;
        this.tick = tick;
        this.bound = slots * tick;
        this.cMask = mask >> 1;
        this.queue = new MpscUnboundedAtomicArrayQueue<>(slots);
        this.wheel = new JobImpl[slots];
        for(int i = 0; i < slots; i++) {
            wheel[i] = JobImpl.HEAD;
        }
        this.wheelThread = ThreadUtil.virtual("Wheel", this::run);
    }
    public static final Wheel instance = new WheelImpl(Wheel.slots, Wheel.tick);

    @Override
    public void init() {
        if(!startFlag.compareAndSet(false, true)) {
            throw new FrameworkException(ExceptionType.WHEEL, "Wheel already started");
        }
        wheelThread.start();
    }

    @Override
    public Job addJob(Runnable job, long delay, TimeUnit timeUnit) {
        long delayMillis = timeUnit.toMillis(delay);
        JobImpl result = new JobImpl(Clock.current() + delayMillis, JobImpl.ONCE, job);
        if (!queue.offer(result)) {
            // should never reach
            throw new FrameworkException(ExceptionType.WHEEL, "Unable to offer");
        }
        return result;
    }

    @Override
    public Job addPeriodicJob(Runnable job, long delay, long periodDelay, TimeUnit timeUnit) {
        long periodDelayMillis = timeUnit.toMillis(periodDelay);
        if (periodDelayMillis <= tick) {
            throw new FrameworkException(ExceptionType.WHEEL, "Wheel tick is too large for current task, might consider tuning Wheel's default parameters");
        }
        long delayMillis = timeUnit.toMillis(delay);
        JobImpl result = new JobImpl(Clock.current() + delayMillis, periodDelayMillis, job);
        if (!queue.offer(result)) {
            // should never reach
            throw new FrameworkException(ExceptionType.WHEEL, "Unable to offer");
        }
        return result;
    }

    @Override
    public void shutdown() {
        wheelThread.interrupt();
    }

    /**
     *  运行时间轮
     */
    private void run() {
        final Scale scale = new Scale();
        final Thread currentThread = Thread.currentThread();
        while (!currentThread.isInterrupted()) {
            final long milli = scale.milli;
            final int slot = scale.slot;
            scale.update();

            // inspecting if there are tasks that should be added to the wheel
            for( ; ;) {
                JobImpl job = queue.poll();
                if(job == null) {
                    break;
                }
                // if delay is smaller than current milli, we will run it in current slot
                final long delayMillis = Math.max(job.execMilli - milli, 0L);
                if(delayMillis >= bound) {
                    waitSet.add(job);
                }else {
                    long ticks = delayMillis / tick;
                    job.pos = (int) ((slot + ticks) & mask);
                    insert(job);
                }
            }
            
            // processing wheel's current mission
            final JobImpl head = wheel[slot];
            JobImpl current = head.next;
            while (current != null) {
                remove(current);
                if(!current.cancel.get()) {
                    ThreadUtil.virtual(NAME, current.mission).start();
                    final long period = current.period;
                    if(period != JobImpl.ONCE) {
                        current.execMilli = current.execMilli + period;
                    }
                }
                current = current.next;
            }

            // check if we need to scan the waiting queue
            if((slot & cMask) == 0) {
                Iterator<JobImpl> iter = waitSet.iterator();
                while (iter.hasNext()) {
                    JobImpl job = iter.next();
                    long delay = job.execMilli - milli;
                    if(delay < bound) {
                        iter.remove();

                    }else {
                        break;
                    }
                }
            }
            
            // sleep until next slot
            final long sleepNano = scale.nano - Clock.nano();
            try{
                TimeUnit.NANOSECONDS.sleep(sleepNano);
            }catch (InterruptedException e) {
                currentThread.interrupt();
                break;
            }
        }
    }

    /**
     *   insert the target node after the head
     */
    private void insert(final JobImpl target) {
        final JobImpl head = wheel[target.pos];
        JobImpl next = head.next;
        head.next = target;
        target.prev = head;
        if(next != null) {
            next.prev = target;
            target.next = next;
        }
    }

    /**
     *   remove the target node from current linked-list
     */
    private void remove(final JobImpl target) {
        JobImpl prev = target.prev;
        JobImpl next = target.next;
        prev.next = next;
        if(next != null) {
            next.prev = prev;
        }
    }

    /**
     * 时间刻度记录,每次进入时间轮将更新当前Scale为下次运行状态
     */
    private final class Scale {
        /**
         *   应进入槽位的纳秒时间 用于计算时间间隔
         */
        private long nano;
        /**
         *   应进入槽位的毫秒时间戳 用于获取当前时间
         */
        private long milli;
        /**
         *   应运行槽位
         */
        private int slot;

        Scale() {
            this.nano = Clock.nano();
            this.milli = Clock.current();
            this.slot = 0;
        }

        /**
         *   更新当前时间槽状态为下一时刻
         */
        public void update() {
            nano = nano + TimeUnit.MILLISECONDS.toNanos(tick);
            milli = milli + tick;
            slot = (slot + 1) & mask;
        }
    }

    private static final class JobImpl implements Job, Comparable<JobImpl> {
        private static final JobImpl HEAD = new JobImpl(Long.MIN_VALUE, Long.MIN_VALUE, null);
        public static final long ONCE = -1L;
        private long execMilli;
        private int pos;
        private JobImpl prev;
        private JobImpl next;
        private final Runnable mission;
        private final AtomicBoolean cancel = new AtomicBoolean(false);
        private final AtomicLong count = new AtomicLong(0L);
        private final long period;

        JobImpl(long execMilli, long period, Runnable runnable) {
            this.execMilli = execMilli;
            this.period = period;
            this.mission = () -> {
                // self calibration
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(execMilli - Clock.current()));
                runnable.run();
                count.incrementAndGet();
            };
        }


        @Override
        public long count() {
            return count.get();
        }

        @Override
        public boolean cancel() {
            return cancel.compareAndSet(false, true);
        }

        @Override
        public int compareTo(JobImpl o) {
            return Long.compare(execMilli, o.execMilli);
        }
    }
}
