package cn.zorcc.common.network;

import cn.zorcc.common.*;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.network.api.Decoder;
import cn.zorcc.common.network.api.Encoder;
import cn.zorcc.common.network.api.Handler;
import cn.zorcc.common.structure.Wheel;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class EchoTest {
    private static final Logger log = new Logger(EchoTest.class);
    private static final int port = 8002;
    private static final Loc serverLoc = new Loc(IpType.IPV6, port);
    private static final Loc clientIpv4Loc = new Loc(IpType.IPV4, "127.0.0.1", port);
    private static final Loc clientIpv6Loc = new Loc(IpType.IPV6, "::1", port);

    @Test
    public void testIpv6EchoClient() throws InterruptedException {
        Net netClient = createEchoNetClient();
        Context.load(netClient, Net.class);
        Context.init();
        netClient.connect(clientIpv6Loc, new EchoEncoder(), new EchoDecoder(), new EchoClientHandler(), Net.tcpProvider());
        Thread.sleep(Long.MAX_VALUE);
    }

    @Test
    public void testIpv4EchoClient() throws InterruptedException {
        Net netClient = createEchoNetClient();
        Context.load(netClient, Net.class);
        Context.init();
        netClient.connect(clientIpv4Loc, new EchoEncoder(), new EchoDecoder(), new EchoClientHandler(), Net.tcpProvider());
        Thread.sleep(Long.MAX_VALUE);
    }

    @Test
    public void testEchoServer() throws InterruptedException {
        Net netServer = createEchoNetServer();
        Context.load(netServer, Net.class);
        Context.init();
        Thread.sleep(Long.MAX_VALUE);
    }

    private static Net createEchoNetClient() {
        return new Net();
    }

    private static class EchoClientHandler implements Handler {
        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public void onConnected(Channel channel) {
            log.info("Client channel connected");
            Wheel.wheel().addPeriodicJob(() -> channel.sendMsg("Hello : " + counter.getAndIncrement()), Duration.ZERO, Duration.ofSeconds(1));
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
        }

        @Override
        public void onRemoved(Channel channel) {
            log.info("Client channel removed");
        }
    }

    private static Net createEchoNetServer() {
        ListenerConfig listenerConfig = new ListenerConfig();
        listenerConfig.setEncoderSupplier(EchoEncoder::new);
        listenerConfig.setDecoderSupplier(EchoDecoder::new);
        listenerConfig.setHandlerSupplier(EchoServerHandler::new);
        listenerConfig.setProvider(Net.tcpProvider());
        listenerConfig.setLoc(serverLoc);
        Net net = new Net();
        net.addListener(listenerConfig);
        return net;
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
                throw new FrameworkException(ExceptionType.NETWORK, "Require a string");
            }
        }
    }
}
