package cn.zorcc.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Context;
import cn.zorcc.common.event.EventPipeline;
import cn.zorcc.common.wheel.Wheel;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentScope;
import java.lang.foreign.ValueLayout;
import java.time.Instant;
import java.time.LocalDateTime;

@Slf4j
public class LogTest {
    public static void main(String[] args) throws Throwable {
        testOffset();
    }

    private static void testLog() throws InterruptedException {
        Wheel.wheel().init();
        EventPipeline<LogEvent> pipeline = Context.pipeline(LogEvent.class);
        pipeline.init();
        for(int i = 0;i < 1000; i++) {
            log.info("hello " + i);
        }
        Thread.sleep(Long.MAX_VALUE);
    }

    private static void testTime() {
        final Instant instant = Constants.SYSTEM_CLOCK.instant();
        final LocalDateTime now = LocalDateTime.ofEpochSecond(instant.getEpochSecond(), instant.getNano(), Constants.LOCAL_ZONE_OFFSET);
        System.out.println(now);
        System.out.println(instant.toEpochMilli());
        System.out.println(System.currentTimeMillis());
    }

    private static void testConsole() throws InterruptedException {
        System.out.println("\033[31mhello\033[0m");
    }

    private static void testNative() {
        MemorySegment memorySegment = MemorySegment.allocateNative(100, SegmentScope.auto());
        MemorySegment.copy(Constants.BLUE_SEGMENT, 0, memorySegment, 0, 3);
    }

    private static void testFile() throws IOException {
        MemorySegment debugSegment = Constants.DEBUG_SEGMENT;
        SegmentScope scope = debugSegment.scope();
        System.out.println(scope.equals(SegmentScope.global()));
        System.out.println(scope.equals(SegmentScope.auto()));
    }

    private static void testOffset() {
        final MemoryLayout epollDataLayout = MemoryLayout.unionLayout(
                ValueLayout.ADDRESS.withName("ptr"),
                ValueLayout.JAVA_INT.withName("fd"),
                ValueLayout.JAVA_INT.withName("u32"),
                ValueLayout.JAVA_LONG.withName("u64"),
                ValueLayout.JAVA_INT.withName("sock"),
                ValueLayout.ADDRESS.withName("hnd")
        );
        final MemoryLayout epollEventLayout = MemoryLayout.structLayout(
                ValueLayout.JAVA_INT.withName("events"),
                MemoryLayout.paddingLayout(32),
                epollDataLayout.withName("data")
        );
        System.out.println(epollEventLayout.byteOffset(MemoryLayout.PathElement.groupElement("data")));
    }

}
