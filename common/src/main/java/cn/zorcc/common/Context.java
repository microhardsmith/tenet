package cn.zorcc.common;

import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.structure.Mutex;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.util.*;

/**
 *   Context is the core of a tenet application, it serves as a singleton pool, and used for triggering Lifecycle events
 *   Application developers should provide different ContextListener implementation to affect how Context works in their app
 *   The containerMap was guarded by a lock, the performance of loading and getting container from Context is not our first concern
 *   Developers should load everything at startup, and let them exit in sequence when the whole application exits
 */
public final class Context {
    record Container(
            Object target,
            Class<?> type
    ) {

    }
    private static final Logger log = new Logger(Context.class);
    private static final Map<Class<?>, Object> containerMap = new HashMap<>();
    private static final Deque<Container> pendingContainers = new ArrayDeque<>();
    private static final State contextState = new State(Constants.INITIAL);
    private static final ContextListener contextListener = ServiceLoader.load(ContextListener.class).findFirst().orElse(new DefaultContextListener());

    public static void init() {
        long nano = Clock.nano();
        try(Mutex _ = contextState.withMutex()) {
            if(!contextState.cas(Constants.INITIAL, Constants.STARTING)) {
                throw new FrameworkException(ExceptionType.CONTEXT, "Context has been initialized");
            }
            contextState.set(Constants.STARTING);
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
            RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
            String[] pidAndDevice = runtimeMXBean.getName().split("@");
            log.info(STR."Process running with Pid: \{pidAndDevice[0]} on Device: \{pidAndDevice[1]}");
            log.info(STR."Tenet application started in \{ Duration.ofNanos(Clock.elapsed(nano)).toMillis()} ms, JVM running for \{runtimeMXBean.getUptime()} ms");
            contextState.set(Constants.RUNNING);
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
        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().unstarted(() -> finishedCycles.reversed().forEach(LifeCycle::UninterruptibleExit)));
    }

    public static <T> void load(T target, Class<T> type) {
        if(target == null) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Container can't be null");
        }
        if (!type.isAssignableFrom(target.getClass())) {
            throw new FrameworkException(ExceptionType.CONTEXT, STR."Container doesn't match its class : \{type.getSimpleName()}");
        }
        try (Mutex _ = contextState.withMutex()) {
            int currentState = contextState.get();
            if(currentState <= Constants.STARTING) {
                pendingContainers.addLast(new Container(target, type));
            }else {
                throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
            }
        }
    }


    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> type) {
        try(Mutex _ = contextState.withMutex()) {
            int currentState = contextState.get();
            if(currentState <= Constants.STARTING) {
                Object o = containerMap.computeIfAbsent(type, contextListener::onRequested);
                if(o == null) {
                    throw new FrameworkException(ExceptionType.CONTEXT, STR."Unable to retrieve target container : \{type.getName()}");
                }
                return (T) o;
            }else {
                throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
            }
        }
    }
}
