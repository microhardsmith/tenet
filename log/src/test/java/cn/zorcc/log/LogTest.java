package cn.zorcc.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.util.NativeUtil;
import cn.zorcc.common.util.ThreadUtil;
import cn.zorcc.common.wheel.Wheel;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentScope;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class LogTest {
    public static void main(String[] args) throws Throwable {
        testWheel();
    }

    private static void testLog() throws InterruptedException {
        Wheel.wheel().init();
        LoggerConsumer loggerConsumer = new LoggerConsumer();
        loggerConsumer.init();
        Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(loggerConsumer::shutdown));
        for(int t = 0;t < 10;t++) {
            ThreadUtil.virtual(String.valueOf(t), () -> {
                for(int i = 0;i < 10; i++) {
                    log.info("hello " + i);
                }
            }).start();
        }
        Thread.sleep(Long.MAX_VALUE);
    }

    private static void testWheel() {
        AtomicLong a = new AtomicLong(System.currentTimeMillis());
        Wheel.wheel().init();
        Wheel.wheel().addPeriodicJob(() -> {
            long l = System.currentTimeMillis();
            System.out.println("hello " + (l - a.get()));
            a.set(l);
        }, 0L, 2565L, TimeUnit.MILLISECONDS);
    }


    private static void testTime() {
        final Instant instant = Constants.SYSTEM_CLOCK.instant();
        final LocalDateTime now = LocalDateTime.ofEpochSecond(instant.getEpochSecond(), instant.getNano(), Constants.LOCAL_ZONE_OFFSET);
        System.out.println(now);
        System.out.println(instant.toEpochMilli());
        System.out.println(System.currentTimeMillis());
    }

    private static void testFile() throws IOException {
        Set<StandardOpenOption> set = Set.of(StandardOpenOption.CREATE_NEW,
                StandardOpenOption.SPARSE,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE);
        try(FileChannel fc = FileChannel.open(Path.of("C:/workspace/hello.txt"), set)) {
            MemorySegment segment = fc.map(FileChannel.MapMode.READ_WRITE, 0, Constants.MB, SegmentScope.auto());
            NativeUtil.setByte(segment, 0L, Constants.SPACE);
            MemorySegment segment2 = fc.map(FileChannel.MapMode.READ_WRITE, Constants.MB, 2 * Constants.MB, SegmentScope.auto());
            NativeUtil.setByte(segment2, 0L, Constants.SPACE);
        }
    }

}
