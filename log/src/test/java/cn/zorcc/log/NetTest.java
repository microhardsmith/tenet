package cn.zorcc.log;

import cn.zorcc.common.Clock;
import cn.zorcc.common.network.Net;
import cn.zorcc.common.network.http.HttpCodec;
import cn.zorcc.common.util.ThreadUtil;
import cn.zorcc.common.wheel.Wheel;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NetTest {
    public static void main(String[] args) {
        long nano = Clock.nano();
        Wheel.wheel().init();
        LoggerConsumer loggerConsumer = new LoggerConsumer();
        loggerConsumer.init();
        Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(loggerConsumer::shutdown));
        Net net = new Net(HttpCodec::new, HttpTestHandler::new);
        Runtime.getRuntime().addShutdownHook(ThreadUtil.virtual("shutdown", net::shutdown));
        net.init();
        long jvmTime = ManagementFactory.getRuntimeMXBean().getUptime();
        log.info("Start now, causing {} milliseconds, jvm started for {} milliseconds", TimeUnit.NANOSECONDS.toMillis(Clock.elapsed(nano)), jvmTime);
    }
}
