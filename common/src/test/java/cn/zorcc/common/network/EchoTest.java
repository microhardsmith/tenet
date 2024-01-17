package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Context;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.TestConstants;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.network.api.Decoder;
import cn.zorcc.common.network.api.Encoder;
import cn.zorcc.common.network.api.Handler;
import cn.zorcc.common.structure.ReadBuffer;
import cn.zorcc.common.structure.Wheel;
import cn.zorcc.common.structure.WriteBuffer;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class EchoTest {
    private static final Logger log = new Logger(EchoTest.class);

    @Test
    public void testIpv6EchoClient() throws InterruptedException {
        Net netClient = createEchoNetClient();
        Context.load(netClient, Net.class);
        Context.init();
        netClient.connect(TestConstants.CLIENT_IPV6_LOC, new EchoEncoder(), new EchoDecoder(), new EchoClientHandler(), Net.tcpProvider());
        Thread.sleep(Long.MAX_VALUE);
    }

    @Test
    public void testIpv6EchoClientWithSsl() throws InterruptedException {
        Net netClient = createEchoNetClient();
        Context.load(netClient, Net.class);
        Context.init();
        netClient.connect(TestConstants.CLIENT_IPV6_LOC, new EchoEncoder(), new EchoDecoder(), new EchoClientHandler(), Net.sslProvider());
        Thread.sleep(Long.MAX_VALUE);
    }

    @Test
    public void testIpv4EchoClient() throws InterruptedException {
        Net netClient = createEchoNetClient();
        Context.load(netClient, Net.class);
        Context.init();
        netClient.connect(TestConstants.CLIENT_IPV4_LOC, new EchoEncoder(), new EchoDecoder(), new EchoClientHandler(), Net.tcpProvider());
        Thread.sleep(Long.MAX_VALUE);
    }

    @Test
    public void testIpv4EchoClientWithSsl() throws InterruptedException {
        Net netClient = createEchoNetClient();
        Context.load(netClient, Net.class);
        Context.init();
        netClient.connect(TestConstants.CLIENT_IPV4_LOC, new EchoEncoder(), new EchoDecoder(), new EchoClientHandler(), Net.sslProvider());
        Thread.sleep(Long.MAX_VALUE);
    }

    @Test
    public void testIpv4EchoServer() throws InterruptedException {
        Net netServer = createEchoNetServer(false, false);
        Context.load(netServer, Net.class);
        Context.init();
        Thread.sleep(Long.MAX_VALUE);
    }

    @Test
    public void testIpv4EchoServerWithSsl() throws InterruptedException {
        Net netServer = createEchoNetServer(true, false);
        Context.load(netServer, Net.class);
        Context.init();
        Thread.sleep(Long.MAX_VALUE);
    }

    @Test
    public void testIpv6EchoServer() throws InterruptedException {
        Net netServer = createEchoNetServer(false, true);
        Context.load(netServer, Net.class);
        Context.init();
        Thread.sleep(Long.MAX_VALUE);
    }

    @Test
    public void testIpv6EchoServerWithSsl() throws InterruptedException {
        Net netServer = createEchoNetServer(true, true);
        Context.load(netServer, Net.class);
        Context.init();
        Thread.sleep(Long.MAX_VALUE);
    }

    private static Net createEchoNetClient() {
        NetConfig netConfig = new NetConfig();
        netConfig.setPollerCount(1);
        netConfig.setWriterCount(1);
        return new Net(netConfig);
    }

    private static Net createEchoNetServer(boolean usingSsl, boolean usingIpv6) {
        ListenerConfig listenerConfig = new ListenerConfig();
        listenerConfig.setEncoderSupplier(EchoEncoder::new);
        listenerConfig.setDecoderSupplier(EchoDecoder::new);
        listenerConfig.setHandlerSupplier(EchoServerHandler::new);
        if(usingSsl) {
            listenerConfig.setProvider(SslProvider.newServerProvider(TestConstants.SELF_PUBLIC_KEY_FILE, TestConstants.SELF_PRIVATE_KEY_FILE));
        }else {
            listenerConfig.setProvider(Net.tcpProvider());
        }
        listenerConfig.setLoc(usingIpv6 ? TestConstants.SERVER_IPV6_LOC : TestConstants.SERVER_IPV4_LOC);
        NetConfig netConfig = new NetConfig();
        netConfig.setPollerCount(1);
        netConfig.setWriterCount(1);
        Net net = new Net(netConfig);
        net.addServerListener(listenerConfig);
        return net;
    }

    private static class EchoClientHandler implements Handler {
        private final AtomicInteger counter = new AtomicInteger(0);
        private final AtomicReference<Runnable> task = new AtomicReference<>();

        @Override
        public void onConnected(Channel channel) {
            log.info(STR."Client channel connected, loc : \{channel.loc()}");
            task.set(Wheel.wheel().addPeriodicJob(() -> channel.sendMsg(STR."Hello : \{counter.getAndIncrement()}"), Duration.ZERO, Duration.ofSeconds(1)));
            Wheel.wheel().addJob(() -> channel.shutdown(Duration.ofSeconds(5)), Duration.ofSeconds(10));
        }

        @Override
        public TaggedResult onRecv(Channel channel, Object data) {
            if(data instanceof String str) {
                log.info(STR."Client receiving msg : \{str}");
                return null;
            }else {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
        }

        @Override
        public void onShutdown(Channel channel) {
            channel.sendMsg("Client going to shutdown");
            Runnable cancel = task.getAndSet(null);
            if(cancel != null) {
                cancel.run();
            }
        }

        @Override
        public void onRemoved(Channel channel) {
            log.info(STR."Client channel removed, loc : \{channel.loc()}");
            Runnable cancel = task.getAndSet(null);
            if(cancel != null) {
                cancel.run();
            }
        }
    }

    private static class EchoServerHandler implements Handler {
        @Override
        public void onConnected(Channel channel) {
            log.info(STR."Detecting channel connected from : \{channel.loc()}");
        }

        @Override
        public TaggedResult onRecv(Channel channel, Object data) {
            if(data instanceof String str) {
                log.info(STR."Msg received : [\{str}]");
                channel.sendMsg(str);
                return null;
            }else {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
        }

        @Override
        public void onShutdown(Channel channel) {
            log.info(STR."Detecting channel shutdown : \{channel.loc()}");
        }

        @Override
        public void onRemoved(Channel channel) {
            log.info(STR."Detecting channel removed : \{channel.loc()}");
        }
    }

    private static class EchoDecoder implements Decoder {
        @Override
        public void decode(ReadBuffer readBuffer, List<Object> entityList) {
            for( ; ; ) {
                long currentIndex = readBuffer.readIndex();
                if(readBuffer.available() < 4) {
                    return ;
                }
                int msgLength = readBuffer.readInt();
                if(readBuffer.available() < msgLength) {
                    readBuffer.setReadIndex(currentIndex);
                    return ;
                }
                entityList.add(new String(readBuffer.readBytes(msgLength), StandardCharsets.UTF_8));
            }
        }
    }

    private static class EchoEncoder implements Encoder {
        @Override
        public void encode(WriteBuffer writeBuffer, Object o) {
            if(o instanceof String str) {
                byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
                writeBuffer.writeInt(bytes.length);
                writeBuffer.writeBytes(bytes);
            }else {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
        }
    }
}
