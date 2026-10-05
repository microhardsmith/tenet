package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Context;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.TestConstants;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.http.HttpCompressionStatus;
import cn.zorcc.common.http.HttpHeader;
import cn.zorcc.common.http.HttpRequest;
import cn.zorcc.common.http.HttpServerDecoder;
import cn.zorcc.common.http.HttpServerEncoder;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.log.LoggerConsumer;
import cn.zorcc.common.structure.Wheel;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.lang.foreign.MemorySegment;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *   HttpTest can only own a single Net and a single Context.init() per JVM, because Net, LoggerConsumer and Context
 *   are all guarded by static single-instance / single-init flags. So all three original scenarios (http, https, both)
 *   are served by one Net inside one test method, and each scenario is verified with a real client request.
 */
public class HttpTest {
    private static final Logger log = new Logger(HttpTest.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static Net net;

    @Test
    public void testHttpAndHttpsServer() throws Exception {
        net = createHttpAndHttpsNet();
        try{
            Context.load(net, Net.class);
            Context.init();
            assertHttpWorks();
            assertHttpsWorks();
        }finally {
            shutdown();
        }
    }

    private static void assertHttpWorks() throws Exception {
        java.net.http.HttpResponse<String> response = send(newHttpClient(), "http://localhost:" + TestConstants.HTTP_PORT + "/hello");
        assertEquals(200, response.statusCode(), "Http server should answer 200");
        assertTrue(response.body().contains("\"hello\""), "Http response body should be the handler payload, got : " + response.body());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/json"),
                "Http response should carry the handler Content-Type header");
    }

    private static void assertHttpsWorks() throws Exception {
        java.net.http.HttpResponse<String> response = send(newHttpsClient(), "https://localhost:" + TestConstants.HTTPS_PORT + "/secure");
        assertEquals(200, response.statusCode(), "Https server should answer 200");
        assertTrue(response.body().contains("\"hello\""), "Https response body should be the handler payload, got : " + response.body());
    }

    private static java.net.http.HttpResponse<String> send(HttpClient client, String url) throws Exception {
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        return client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    /**
     *   The test certificate is self-signed, so the client has to trust it explicitly instead of the platform trust store
     */
    private static HttpClient newHttpsClient() throws Exception {
        TrustManager trustEverything = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                // test-only trust manager, the server certificate is generated for this suite
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                // test-only trust manager, the server certificate is generated for this suite
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{trustEverything}, new SecureRandom());
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(REQUEST_TIMEOUT)
                .sslContext(sslContext)
                .build();
    }

    private static HttpClient newHttpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
    }

    /**
     *   Context has no public exit(), it only registers a JVM shutdown hook. Without an explicit teardown the
     *   non-daemon net/poller/writer/wheel threads would keep the forked Surefire JVM alive forever, so the
     *   containers are stopped by hand in the reverse order Context used.
     */
    private static void shutdown() {
        exitQuietly("Net", net);
        exitQuietly("LoggerConsumer", Context.get(LoggerConsumer.class));
        exitQuietly("Wheel", Context.get(Wheel.class));
    }

    private static void exitQuietly(String name, cn.zorcc.common.LifeCycle lifeCycle) {
        if(lifeCycle == null) {
            return ;
        }
        try{
            lifeCycle.UninterruptibleExit();
        }catch (RuntimeException e) {
            System.err.println("Stopping " + name + " failed: " + e);
        }
    }

    private static Net createHttpAndHttpsNet() {
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
        httpsListenerConfig.setProvider(Provider.newSslServerProvider(TestConstants.SERVER_PUBLIC_KEY_FILE, TestConstants.SERVER_PRIVATE_KEY_FILE));
        httpsListenerConfig.setLoc(TestConstants.HTTPS_LOC);
        NetConfig netConfig = new NetConfig();
        netConfig.setPollerCount(1);
        netConfig.setWriterCount(1);
        netConfig.setGracefulShutdownTimeout(5);
        Net created = new Net(netConfig);
        created.serve(httpListenerConfig);
        created.serve(httpsListenerConfig);
        return created;
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
        public void onFailed(Channel channel) {
            log.debug("Http connection failed to establish : " + channel.loc());
        }

        @Override
        public void onConnected(Channel channel) {
            log.debug("Http connection established : " + channel.loc());
        }

        @Override
        public Optional<TagMsg> onRecv(Channel channel, Object data) {
            if(data instanceof HttpRequest httpRequest) {
                Thread.ofVirtual().start(() -> onHttpRequest(channel, httpRequest));
                return Optional.empty();
            }else {
                throw new FrameworkException(ExceptionType.HTTP, Constants.UNREACHED);
            }
        }

        private void onHttpRequest(Channel channel, HttpRequest httpRequest) {
            cn.zorcc.common.http.HttpResponse httpResponse = new cn.zorcc.common.http.HttpResponse();
            HttpHeader headers = httpResponse.getHeaders();
            headers.put("Content-Type", "application/json; charset=utf-8");
            headers.put("Date", formatter.format(ZonedDateTime.now(gmt)));
            String acceptEncoding = httpRequest.getHttpHeader().get(HttpHeader.K_ACCEPT_ENCODING);
            if(acceptEncoding != null) {
                if (acceptEncoding.contains(HttpHeader.V_GZIP)) {
                    httpResponse.setCompressionStatus(HttpCompressionStatus.GZIP);
                }else if(acceptEncoding.contains(HttpHeader.V_DEFLATE)) {
                    httpResponse.setCompressionStatus(HttpCompressionStatus.DEFLATE);
                }
            }
            httpResponse.setData(body);
            channel.sendMsg(httpResponse);
        }

        @Override
        public void onShutdown(Channel channel) {
            log.debug("Http connection shutdown : " + channel.loc());
        }

        @Override
        public void onRemoved(Channel channel) {
            log.debug("Http connection closed : " + channel.loc());
        }
    }
}
