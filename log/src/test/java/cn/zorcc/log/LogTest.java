package cn.zorcc.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Context;
import cn.zorcc.common.event.ContextEvent;
import cn.zorcc.common.event.EventPipeline;
import cn.zorcc.common.util.NativeUtil;
import cn.zorcc.common.wheel.TimeWheel;
import cn.zorcc.common.wheel.TimeWheelImpl;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentScope;
import java.lang.foreign.SymbolLookup;

@Slf4j
public class LogTest {
    public static void main(String[] args) throws Throwable {
        TimeWheel.instance().start();
        log.info("hello");
        Thread.sleep(Long.MAX_VALUE);
    }

    private static void testAnsi() {
        System.out.println();
    }

    private static void testLog() throws InterruptedException {
        EventPipeline<LogEvent> pipeline = Context.pipeline(LogEvent.class);
        pipeline.init();
        // log.info("hello world");
        Thread.sleep(Long.MAX_VALUE);
    }

    private static void testNative() {
        SymbolLookup symbolLookup = NativeUtil.loadLibraryFromResource("/lib/test.dll");
        System.out.println(symbolLookup == null);
    }

    private static void testFile() throws IOException {
        MemorySegment debugSegment = Constants.DEBUG_SEGMENT;
        SegmentScope scope = debugSegment.scope();
        System.out.println(scope.equals(SegmentScope.global()));
        System.out.println(scope.equals(SegmentScope.auto()));
    }

}
