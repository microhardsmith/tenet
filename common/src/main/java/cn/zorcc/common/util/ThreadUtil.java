package cn.zorcc.common.util;

import cn.zorcc.common.Constants;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 *  虚拟线程工具类
 */
public class ThreadUtil {

    private ThreadUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     *  构建虚拟线程执行任务
     * @return 执行任务的虚拟线程
     */
    public static Thread virtual(String name, Runnable runnable) {
        return Thread.ofVirtual()
                .allowSetThreadLocals(false)
                .inheritInheritableThreadLocals(false)
                .uncaughtExceptionHandler(Constants.DEFAULT_EXCEPTION_HANDLER)
                .name(name)
                .unstarted(runnable);
    }

    public static ThreadFactory threadFactory(String name) {
        final AtomicLong counter = new AtomicLong(0L);
        return runnable -> Thread.ofPlatform()
                .name(name + "-" + counter.getAndIncrement())
                .allowSetThreadLocals(true)
                .inheritInheritableThreadLocals(true)
                .uncaughtExceptionHandler(Constants.DEFAULT_EXCEPTION_HANDLER)
                .unstarted(runnable);
    }

    /**
     *  获取当前线程线程名,如果为空则返回线程类型拼接编号
     */
    public static String threadName() {
        Thread currentThread = Thread.currentThread();
        String threadName = currentThread.getName();
        if(threadName.isBlank()) {
            return (currentThread.isVirtual() ? Constants.VIRTUAL_THREAD : Constants.PLATFORM_THREAD) + currentThread.threadId();
        }else {
            return threadName;
        }
    }
}
