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
import java.util.concurrent.atomic.AtomicReference;

public class TagTest {
    private static final Logger log = new Logger(TagTest.class);

    @Test
    public void testIpv4TaggedClient() throws InterruptedException {
        Net netClient = createTagNetClient();
        Context.load(netClient, Net.class);
        Context.init();
        netClient.connect(TestConstants.CLIENT_IPV4_LOC, new TagEncoder(), new TagDecoder(), new TagClientHandler(), Net.tcpProvider());
        Thread.sleep(Long.MAX_VALUE);
    }

    @Test
    public void testIpv4TaggedClientWithSsl() throws InterruptedException {
        Net netClient = createTagNetClient();
        Context.load(netClient, Net.class);
        Context.init();
        netClient.connect(TestConstants.CLIENT_IPV4_LOC, new TagEncoder(), new TagDecoder(), new TagClientHandler(), Net.sslProvider());
        Thread.sleep(Long.MAX_VALUE);
    }

    @Test
    public void testIpv6TaggedClient() throws InterruptedException {
        Net netClient = createTagNetClient();
        Context.load(netClient, Net.class);
        Context.init();
        netClient.connect(TestConstants.CLIENT_IPV6_LOC, new TagEncoder(), new TagDecoder(), new TagClientHandler(), Net.tcpProvider());
        Thread.sleep(Long.MAX_VALUE);
    }

    @Test
    public void testIpv6TaggedClientWithSsl() throws InterruptedException {
        Net netClient = createTagNetClient();
        Context.load(netClient, Net.class);
        Context.init();
        netClient.connect(TestConstants.CLIENT_IPV6_LOC, new TagEncoder(), new TagDecoder(), new TagClientHandler(), Net.sslProvider());
        Thread.sleep(Long.MAX_VALUE);
    }

    @Test
    public void testIpv4TaggedServer() throws InterruptedException {
        Net netServer = createTagNetServer(false, false);
        Context.load(netServer, Net.class);
        Context.init();
        Thread.sleep(Long.MAX_VALUE);
    }

    @Test
    public void testIpv4TaggedServerWithSsl() throws InterruptedException {
        Net netServer = createTagNetServer(true, false);
        Context.load(netServer, Net.class);
        Context.init();
        Thread.sleep(Long.MAX_VALUE);
    }

    @Test
    public void testIpv6TaggedServer() throws InterruptedException {
        Net netServer = createTagNetServer(false, true);
        Context.load(netServer, Net.class);
        Context.init();
        Thread.sleep(Long.MAX_VALUE);
    }

    @Test
    public void testIpv6TaggedServerWithSsl() throws InterruptedException {
        Net netServer = createTagNetServer(true, true);
        Context.load(netServer, Net.class);
        Context.init();
        Thread.sleep(Long.MAX_VALUE);
    }

    private static Net createTagNetClient() {
        PollerConfig pollerConfig = new PollerConfig();
        pollerConfig.setPollerCount(1);
        WriterConfig writerConfig = new WriterConfig();
        writerConfig.setWriterCount(1);
        return new Net();
    }

    private static Net createTagNetServer(boolean usingSsl, boolean usingIpv6) {
        ListenerConfig listenerConfig = new ListenerConfig();
        listenerConfig.setEncoderSupplier(TagEncoder::new);
        listenerConfig.setDecoderSupplier(TagDecoder::new);
        listenerConfig.setHandlerSupplier(TagServerHandler::new);
        if(usingSsl) {
            listenerConfig.setProvider(SslProvider.newServerProvider(TestConstants.SELF_PUBLIC_KEY_FILE, TestConstants.SELF_PRIVATE_KEY_FILE));
        }else {
            listenerConfig.setProvider(Net.tcpProvider());
        }
        listenerConfig.setLoc(usingIpv6 ? TestConstants.SERVER_IPV6_LOC : TestConstants.SERVER_IPV4_LOC);
        PollerConfig pollerConfig = new PollerConfig();
        pollerConfig.setPollerCount(1);
        WriterConfig writerConfig = new WriterConfig();
        writerConfig.setWriterCount(1);
        Net net = new Net(pollerConfig, writerConfig);
        net.addServerListener(listenerConfig);
        return net;
    }

    private static class TagClientHandler implements Handler {
        private final AtomicInteger counter = new AtomicInteger(0);
        private final AtomicReference<Runnable> task = new AtomicReference<>();
        @Override
        public void onConnected(Channel channel) {
            log.info(STR."Client channel connected, loc : \{channel.loc()}");
            task.set(Wheel.wheel().addPeriodicJob(() -> channel.sendTaggedMsg(tag -> new Msg(tag, STR."Hello : \{counter.getAndIncrement()}")), Duration.ZERO, Duration.ofSeconds(1)));
            Wheel.wheel().addJob(() -> channel.shutdown(Duration.ofSeconds(5)), Duration.ofSeconds(30));
        }

        @Override
        public TaggedResult onRecv(Channel channel, Object data) {
            if(data instanceof Msg(int tag, String content)) {
                log.info(STR."Client receiving msg : \{content}");
                return new TaggedResult(tag, content);
            }else {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
        }

        @Override
        public void onShutdown(Channel channel) {
            channel.sendMsg(new Msg(Channel.SEQ, "Client going to shutdown"));
            task.getAndSet(null);
        }

        @Override
        public void onRemoved(Channel channel) {
            log.info(STR."Client channel removed, loc : \{channel.loc()}");
        }
    }

    private static class TagServerHandler implements Handler {
        @Override
        public void onConnected(Channel channel) {
            log.info(STR."Detecting channel connected from : \{channel.loc()}");
        }

        @Override
        public TaggedResult onRecv(Channel channel, Object data) {
            if(data instanceof Msg msg) {
                log.info(STR."Msg received : [\{msg.content()}]");
                channel.sendMsg(msg);
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

    private record Msg(int tag, String content) {

    }

    private static class TagDecoder implements Decoder {
        @Override
        public void decode(ReadBuffer readBuffer, List<Object> entityList) {
            for( ; ; ) {
                long currentIndex = readBuffer.readIndex();
                if(readBuffer.available() < 8) {
                    return ;
                }
                int tag = readBuffer.readInt();
                int msgLength = readBuffer.readInt();
                if(readBuffer.available() < msgLength) {
                    readBuffer.setReadIndex(currentIndex);
                    return ;
                }
                entityList.add(new Msg(tag, new String(readBuffer.readBytes(msgLength), StandardCharsets.UTF_8)));
            }
        }
    }

    private static class TagEncoder implements Encoder {
        @Override
        public void encode(WriteBuffer writeBuffer, Object o) {
            if(o instanceof Msg(int tag, String content)) {
                byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
                writeBuffer.writeInt(tag);
                writeBuffer.writeInt(bytes.length);
                writeBuffer.writeBytes(bytes);
            }else {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
        }
    }
}
