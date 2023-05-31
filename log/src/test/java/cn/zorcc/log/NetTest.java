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
        long nano = Clock.nano();

        //testTcp();
        testSsl();

        long jvmTime = ManagementFactory.getRuntimeMXBean().getUptime();
        log.info("Starting now, causing {} ms, jvm started for {} ms", TimeUnit.NANOSECONDS.toMillis(Clock.elapsed(nano)), jvmTime);
    }

    public static void testTcp() {
        Wheel.wheel().init();
        LoggerConsumer loggerConsumer = new LoggerConsumer();
        loggerConsumer.init();
        Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(loggerConsumer::shutdown));
        Net net = new Net(HttpCodec::new, HttpTestHandler::new);
        Runtime.getRuntime().addShutdownHook(ThreadUtil.virtual("shutdown", net::shutdown));
        net.init();
    }

    public static void testSsl() {
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
    }
}
