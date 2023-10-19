package cn.zorcc.common.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Context;
import cn.zorcc.common.structure.Wheel;
import org.junit.jupiter.api.Test;

public class LogTest {
    private static final Logger log = new Logger(LogTest.class);

    @Test
    public void testShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().unstarted(() -> {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("shutdown");
        }));
    }

    @Test
    public void testConsoleLog() {
        Context.load(Wheel.wheel(), Wheel.class);
        Context.load(new LoggerConsumer(), LoggerConsumer.class);
        Context.init();
        for(int i = Constants.ZERO; i < Constants.KB; i++) {
            log.info(STR."hello , \{i}");
        }
    }

    @Test
    public void testFileLog() {
        Context.load(Wheel.wheel(), Wheel.class);
        LogConfig logConfig = new LogConfig();
        logConfig.setFile(new FileLogConfig());
        Context.load(new LoggerConsumer(logConfig), LoggerConsumer.class);
        Context.init();
        for(int i = Constants.ZERO; i < Constants.KB; i++) {
            log.info(STR."hello , \{i}");
        }
    }

    @Test
    public void testSqliteLog() {
        Context.load(Wheel.wheel(), Wheel.class);
        LogConfig logConfig = new LogConfig();
        logConfig.setSqlite(new SqliteLogConfig());
        Context.load(new LoggerConsumer(logConfig), LoggerConsumer.class);
        Context.init();
        for(int i = Constants.ZERO; i < Constants.KB; i++) {
            log.info(STR."hello , \{i}");
        }
    }
}
