package cn.zorcc.log;

import cn.zorcc.common.Clock;
import cn.zorcc.common.network.Net;
import cn.zorcc.common.network.NetworkConfig;
import cn.zorcc.common.network.http.HttpCodec;
import cn.zorcc.common.util.ThreadUtil;
import cn.zorcc.common.wheel.Wheel;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NetTest {
    public static void main(String[] args) {
        testTcp();
        //testSsl();
    }

    public static void testTcp() {
        long nano = Clock.nano();
        Wheel.wheel().init();
        LoggerConsumer loggerConsumer = new LoggerConsumer();
        loggerConsumer.init();
        Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(loggerConsumer::shutdown));
        long logTime = TimeUnit.NANOSECONDS.toMillis(Clock.elapsed(nano));
        log.info("Initializing log and wheel cost : {} ms", logTime);
        Net net = new Net(HttpCodec::new, HttpTestHandler::new);
        Runtime.getRuntime().addShutdownHook(ThreadUtil.virtual("shutdown", net::shutdown));
        net.init();
        log.info("Net cost : {} ms", TimeUnit.NANOSECONDS.toMillis(Clock.elapsed(nano)) - logTime);
        long jvmTime = ManagementFactory.getRuntimeMXBean().getUptime();
        log.info("Start now, causing {} ms, jvm started for {} ms", TimeUnit.NANOSECONDS.toMillis(Clock.elapsed(nano)), jvmTime);
    }

    public static void testSsl() {
        long nano = Clock.nano();
        Wheel.wheel().init();
        LoggerConsumer loggerConsumer = new LoggerConsumer();
        loggerConsumer.init();
        Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(loggerConsumer::shutdown));
        NetworkConfig networkConfig = new NetworkConfig();
        networkConfig.setEnableSsl(true);
        networkConfig.setPublicKeyFile("C:/openresty-1.21.4.1-win64/conf/zorcc.cn+1.pem");
        networkConfig.setPrivateKeyFile("C:/openresty-1.21.4.1-win64/conf/zorcc.cn+1-key.pem");
        Net net = new Net(HttpCodec::new, HttpTestHandler::new, networkConfig);
        Runtime.getRuntime().addShutdownHook(ThreadUtil.virtual("shutdown", net::shutdown));
        net.init();
        long jvmTime = ManagementFactory.getRuntimeMXBean().getUptime();
        log.info("Start now, causing {} milliseconds, jvm started for {} milliseconds", TimeUnit.NANOSECONDS.toMillis(Clock.elapsed(nano)), jvmTime);
    }
}
