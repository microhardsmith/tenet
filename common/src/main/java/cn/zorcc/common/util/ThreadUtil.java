package cn.zorcc.common.util;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

/**
 *  虚拟线程工具类
 */
public class ThreadUtil {
    private ThreadUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     *  构建虚拟线程执行任务,虚拟线程默认不允许thread-local
     * @param name 虚拟线程名称
     * @param runnable 任务体
     * @return 执行任务的虚拟线程
     */
    public static Thread virtual(final String name, final Runnable runnable) {
        return Thread.ofVirtual()
                .inheritInheritableThreadLocals(false)
                .name(name)
                .unstarted(runnable);
    }

    /**
     *  构建平台线程执行任务,平台线程默认允许thread-local
     * @param name 平台线程名称
     * @param runnable 任务体
     * @return 执行任务的平台线程
     */
    public static Thread platform(String name, Runnable runnable) {
        return Thread.ofPlatform()
                .inheritInheritableThreadLocals(true)
                .name(name)
                .unstarted(runnable);
    }

    /**
     *  获取当前线程线程名,如果为空则返回线程类型拼接编号
     */
    public static String threadName() {
        Thread currentThread = Thread.currentThread();
        String threadName = currentThread.getName();
        if(threadName.isEmpty()) {
            return (currentThread.isVirtual() ? Constants.VIRTUAL_THREAD : Constants.PLATFORM_THREAD) + currentThread.threadId();
        }else {
            return threadName;
        }
    }

    public static Thread checkVirtualThread() {
        Thread currentThread = Thread.currentThread();
        if(!currentThread.isVirtual()) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Must be invoked in virtual thread");
        }
        return currentThread;
    }
}
