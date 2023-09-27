package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Context;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.http.*;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.log.LoggerConsumer;
import cn.zorcc.common.pojo.Loc;
import cn.zorcc.common.util.CompressUtil;
import cn.zorcc.common.wheel.Wheel;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.zip.Deflater;

public class HttpServerTest {
    private static final Logger log = new Logger(HttpServerTest.class);
    private static final Loc HTTP_LOC = new Loc("0.0.0.0", 80);
    private static final Loc HTTPS_LOC = new Loc("0.0.0.0", 443);
    private static final String PUBLIC_KEY_FILE = "/Users/liuxichen/workspace/ca/server.crt";
    private static final String PRIVATE_KEY_FILE = "/Users/liuxichen/workspace/ca/server.key";
    @Test
    public void testHttpServer() {
        Context.load(Wheel.wheel(), Wheel.class);
        Context.load(new LoggerConsumer(), LoggerConsumer.class);
        Context.load(createHttpNet(), Net.class);
        Context.init();
    }

    @Test
    public void testHttpsServer() {
        Context.load(Wheel.wheel(), Wheel.class);
        Context.load(new LoggerConsumer(), LoggerConsumer.class);
        Context.load(createHttpsNet(), Net.class);
        Context.init();
    }

    @Test
    public void testHttpAndHttpsServer() {
        Context.load(Wheel.wheel(), Wheel.class);
        Context.load(new LoggerConsumer(), LoggerConsumer.class);
        Context.load(createHttpAndHttpsNet(), Net.class);
        Context.init();
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
        private static final MemorySegment body = MemorySegment.ofArray("""
                {
                    "hello" : "world"
                }
                """.getBytes(StandardCharsets.UTF_8));
        private static final ZoneId gmt = ZoneId.of("GMT");
        private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(gmt);
        private static final Executor executor = Executors.newVirtualThreadPerTaskExecutor();
        @Override
        public void onConnected(Channel channel) {
            log.debug(STR."Http connection established : \{channel.loc()}");
        }

        @Override
        public void onRecv(Channel channel, Object data) {
            if(data instanceof HttpRequest httpRequest) {
                executor.execute(() -> onHttpRequest(channel, httpRequest));
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
            if(acceptEncoding != null && acceptEncoding.contains(HttpHeader.V_GZIP)) {
                headers.put(HttpHeader.K_CONTENT_ENCODING, HttpHeader.V_GZIP);
                httpResponse.setData(CompressUtil.compressUsingGzip(body, Deflater.BEST_COMPRESSION));
            }else {
                httpResponse.setData(body);
            }
            channel.send(httpResponse);
        }

        @Override
        public void onShutdown(Channel channel) {
            log.debug(STR."Http connection shutdown : \{channel.loc()}");
        }

        @Override
        public void onRemoved(Channel channel) {
            log.debug(STR."Http connection closed : \{channel.loc()}");
        }
    }
}
