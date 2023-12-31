package cn.zorcc.common;

import cn.zorcc.common.log.Logger;

/**
 *   For test running from jar purpose only
 */
public class LaunchTest {
    private static final Logger log = new Logger(LaunchTest.class);
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
    public static void main(String[] args) {
        Context.load(new LifeCycleTest(), LifeCycleTest.class);
        Context.init();

    }
}


