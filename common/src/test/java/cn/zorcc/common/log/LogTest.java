package cn.zorcc.common.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Context;
import cn.zorcc.common.wheel.Wheel;
import org.junit.jupiter.api.Test;

public class LogTest {
    private static final Logger log = new Logger(LogTest.class);

    @Test
    public void testConsoleLog() throws InterruptedException {
        Context.load(Wheel.wheel(), Wheel.class);
        Context.load(new LoggerConsumer(), LoggerConsumer.class);
        Context.init();
        for(int i = Constants.ZERO; i < Constants.KB; i++) {
            log.info(STR."hello , \{i}");
        }
        Thread.sleep(1000L);
    }

    @Test
    public void testFileLog() throws InterruptedException {
        Context.load(Wheel.wheel(), Wheel.class);
        LogConfig logConfig = new LogConfig();
        logConfig.setFile(new FileLogConfig());
        Context.load(new LoggerConsumer(logConfig), LoggerConsumer.class);
        Context.init();
        for(int i = Constants.ZERO; i < Constants.KB; i++) {
            log.info(STR."hello , \{i}");
        }
        Thread.sleep(1000L);
    }

    @Test
    public void testSqliteLog() throws InterruptedException {
        Context.load(Wheel.wheel(), Wheel.class);
        LogConfig logConfig = new LogConfig();
        logConfig.setSqlite(new SqliteLogConfig());
        Context.load(new LoggerConsumer(logConfig), LoggerConsumer.class);
        Context.init();
        for(int i = Constants.ZERO; i < Constants.KB; i++) {
            log.info(STR."hello , \{i}");
        }
        Thread.sleep(1000L);
    }
}
