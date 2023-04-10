package cn.zorcc.common.net;

import cn.zorcc.common.util.ThreadUtil;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.ValueLayout;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class MacosTest {
    public static void main(String[] args) throws Exception {
        System.out.println(keventLayout.byteSize());
    }

    /**
     *  corresponding to struct kevent in event.h
     */
    private static final MemoryLayout keventLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_LONG.withName("ident"),
            ValueLayout.JAVA_SHORT.withName("filter"),
            ValueLayout.JAVA_SHORT.withName("flags"),
            ValueLayout.JAVA_INT.withName("fflags"),
            ValueLayout.JAVA_LONG.withName("data"),
            ValueLayout.ADDRESS.withName("udata")
    );

    private static void testPark() throws InterruptedException {
        System.out.println(System.currentTimeMillis());
        ThreadUtil.virtual("test1", () -> {
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(5));
            System.out.println(System.currentTimeMillis() + " test1");
        }).start();
        ThreadUtil.virtual("test2", () -> {
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(3));
            System.out.println(System.currentTimeMillis() + " test2");
        }).start();
        Thread.sleep(Long.MAX_VALUE);
    }

    private static void testScheduleExecutorService() throws InterruptedException {
        System.out.println(System.currentTimeMillis());
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.schedule(() -> {
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(5));
            System.out.println(System.currentTimeMillis() + " test1");
        }, 0, TimeUnit.SECONDS);
        scheduledExecutorService.schedule(() -> {
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(3));
            System.out.println(System.currentTimeMillis() + " test2");
        }, 0, TimeUnit.SECONDS);
        Thread.sleep(Long.MAX_VALUE);
    }
}
