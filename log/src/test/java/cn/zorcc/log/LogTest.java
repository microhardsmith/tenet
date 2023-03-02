package cn.zorcc.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Context;
import cn.zorcc.common.event.EventPipeline;
import cn.zorcc.common.wheel.Wheel;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentScope;

@Slf4j
public class LogTest {
    public static void main(String[] args) throws Throwable {
        testLog();
    }

    private static void testLog() throws InterruptedException {
        Wheel.wheel().init();
        EventPipeline<LogEvent> pipeline = Context.pipeline(LogEvent.class);
        pipeline.init();
        for(int i = 0;i < 100000; i++) {
            log.info("hello " + i);
        }
        Thread.sleep(Long.MAX_VALUE);
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

}
