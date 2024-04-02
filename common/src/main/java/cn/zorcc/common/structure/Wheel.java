package cn.zorcc.common.structure;

import cn.zorcc.common.*;
import cn.zorcc.common.exception.FrameworkException;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

public sealed interface Wheel extends LifeCycle permits Wheel.WheelImpl {
    int slots = Integer.getInteger("wheel.slots", 4096);
    long tick = Long.getLong("wheel.tick", 10L);

    static Wheel wheel() {
        return WheelImpl.INSTANCE;
    }

    Runnable addJob(Runnable mission, Duration delay);

    Runnable addPeriodicJob(Runnable mission, Duration delay, Duration period);

    final class WheelImpl extends AbstractLifeCycle implements Wheel {
        private static final long ONE_TIME_MISSION = -1;
        private static final long CANCEL_ONE_TIME_MISSION = -2;
        private static final long CANCEL_PERIOD_MISSION = -3;
        private static final AtomicBoolean instanceFlag = new AtomicBoolean(false);
        private static final WheelTask exitTask = new WheelTask(Long.MIN_VALUE, Long.MIN_VALUE, () -> {});
        private final int mask;
        private final long tick;
        private final long tickNano;
        private final long bound;
        private final int cMask;
        private final TaskQueue<WheelTask> taskQueue;
        private final Job[] wheel;
        private final Map<Runnable, Job> jobMap = new HashMap<>();
        private final TreeSet<Job> waitSet = new TreeSet<>(Job::compareTo);
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
            this.taskQueue = new TaskQueue<>(Constants.KB);
            this.wheel = new Job[slots];
            this.wheelThread = Thread.ofPlatform().name("wheel").unstarted(this::run);
        }

        public static final Wheel INSTANCE = new WheelImpl(Wheel.slots, Wheel.tick);

        @Override
        public void doInit() {
            wheelThread.start();
        }

        @Override
        public void doExit() throws InterruptedException {
            taskQueue.offer(exitTask);
            wheelThread.join();
        }

        @Override
        public Runnable addJob(Runnable mission, Duration delay) {
            return addWheelTask(mission, delay, null);
        }

        @Override
        public Runnable addPeriodicJob(Runnable mission, Duration delay, Duration period) {
            long periodDelayMillis = period.toMillis();
            if (periodDelayMillis <= tick) {
                throw new FrameworkException(ExceptionType.WHEEL, "Wheel tick is too large for current task, might consider tuning Wheel's default parameters");
            }
            return addWheelTask(mission, delay, period);
        }

        private Runnable addWheelTask(Runnable mission, Duration delay, Duration period) {
            long current = Clock.current();
            WheelTask wheelTask = new WheelTask(current + delay.toMillis(), period == null ? ONE_TIME_MISSION : period.toMillis(), mission);
            WheelTask cancelTask = new WheelTask(current + delay.toMillis(), period == null ? CANCEL_ONE_TIME_MISSION : CANCEL_PERIOD_MISSION, mission);
            taskQueue.offer(wheelTask);
            return () -> taskQueue.offer(cancelTask);
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
                for (WheelTask task : taskQueue.elements()) {
                    if(task == exitTask) {
                        return ;
                    }else {
                        // if delay is smaller than current milli, we should run it in current slot, so we select tasks before running wheel
                        Job job = new Job(task.execMilli(), task.period(), task.mission());
                        long delayMillis = Math.max(task.execMilli() - currentMilli, 0);
                        int pos = calculatePos(currentSlot, delayMillis);
                        if(task.period() == CANCEL_ONE_TIME_MISSION) {
                            cancelJob(job, pos);
                        }else if(task.period() == CANCEL_PERIOD_MISSION) {
                            cancelPeriodJob(job);
                        }else {
                            registerPeriodJobIfNeeded(job);
                            insertJob(job, pos, delayMillis);
                        }
                    }
                }

                // check if we need to scan the waiting queue
                if((currentSlot & cMask) == 0) {
                    Iterator<Job> iterator = waitSet.iterator();
                    while (iterator.hasNext()) {
                        Job job = iterator.next();
                        long delayMillis = job.execMilli - currentMilli;
                        int pos = calculatePos(currentSlot, delayMillis);
                        if(delayMillis < bound) {
                            iterator.remove();
                            registerPeriodJobIfNeeded(job);
                            insertJob(job, pos, delayMillis);
                        }else {
                            // if current task is not for scheduling, then the following tasks won't be available
                            break;
                        }
                    }
                }

                // processing wheel's current mission
                Job current = wheel[currentSlot];
                while (current != null) {
                    Job next = current.next;
                    if(current.pos == currentSlot) {
                        Thread.ofVirtual().name("job").start(current.mission);
                        long period = current.period;
                        if(period != ONE_TIME_MISSION) {
                            current.execMilli = current.execMilli + period;
                            int pos = calculatePos(currentSlot, period);
                            insertJob(current, pos, period);
                        }else {
                            current.next = null;
                        }
                    }else {
                        current.next = null;
                    }
                    current = next;
                }
                wheel[currentSlot] = null;

                // park until next slot, it's safe even the value is negative
                LockSupport.parkNanos(nano - Clock.nano());
            }
        }

        private void cancelJob(Job job, int pos) {
            if(!waitSet.remove(job)) {
                Job current = wheel[pos];
                while (current != null) {
                    if(current.mission == job.mission) {
                        current.pos = -1;
                        return ;
                    }else {
                        current = current.next;
                    }
                }
                throw new FrameworkException(ExceptionType.WHEEL, "Wrong use of wheel");
            }
        }

        private void cancelPeriodJob(Job job) {
            Job target = jobMap.remove(job.mission);
            if(target != null) {
                if(target.pos == -1) {
                    if(!waitSet.remove(target)) {
                        throw new FrameworkException(ExceptionType.WHEEL, Constants.UNREACHED);
                    }
                }else {
                    Job current = wheel[target.pos];
                    while (current != null) {
                        if(current.mission == job.mission) {
                            current.pos = -1;
                            return ;
                        }else {
                            current = current.next;
                        }
                    }
                }
            }else {
                throw new FrameworkException(ExceptionType.WHEEL, "Wrong use of wheel");
            }
        }

        private void registerPeriodJobIfNeeded(Job job) {
            if(job.period != ONE_TIME_MISSION) {
                jobMap.put(job.mission, job);
            }
        }

        private void insertJob(Job job, int pos, long delayMillis) {
            if(delayMillis >= bound) {
                job.pos = -1;
                waitSet.add(job);
            }else {
                job.pos = pos;
                Job current = wheel[pos];
                if(current != null) {
                    job.next = current;
                }
                wheel[pos] = job;
            }
        }

        private int calculatePos(int currentSlot, long delayMillis) {
            return (int) ((currentSlot + (delayMillis / tick)) & mask);
        }

        private static final class Job implements Comparable<Job> {
            private long execMilli;
            private int pos;
            private Job next;
            private final long period;
            private final Runnable mission;

            Job(long execMilli, long period, Runnable runnable) {
                this.execMilli = execMilli;
                this.period = period;
                this.mission = runnable;
            }

            @Override
            public int compareTo(Job other) {
                return Long.compare(execMilli, other.execMilli);
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Job job && mission == job.mission;
            }
        }
    }

    record WheelTask(
            long execMilli,
            long period,
            Runnable mission
    ) {
    }
}
