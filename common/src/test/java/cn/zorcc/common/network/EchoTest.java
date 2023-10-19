package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Context;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.log.LoggerConsumer;
import cn.zorcc.common.structure.IpType;
import cn.zorcc.common.structure.Loc;
import cn.zorcc.common.structure.Wheel;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public class EchoTest {
    private static final Logger log = new Logger(EchoTest.class);
    private static final int port = 8002;
    private static final Loc serverLoc = new Loc(IpType.IPV6, port);
    private static final Loc clientIpv4Loc = new Loc(IpType.IPV4, "127.0.0.1", port);

    @Test
    public void testEchoClient() throws InterruptedException {
        Context.load(Wheel.wheel(), Wheel.class);
        Context.load(new LoggerConsumer(), LoggerConsumer.class);
        Net netClient = createEchoNetClient();
        Context.load(netClient, Net.class);
        Context.init();
        netClient.connect(serverLoc, new EchoEncoder(), new EchoDecoder(), new EchoClientHandler(), new TcpConnector());
        Thread.sleep(Long.MAX_VALUE);
    }

    @Test
    public void testIpv4EchoClient() throws InterruptedException {
        Context.load(Wheel.wheel(), Wheel.class);
        Context.load(new LoggerConsumer(), LoggerConsumer.class);
        Net netClient = createEchoNetClient();
        Context.load(netClient, Net.class);
        Context.init();
        netClient.connect(clientIpv4Loc, new EchoEncoder(), new EchoDecoder(), new EchoClientHandler(), new TcpConnector());
        Thread.sleep(Long.MAX_VALUE);
    }

    @Test
    public void testEchoServer() throws InterruptedException {
        Context.load(Wheel.wheel(), Wheel.class);
        Context.load(new LoggerConsumer(), LoggerConsumer.class);
        Net netServer = createEchoNetServer();
        Context.load(netServer, Net.class);
        Context.init();
        Thread.sleep(Long.MAX_VALUE);
    }

    private static Net createEchoNetClient() {
        return new Net(Constants.ONE);
    }

    private static class EchoClientHandler implements Handler {
        private final AtomicInteger counter = new AtomicInteger(Constants.ZERO);
        @Override
        public void onConnected(Channel channel) {
            log.info("Client channel connected");
            Wheel.wheel().addPeriodicJob(() -> channel.sendMsg("Hello : " + counter.getAndIncrement()), Duration.ZERO, Duration.ofSeconds(Constants.ONE));
        }

        @Override
        public void onRecv(Channel channel, Object data) {
            if(data instanceof String str) {
                log.info(STR."Client receiving msg : \{str}");
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
        MasterConfig masterConfig = new MasterConfig();
        masterConfig.setEncoderSupplier(EchoEncoder::new);
        masterConfig.setDecoderSupplier(EchoDecoder::new);
        masterConfig.setHandlerSupplier(EchoServerHandler::new);
        masterConfig.setProvider(Net.tcpProvider());
        masterConfig.setLoc(serverLoc);
        return new Net(masterConfig, Constants.ONE);
    }

    private static class EchoServerHandler implements Handler {
        @Override
        public void onConnected(Channel channel) {
            log.info(STR."Receiving client connected from : \{channel.loc()}");
        }

        @Override
        public void onRecv(Channel channel, Object data) {
            if(data instanceof String str) {
                log.info(STR."Msg received : [\{str}]");
                channel.sendMsg(str);
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
        public Object decode(ReadBuffer readBuffer) {
            long size = readBuffer.size();
            if(size < 4) {
                return null;
            }
            int msgLength = readBuffer.readInt();
            if(size < msgLength + 4) {
                return null;
            }
            return new String(readBuffer.readBytes(msgLength), StandardCharsets.UTF_8);
        }
    }

    private static class EchoEncoder implements Encoder {
        @Override
        public WriteBuffer encode(WriteBuffer writeBuffer, Object o) {
            if(o instanceof String str) {
                byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
                writeBuffer.writeInt(bytes.length);
                writeBuffer.writeBytes(bytes);
                return writeBuffer;
            }else {
                throw new FrameworkException(ExceptionType.NETWORK, "Require a string");
            }
        }
    }
}
