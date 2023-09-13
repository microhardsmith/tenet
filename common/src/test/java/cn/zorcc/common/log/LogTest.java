package cn.zorcc.common.log;

import cn.zorcc.common.Chain;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogTest {
    private static final Logger log = LoggerFactory.getLogger(LogTest.class);

    @BeforeAll
    public static void beforeAll() {
        Chain chain = Chain.chain();
        chain.add(new LoggerConsumer());
        chain.run();
    }

    @Test
    public void testPlainLog() throws InterruptedException {
        log.info("hello");
        Thread.sleep(200L);
    }
}
