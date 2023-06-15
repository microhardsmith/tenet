package cn.zorcc.common;

import cn.zorcc.common.log.LoggerConsumer;
import cn.zorcc.common.network.Channel;
import cn.zorcc.common.network.Handler;
import cn.zorcc.common.network.Net;
import cn.zorcc.common.network.NetworkConfig;
import cn.zorcc.common.network.http.HttpDecoder;
import cn.zorcc.common.network.http.HttpEncoder;
import cn.zorcc.common.network.http.HttpReq;
import cn.zorcc.common.network.http.HttpRes;
import cn.zorcc.common.util.ThreadUtil;
import cn.zorcc.common.wheel.Wheel;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 *   Test class for building a executable fat-jar
 */
@Slf4j
public class Test {
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
        Net net = new Net(HttpEncoder::new, HttpDecoder::new, HttpTestHandler::new);
        Runtime.getRuntime().addShutdownHook(ThreadUtil.virtual("shutdown", () -> {
            net.shutdown();
            loggerConsumer.shutdown();
        }));
        net.init();
    }

    public static void testSsl() {
        Wheel.wheel().init();
        LoggerConsumer loggerConsumer = new LoggerConsumer();
        loggerConsumer.init();
        NetworkConfig networkConfig = new NetworkConfig();
        networkConfig.setEnableSsl(true);
        networkConfig.setPublicKeyFile("C:/openresty-1.21.4.1-win64/conf/zorcc.cn+1.pem");
        networkConfig.setPrivateKeyFile("C:/openresty-1.21.4.1-win64/conf/zorcc.cn+1-key.pem");
        Net net = new Net(HttpEncoder::new, HttpDecoder::new, HttpTestHandler::new, networkConfig);
        Runtime.getRuntime().addShutdownHook(ThreadUtil.virtual("shutdown", () -> {
            net.shutdown();
            loggerConsumer.shutdown();
        }));
        net.init();
    }

    @Slf4j
    private static class HttpTestHandler implements Handler {
        private static final byte[] body = """
                {
                    "hello" : "world"
                }
                """.getBytes(StandardCharsets.UTF_8);
        private static final ZoneId gmt = ZoneId.of("GMT");
        private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(gmt);
        @Override
        public void onConnected(Channel channel) {
            log.debug("Http connection established");
        }

        @Override
        public void onRecv(Channel channel, Object data) {
            if(data instanceof HttpReq httpReq) {
                HttpRes httpRes = new HttpRes();
                Map<String, String> headers = httpRes.getHeaders();
                headers.put("Content-Type", "application/json; charset=utf-8");
                headers.put("Content-Length", String.valueOf(body.length));
                headers.put("Date", formatter.format(ZonedDateTime.now(gmt)));
                httpRes.setData(body);
                channel.send(httpRes);
            }
        }

        @Override
        public void onShutdown(Channel channel) {
            log.debug("Http connection shutdown");
        }

        @Override
        public void onRemoved(Channel channel) {
            log.debug("Http connection closed");
        }
    }
}
