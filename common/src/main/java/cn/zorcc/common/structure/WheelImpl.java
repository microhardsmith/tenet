package cn.zorcc.common.structure;

import cn.zorcc.common.AbstractLifeCycle;
import cn.zorcc.common.Clock;
import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import org.jctools.queues.atomic.MpscUnboundedAtomicArrayQueue;

import java.time.Duration;
import java.util.Iterator;
import java.util.Queue;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.LongConsumer;

public final class WheelImpl extends AbstractLifeCycle implements Wheel {
    private static final long ONCE = Long.MIN_VALUE;
    private static final AtomicLong counter = new AtomicLong(0);
    private static final AtomicBoolean instanceFlag = new AtomicBoolean(false);
    private static final JobImpl exit = new JobImpl(Long.MIN_VALUE, Long.MIN_VALUE, () -> {});
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
        if(Integer.bitCount(slots) != 1) {
            throw new FrameworkException(ExceptionType.WHEEL, "Slots must be power of two");
        }
        this.mask = slots - 1;
        this.tick = tick;
        this.tickNano = Duration.ofMillis(tick).toNanos();
        this.bound = slots * tick;
        this.cMask = mask >> 1;
        this.queue = new MpscUnboundedAtomicArrayQueue<>(Constants.KB);
        this.wheel = new JobImpl[slots];
        this.wheelThread = Thread.ofPlatform().name("Wheel").unstarted(this::run);
    }

    public static final Wheel instance = new WheelImpl(Wheel.slots, Wheel.tick);

    @Override
    public void doInit() {
        wheelThread.start();
    }

    @Override
    public void doExit() throws InterruptedException {
        if (!queue.offer(exit)) {
            throw new FrameworkException(ExceptionType.WHEEL, Constants.UNREACHED);
        }
        wheelThread.join();
    }

    @Override
    public Job addJob(Runnable job, Duration delay) {
        return enqueue(delay.toMillis(), ONCE, job);
    }

    @Override
    public Job addPeriodicJob(Runnable job, Duration delay, Duration period) {
        long periodDelayMillis = period.toMillis();
        if (periodDelayMillis <= tick) {
            throw new FrameworkException(ExceptionType.WHEEL, "Wheel tick is too large for current task, might consider tuning Wheel's default parameters");
        }
        return enqueue(delay.toMillis(), periodDelayMillis, job);
    }

    @Override
    public Job addJob(LongConsumer job, Duration delay) {
        return enqueue(delay.toMillis(), ONCE, job);
    }

    @Override
    public Job addPeriodicJob(LongConsumer job, Duration delay, Duration period) {
        long periodDelayMillis = period.toMillis();
        if (periodDelayMillis <= tick) {
            throw new FrameworkException(ExceptionType.WHEEL, "Wheel tick is too large for current task, might consider tuning Wheel's default parameters");
        }
        return enqueue(delay.toMillis(), periodDelayMillis, job);
    }

    private Job enqueue(long delayMillis, long periodDelayMillis, Runnable job) {
        JobImpl result = new JobImpl(Clock.current() + delayMillis, periodDelayMillis, job);
        if (!queue.offer(result)) {
            throw new FrameworkException(ExceptionType.WHEEL, Constants.UNREACHED);
        }
        return result;
    }

    private Job enqueue(long delayMillis, long periodDelayMillis, LongConsumer job) {
        JobImpl result = new JobImpl(Clock.current() + delayMillis, periodDelayMillis, job);
        if (!queue.offer(result)) {
            throw new FrameworkException(ExceptionType.WHEEL, Constants.UNREACHED);
        }
        return result;
    }

    private void run() {
        long nano = Clock.nano();
        long milli = Clock.current();
        int slot = 0;
        for( ; ; ) {
            final long currentMilli = milli;
            final int currentSlot = slot;
            nano = nano + tickNano;
            milli = milli + tick;
            slot = (slot + 1) & mask;

            // inspecting if there are tasks that should be added to the wheel
            for( ; ;) {
                final JobImpl job = queue.poll();
                if(job == null) {
                    break;
                }else if(job == exit) {
                    return ;
                }else {
                    // if delay is smaller than current milli, we should run it in current slot, so we select tasks before running wheel
                    final long delayMillis = Math.max(job.execMilli - currentMilli, 0);
                    if(delayMillis >= bound) {
                        waitSet.add(job);
                    }else {
                        job.pos = (int) ((currentSlot + (delayMillis / tick)) & mask);
                        insert(job);
                    }
                }
            }

            // check if we need to scan the waiting queue
            if((currentSlot & cMask) == 0) {
                Iterator<JobImpl> iterator = waitSet.iterator();
                while (iterator.hasNext()) {
                    JobImpl job = iterator.next();
                    final long delayMillis = job.execMilli - currentMilli;
                    if(delayMillis < bound) {
                        iterator.remove();
                        job.pos = (int) ((currentSlot + (delayMillis / tick)) & mask);
                        insert(job);
                    }else {
                        // if current task is not for scheduling, then the following tasks won't be available
                        break;
                    }
                }
            }
            
            // processing wheel's current mission
            JobImpl current = wheel[currentSlot];
            while (current != null) {
                JobImpl next = current.next;
                // the executor thread and the canceller thread will fight for ownership, but only one would succeed
                if(current.owner.compareAndSet(false, true)) {
                    Thread.ofVirtual().name("wheel-job-" + counter.getAndIncrement()).start(current.mission);
                    final long period = current.period;
                    if(period == ONCE) {
                        // help GC
                        current.prev = null;
                        current.next = null;
                    } else {
                        // reset current node's status
                        current.owner.set(false);
                        current.execMilli = current.execMilli + period;
                        current.prev = null;
                        current.next = null;
                        if(period >= bound) {
                            waitSet.add(current);
                        }else {
                            current.pos = (int) ((currentSlot + (period / tick)) & mask);
                            insert(current);
                        }
                    }
                }
                current = next;
            }
            
            // park until next slot, it's safe even the value is negative
            LockSupport.parkNanos(nano - Clock.nano());
        }
    }

    private void insert(JobImpl target) {
        int pos = target.pos;
        final JobImpl current = wheel[pos];
        if(current != null) {
            target.next = current;
            current.prev = target;
        }
        wheel[pos] = target;
    }

    private JobImpl remove(JobImpl target) {
        int pos = target.pos;
        JobImpl prev = target.prev;
        JobImpl next = target.next;
        if(prev == null) {
            // TODO
        }
        return null;
    }

    private static final class JobImpl implements Job, Comparable<JobImpl> {
        private long execMilli;
        private int pos;
        private JobImpl prev;
        private JobImpl next;
        private final Runnable mission;
        private final AtomicBoolean owner = new AtomicBoolean(false);
        private final long period;

        JobImpl(long execMilli, long period, Runnable runnable) {
            this.execMilli = execMilli;
            this.period = period;
            this.mission = runnable;
        }

        JobImpl(long execMilli, long period, LongConsumer consumer) {
            this.execMilli = execMilli;
            this.period = period;
            this.mission = () -> {
                consumer.accept(execMilli);
            };
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
