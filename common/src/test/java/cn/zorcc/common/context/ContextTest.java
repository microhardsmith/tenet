package cn.zorcc.common.context;

import cn.zorcc.common.AbstractLifeCycle;
import cn.zorcc.common.Context;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.structure.Wheel;
import org.junit.jupiter.api.Test;

import java.time.Duration;

public class ContextTest {
    private static final Logger log = new Logger(ContextTest.class);
    static class LifeCycleTest extends AbstractLifeCycle {
        @Override
        protected void doInit() {
            log.info("Init called");
        }

        @Override
        protected void doExit() {
            log.info("Exit called");
        }
    }

    @Test
    public void testContext() throws InterruptedException {
        Context.load(new LifeCycleTest(), LifeCycleTest.class);
        Context.init();
        Runnable exitOnce = Wheel.wheel().addJob(() -> log.info(STR."onetime\{Thread.currentThread().threadId()}"), Duration.ofSeconds(5));
        exitOnce.run();
        Runnable exitPeriod = Wheel.wheel().addPeriodicJob(() -> log.info(String.valueOf(Thread.currentThread().threadId())), Duration.ZERO, Duration.ofSeconds(1));
        Thread.sleep(5000);
        exitPeriod.run();
        Thread.sleep(Long.MAX_VALUE);
    }

    @Test
    public void testWheel() throws InterruptedException {
        Context.init();
        Wheel.wheel().addPeriodicJob(() -> log.debug("1"), Duration.ZERO, Duration.ofSeconds(1));
        Wheel.wheel().addJob(() -> log.info("hello"), Duration.ofSeconds(5));
        Wheel.wheel().addPeriodicJob(() -> log.debug("2"), Duration.ZERO, Duration.ofSeconds(3));
        Wheel.wheel().addJob(() -> log.info("world"), Duration.ofSeconds(10));
        Thread.sleep(Long.MAX_VALUE);
    }
}
