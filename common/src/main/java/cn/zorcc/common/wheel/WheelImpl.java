package cn.zorcc.common.wheel;

import cn.zorcc.common.Clock;
import cn.zorcc.common.Constants;
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
    private static final AtomicLong counter = new AtomicLong(Constants.ZERO);
    private static final AtomicBoolean instanceFlag = new AtomicBoolean(false);
    private static final AtomicBoolean startFlag = new AtomicBoolean(false);
    private final int mask;
    private final long tick;
    private final long tickNano;
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
        this.tickNano = TimeUnit.MILLISECONDS.toNanos(tick);
        this.bound = slots * tick;
        this.cMask = mask >> 1;
        this.queue = new MpscUnboundedAtomicArrayQueue<>(slots);
        this.wheel = new JobImpl[slots];
        for(int i = 0; i < slots; i++) {
            // create head node
            wheel[i] = new JobImpl(Long.MIN_VALUE, Long.MIN_VALUE, null);
        }
        // if we use virtual thread, then parkNanos() will internally use a ScheduledThreadPoolExecutor for unpark the current vthread
        // still there is a platform thread constantly waiting for lock and go to sleep and so on. So use platform thread would be more simplified
        this.wheelThread = ThreadUtil.platform("Wheel", this::run);
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
    public void shutdown() {
        wheelThread.interrupt();
    }

    @Override
    public Job addJob(Runnable job, long delay, TimeUnit timeUnit) {
        long delayMillis = timeUnit.toMillis(delay);
        JobImpl result = new JobImpl(Clock.current() + delayMillis, JobImpl.ONCE, job);
        if (!queue.offer(result)) {
            throw new FrameworkException(ExceptionType.WHEEL, Constants.UNREACHED);
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
            throw new FrameworkException(ExceptionType.WHEEL, Constants.UNREACHED);
        }
        return result;
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
                final JobImpl job = queue.poll();
                if(job == null) {
                    break;
                }
                // if delay is smaller than current milli, we should run it in current slot, so we select tasks before running wheel
                final long delayMillis = Math.max(job.execMilli - milli, 0L);
                if(delayMillis >= bound) {
                    waitSet.add(job);
                }else {
                    job.pos = (int) ((slot + (delayMillis / tick)) & mask);
                    insert(job);
                }
            }

            // check if we need to scan the waiting queue
            if((slot & cMask) == 0) {
                Iterator<JobImpl> iter = waitSet.iterator();
                while (iter.hasNext()) {
                    JobImpl job = iter.next();
                    final long delayMillis = job.execMilli - milli;
                    if(delayMillis < bound) {
                        iter.remove();
                        job.pos = (int) ((slot + (delayMillis / tick)) & mask);
                        insert(job);
                    }else {
                        // if current task is not for scheduling, then the following tasks won't be available
                        break;
                    }
                }
            }
            
            // processing wheel's current mission
            final JobImpl head = wheel[slot];
            JobImpl current = head.next;
            while (current != null) {
                JobImpl next = remove(current);
                // the executor thread and the canceller thread will fight for ownership, but only one would succeed
                if(current.owner.compareAndSet(false, true)) {
                    ThreadUtil.virtual("wheel-job-" + counter.getAndIncrement(), current.mission).start();
                    final long period = current.period;
                    if(period != JobImpl.ONCE) {
                        // reset current node's status
                        current.owner.set(false);
                        current.execMilli = current.execMilli + period;
                        current.prev = null;
                        current.next = null;
                        if(period >= bound) {
                            waitSet.add(current);
                        }else {
                            current.pos = (int) ((slot + (period / tick)) & mask);
                            insert(current);
                        }
                    } else {
                        // help GC
                        current.prev = null;
                        current.next = null;
                    }
                }
                current = next;
            }
            
            // park until next slot
            LockSupport.parkNanos(scale.nano - Clock.nano());
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
     *   remove the target node from current linked-list, return next node
     */
    private JobImpl remove(final JobImpl target) {
        JobImpl prev = target.prev;
        JobImpl next = target.next;
        prev.next = next;
        if(next != null) {
            next.prev = prev;
        }
        return next;
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
            nano = nano + tickNano;
            milli = milli + tick;
            slot = (slot + 1) & mask;
        }
    }

    private static final class JobImpl implements Job, Comparable<JobImpl> {
        public static final long ONCE = -1L;
        private long execMilli;
        private int pos;
        private JobImpl prev;
        private JobImpl next;
        private final Runnable mission;
        private final AtomicBoolean owner = new AtomicBoolean(false);
        private final AtomicLong count = new AtomicLong(0L);
        private final long period;

        JobImpl(long execMilli, long period, Runnable runnable) {
            this.execMilli = execMilli;
            this.period = period;
            this.mission = () -> {
                // there could be self calibration inside job itself if the tasks in your scenario, since LockSupport.parkNanos depends on a ScheduledExecutorService,
                // using calibration would make us fallback to JDK's DelayQueue, without calibration, the error range within tick should already be acceptable for most scenarios

                // LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(execMilli - Clock.current()));
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
            return owner.compareAndSet(false, true);
        }

        @Override
        public int compareTo(JobImpl o) {
            return Long.compare(execMilli, o.execMilli);
        }
    }
}
