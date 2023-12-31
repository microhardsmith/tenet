package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Context;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.TestConstants;
import cn.zorcc.common.bindings.DeflateBinding;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.http.*;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.network.api.Handler;
import cn.zorcc.common.util.CompressUtil;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class HttpTest {
    private static final Logger log = new Logger(HttpTest.class);
    @Test
    public void testHttpServer() throws InterruptedException {
        Context.load(createHttpNet(), Net.class);
        Context.init();
        Thread.sleep(Long.MAX_VALUE);
    }

    @Test
    public void testHttpsServer() throws InterruptedException {
        Context.load(createHttpsNet(), Net.class);
        Context.init();
        Thread.sleep(Long.MAX_VALUE);
    }

    @Test
    public void testHttpAndHttpsServer() throws InterruptedException {
        Context.load(createHttpAndHttpsNet(), Net.class);
        Context.init();
        Thread.sleep(Long.MAX_VALUE);
    }

    public static Net createHttpNet() {
        ListenerConfig httpListenerConfig = new ListenerConfig();
        httpListenerConfig.setEncoderSupplier(HttpServerEncoder::new);
        httpListenerConfig.setDecoderSupplier(HttpServerDecoder::new);
        httpListenerConfig.setHandlerSupplier(HttpTestHandler::new);
        httpListenerConfig.setProvider(Net.tcpProvider());
        httpListenerConfig.setLoc(TestConstants.HTTP_LOC);
        Net net = new Net();
        net.addServerListener(httpListenerConfig);
        return net;
    }

    public static Net createHttpsNet() {
        ListenerConfig httpsListenerConfig = new ListenerConfig();
        httpsListenerConfig.setEncoderSupplier(HttpServerEncoder::new);
        httpsListenerConfig.setDecoderSupplier(HttpServerDecoder::new);
        httpsListenerConfig.setHandlerSupplier(HttpTestHandler::new);
        httpsListenerConfig.setProvider(SslProvider.newServerProvider(TestConstants.SERVER_PUBLIC_KEY_FILE, TestConstants.SERVER_PRIVATE_KEY_FILE));
        httpsListenerConfig.setLoc(TestConstants.HTTPS_LOC);
        Net net = new Net();
        net.addServerListener(httpsListenerConfig);
        return net;
    }

    public static Net createHttpAndHttpsNet() {
        ListenerConfig httpListenerConfig = new ListenerConfig();
        httpListenerConfig.setEncoderSupplier(HttpServerEncoder::new);
        httpListenerConfig.setDecoderSupplier(HttpServerDecoder::new);
        httpListenerConfig.setHandlerSupplier(HttpTestHandler::new);
        httpListenerConfig.setProvider(Net.tcpProvider());
        httpListenerConfig.setLoc(TestConstants.HTTP_LOC);
        ListenerConfig httpsListenerConfig = new ListenerConfig();
        httpsListenerConfig.setEncoderSupplier(HttpServerEncoder::new);
        httpsListenerConfig.setDecoderSupplier(HttpServerDecoder::new);
        httpsListenerConfig.setHandlerSupplier(HttpTestHandler::new);
        httpsListenerConfig.setProvider(SslProvider.newServerProvider(TestConstants.SERVER_PUBLIC_KEY_FILE, TestConstants.SERVER_PRIVATE_KEY_FILE));
        httpsListenerConfig.setLoc(TestConstants.HTTPS_LOC);
        Net net = new Net();
        net.addServerListener(httpListenerConfig);
        net.addServerListener(httpsListenerConfig);
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

        @Override
        public void onConnected(Channel channel) {
            log.debug(STR."Http connection established : \{channel.loc()}");
        }

        @Override
        public TaggedResult onRecv(Channel channel, Object data) {
            if(data instanceof HttpRequest httpRequest) {
                Thread.ofVirtual().start(() -> onHttpRequest(channel, httpRequest));
                return null;
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
                httpResponse.setData(CompressUtil.compressUsingGzip(body, DeflateBinding.LIBDEFLATE_FASTEST_LEVEL));
            }else {
                httpResponse.setData(body);
            }
            channel.sendMsg(httpResponse);
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
