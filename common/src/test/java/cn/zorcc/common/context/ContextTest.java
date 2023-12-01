package cn.zorcc.common.context;

import cn.zorcc.common.AbstractLifeCycle;
import cn.zorcc.common.Context;
import cn.zorcc.common.log.Logger;
import org.junit.jupiter.api.Test;

public class ContextTest {
    private static final Logger log = new Logger(ContextTest.class);
    static class LifeCycleTest extends AbstractLifeCycle {

        @Override
        protected void doInit() {
            log.info("Init called");
        }

        @Override
        protected void doExit() throws InterruptedException {
            log.info("Exit called");
        }
    }

    @Test
    public void testContext() throws InterruptedException {
        Context.load(new LifeCycleTest(), LifeCycleTest.class);
        Context.init();
        Thread.sleep(Long.MAX_VALUE);
    }
}
