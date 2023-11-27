package cn.zorcc.common;

import cn.zorcc.common.log.Logger;

/**
 *   For test running from jar purpose only
 */
public class LaunchTest {
    private static final Logger log = new Logger(LaunchTest.class);
    public static void main(String[] args) {
        Context.init();
        for(int i = 0; i < 10; i++) {
            log.error("test exception", new RuntimeException());
        }
    }
}


