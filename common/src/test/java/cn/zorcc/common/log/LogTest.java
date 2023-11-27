package cn.zorcc.common.log;

import cn.zorcc.common.Context;
import org.junit.jupiter.api.Test;

public class LogTest {
    private static final Logger log = new Logger(LogTest.class);

    @Test
    public void testLogWithException() {
        Context.init();
        for(int i = 0; i < 10; i++) {
            log.error("test exception", new RuntimeException());
        }
    }

    @Test
    public void testLog() {
        Context.init();
        for(int i = 0; i < 50; i++) {
            log.info(STR."hello , \{i}");
        }
    }
}
