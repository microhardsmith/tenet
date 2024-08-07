package cn.zorcc.common;

import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *   Context is the core of a tenet application, it serves as a singleton pool, and used for triggering Lifecycle events
 *   Application developers should provide different ContextListener implementation to affect how Context works in their app
 *   The containerMap was guarded by a lock, the performance of loading and getting container from Context is not our first concern
 *   Developers should load everything at startup, and let them exit in sequence when the whole application exits
 *   TODO refactor to dependency injection
 */
public final class Context {
    record Container(
            Object target,
            Class<?> type
    ) {

    }
    private static final Logger log = new Logger(Context.class);
    private static final Map<Class<?>, Object> containerMap = new ConcurrentHashMap<>();
    private static final Deque<Container> pendingContainers = new ConcurrentLinkedDeque<>();
    private static final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private static final Lock readLock = readWriteLock.readLock();
    private static final Lock writeLock = readWriteLock.writeLock();
    private static final ContextListener contextListener = ServiceLoader.load(ContextListener.class).findFirst().orElse(new DefaultContextListener());
    private static int state = Constants.INITIAL;

    public static void init() {
        long nano = Clock.nano();
        writeLock.lock();
        try{
            if(state != Constants.INITIAL) {
                throw new FrameworkException(ExceptionType.CONTEXT, "Context has been initialized");
            }
            List<Container> tempContainers = new ArrayList<>(pendingContainers);
            pendingContainers.clear();
            contextListener.beforeStarted();
            pendingContainers.addAll(tempContainers);
            List<LifeCycle> cycles = new ArrayList<>();
            while (!pendingContainers.isEmpty()) {
                Container container = pendingContainers.pollFirst();
                Object target = container.target();
                Class<?> type = container.type();
                if(containerMap.putIfAbsent(type, target) != null) {
                    throw new FrameworkException(ExceptionType.CONTEXT, STR."Container already exist : \{type.getSimpleName()}");
                }
                if(target instanceof LifeCycle lifeCycle) {
                    cycles.addLast(lifeCycle);
                }
                contextListener.onLoaded(target, type);
            }
            initializeCycles(cycles);
            contextListener.afterStarted();
            RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
            String[] pidAndDevice = runtimeMXBean.getName().split("@");
            log.info(STR."Process running with Pid: \{pidAndDevice[0]} on Device: \{pidAndDevice[1]}");
            log.info(STR."Tenet application started in \{ Duration.ofNanos(Clock.elapsed(nano)).toMillis()} ms, JVM running for \{runtimeMXBean.getUptime()} ms");
            state = Constants.RUNNING;
        } finally {
            writeLock.unlock();
        }
    }

    private static void initializeCycles(List<LifeCycle> cycles) {
        List<LifeCycle> finishedCycles = new ArrayList<>(cycles.size());
        for (LifeCycle cycle : cycles) {
            try{
                cycle.init();
            }catch (RuntimeException e) {
                log.error("Initializing container failed", e);
                finishedCycles.reversed().forEach(LifeCycle::UninterruptibleExit);
                System.exit(1);
            }
            finishedCycles.addLast(cycle);
        }
        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().unstarted(() -> {
            writeLock.lock();
            try {
                if(state == Constants.RUNNING) {
                    state = Constants.CLOSING;
                    finishedCycles.reversed().forEach(LifeCycle::UninterruptibleExit);
                    state = Constants.STOPPED;
                }
            } finally {
                writeLock.unlock();
            }
        }));
    }

    public static <T> void load(T target, Class<T> type) {
        if(target == null) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Container can't be null");
        }
        if (!type.isAssignableFrom(target.getClass())) {
            throw new FrameworkException(ExceptionType.CONTEXT, STR."Container doesn't match its class : \{type.getSimpleName()}");
        }
        readLock.lock();
        try {
            if(state <= Constants.RUNNING) {
                pendingContainers.addLast(new Container(target, type));
            }
        } finally {
            readLock.unlock();
        }
    }


    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> type) {
        readLock.lock();
        try {
            if(state <= Constants.RUNNING) {
                Object o = containerMap.computeIfAbsent(type, contextListener::onRequested);
                if(o == null) {
                    throw new FrameworkException(ExceptionType.CONTEXT, STR."Unable to retrieve target container : \{type.getName()}");
                }
                return (T) o;
            }else {
                return (T) containerMap.get(type);
            }
        } finally {
            readLock.unlock();
        }
    }
}
