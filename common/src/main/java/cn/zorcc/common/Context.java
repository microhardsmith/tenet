package cn.zorcc.common;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.util.ThreadUtil;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class Context {
    private static final Logger log = new Logger(Context.class);
    private static final AtomicBoolean initFlag = new AtomicBoolean(false);
    private static final Map<Class<?>, Object> containerMap = new HashMap<>();
    private static final List<LifeCycle> cycles = new ArrayList<>();
    private static final Lock lock = new ReentrantLock();
    private static final ContextListener contextListener = ServiceLoader.load(ContextListener.class).findFirst().orElse(new DefaultContextListener());

    public static void init() {
        if (!initFlag.compareAndSet(false, true)) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Context has been initialized");
        }
        long nano = Clock.nano();
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        String[] pidAndDevice = runtimeMXBean.getName().split("@");
        log.info(STR."Process running with Pid: \{pidAndDevice[Constants.ZERO]} on Device: \{pidAndDevice[Constants.ONE]}");
        List<LifeCycle> temp = new ArrayList<>();
        boolean success = true;
        lock.lock();
        try{
            cycles.forEach(c -> {
                c.init();
                temp.add(c);
            });
        }catch (FrameworkException e) {
            log.error("Err caught when initializing container, exiting application now", e);
            temp.reversed().forEach(LifeCycle::UninterruptibleExit);
            success = false;
        }finally {
            lock.unlock();
        }
        if(success) {
            Runtime.getRuntime().addShutdownHook(ThreadUtil.platform("Exit", () -> cycles.reversed().forEach(LifeCycle::UninterruptibleExit)));
            contextListener.onStarted();
        }else {
            System.exit(Constants.ONE);
        }
        log.info(STR."Tenet application started in \{ Duration.ofNanos(Clock.elapsed(nano)).toMillis()} ms, JVM running for \{runtimeMXBean.getUptime()} ms");
    }

    public static <T> void load(T target, Class<T> type) {
        if(target == null) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Container can't be null");
        }
        if (!type.isAssignableFrom(target.getClass())) {
            throw new FrameworkException(ExceptionType.CONTEXT, STR."Container doesn't match its class : \{type.getSimpleName()}");
        }
        lock.lock();
        try{
            if(containerMap.putIfAbsent(type, target) != null) {
                throw new FrameworkException(ExceptionType.CONTEXT, STR."Container already exist : \{type.getSimpleName()}");
            }
            if(target instanceof LifeCycle lifeCycle) {
                cycles.add(lifeCycle);
            }
            contextListener.onLoaded(target, type);
        }finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> type) {
        lock.lock();
        try{
            Object o = containerMap.computeIfAbsent(type, contextListener::onRequested);
            if(o == null) {
                throw new FrameworkException(ExceptionType.CONTEXT, STR."Unable to find target container : \{type.getName()}");
            }
            return (T) o;
        }finally {
            lock.unlock();
        }
    }

}
