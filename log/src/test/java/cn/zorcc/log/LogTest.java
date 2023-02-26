package cn.zorcc.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Context;
import cn.zorcc.common.event.EventPipeline;
import cn.zorcc.common.pojo.Loc;
import cn.zorcc.common.util.NativeUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.foreign.*;
import java.lang.reflect.Field;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetTime;
import java.util.Set;

import static java.nio.file.StandardOpenOption.*;

@Slf4j
public class LogTest {
    public static void main(String[] args) throws Throwable {
        SegmentBuilder segmentBuilder = new SegmentBuilder(Arena.openConfined(), 1024);
        segmentBuilder.append(Constants.DEBUG_SEGMENT, NativeUtil.globalSegment(Constants.RED), 5);
        NativeUtil.test(segmentBuilder.segment(), 5, "");
    }

    private static void testLog() throws InterruptedException {
        EventPipeline<LogEvent> pipeline = Context.pipeline(LogEvent.class);
        pipeline.init();
        log.info("hello world");
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
