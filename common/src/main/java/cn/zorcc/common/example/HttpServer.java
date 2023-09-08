package cn.zorcc.common.example;

import cn.zorcc.common.Chain;
import cn.zorcc.common.Clock;
import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.http.*;
import cn.zorcc.common.log.LoggerConsumer;
import cn.zorcc.common.network.*;
import cn.zorcc.common.pojo.Loc;
import cn.zorcc.common.util.CompressUtil;
import cn.zorcc.common.wheel.Wheel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;

/**
 *   Test class for building a executable fat-jar
 */
public final class HttpServer {
    private static final Logger log = LoggerFactory.getLogger(HttpServer.class);
    private static final Loc HTTP_LOC = new Loc("0.0.0.0", 80);
    private static final Loc HTTPS_LOC = new Loc("0.0.0.0", 443);
    private static final String PUBLIC_KEY_FILE = "/Users/liuxichen/workspace/ca/server.crt";
    private static final String PRIVATE_KEY_FILE = "/Users/liuxichen/workspace/ca/server.key";
    public static void main(String[] args) {
        long nano = Clock.nano();
        Chain chain = Chain.chain();
        chain.add(Wheel.wheel());
        chain.add(new LoggerConsumer());

        //chain.add(createHttpNet());

        //chain.add(createHttpsNet());

        chain.add(createHttpAndHttpsNet());
        chain.run();

        long jvmTime = ManagementFactory.getRuntimeMXBean().getUptime();
        log.info("Starting now, causing {} ms, JVM started for {} ms", TimeUnit.NANOSECONDS.toMillis(Clock.elapsed(nano)), jvmTime);
    }

    public static Net createHttpNet() {
        NetworkConfig networkConfig = new NetworkConfig();
        Net net = new Net(networkConfig);
        MuxConfig muxConfig = new MuxConfig();
        net.addMaster(HttpServerEncoder::new, HttpServerDecoder::new, HttpTestHandler::new, TcpConnector::new, HTTP_LOC, muxConfig);
        for(int i = 0; i < 4; i++) {
            net.addWorker(networkConfig, muxConfig);
        }
        return net;
    }

    public static Net createHttpsNet() {
        NetworkConfig networkConfig = new NetworkConfig();
        networkConfig.setEnableSsl(true);
        networkConfig.setPublicKeyFile(PUBLIC_KEY_FILE);
        networkConfig.setPrivateKeyFile(PRIVATE_KEY_FILE);
        Net net = new Net(networkConfig);
        MuxConfig muxConfig = new MuxConfig();
        net.addMaster(HttpServerEncoder::new, HttpServerDecoder::new, HttpTestHandler::new, net.sslServerConnectorSupplier(), HTTPS_LOC, muxConfig);
        for(int i = 0; i < 4; i++) {
            net.addWorker(networkConfig, muxConfig);
        }
        return net;
    }

    public static Net createHttpAndHttpsNet() {
        NetworkConfig networkConfig = new NetworkConfig();
        networkConfig.setEnableSsl(true);
        networkConfig.setPublicKeyFile(PUBLIC_KEY_FILE);
        networkConfig.setPrivateKeyFile(PRIVATE_KEY_FILE);
        Net net = new Net(networkConfig);
        MuxConfig muxConfig = new MuxConfig();
        net.addMaster(HttpServerEncoder::new, HttpServerDecoder::new, HttpTestHandler::new, TcpConnector::new, HTTP_LOC, muxConfig);
        net.addMaster(HttpServerEncoder::new, HttpServerDecoder::new, HttpTestHandler::new, net.sslServerConnectorSupplier(), HTTPS_LOC, muxConfig);
        for(int i = 0; i < 4; i++) {
            net.addWorker(networkConfig, muxConfig);
        }
        return net;
    }

    private static class HttpTestHandler implements Handler {
        private static final Logger log = LoggerFactory.getLogger(HttpTestHandler.class);
        private static final byte[] body = """
                {
                    "hello" : "world"
                }
                """.getBytes(StandardCharsets.UTF_8);
        private static final ZoneId gmt = ZoneId.of("GMT");
        private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(gmt);
        private final ThreadFactory threadFactory = Thread.ofVirtual().name("http-", 0).factory();
        @Override
        public void onConnected(Channel channel) {
            log.debug("Http connection established : {}", channel.loc());
        }

        @Override
        public void onRecv(Channel channel, Object data) {
            if(data instanceof HttpRequest httpRequest) {
                // TODO 好像有线程安全问题，只有在多个worker时的情况下会触发
//                threadFactory.newThread(() -> onHttpRequest(channel, httpRequest)).start();
                onHttpRequest(channel, httpRequest);
            }else {
                throw new FrameworkException(ExceptionType.HTTP, Constants.UNREACHED);
            }
        }

        private void onHttpRequest(Channel channel, HttpRequest httpRequest) {
            HttpResponse httpResponse = new HttpResponse();
            HttpHeader headers = httpResponse.getHeaders();
            headers.put("Content-Type", "application/json; charset=utf-8");
            headers.put("Date", formatter.format(ZonedDateTime.now(gmt)));
            String acceptEncoding = httpRequest.getHttpHeader().get(HttpHeader.K_ACCEPT_ENCODING);
            if(acceptEncoding != null && acceptEncoding.contains("gzip")) {
                headers.put(HttpHeader.K_CONTENT_ENCODING, HttpHeader.V_GZIP);
                httpResponse.setData(CompressUtil.compressUsingJdkGzip(body, Deflater.BEST_COMPRESSION));
            }else {
                httpResponse.setData(body);
            }
            channel.send(httpResponse);
        }

        @Override
        public void onShutdown(Channel channel) {
            log.debug("Http connection shutdown : {}", channel.loc());
        }

        @Override
        public void onRemoved(Channel channel) {
            log.debug("Http connection closed : {}", channel.loc());
        }
    }
}
