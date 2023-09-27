package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Context;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.log.LoggerConsumer;
import cn.zorcc.common.pojo.Loc;
import cn.zorcc.common.wheel.Wheel;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class EchoTest {
    private static final Logger log = new Logger(EchoTest.class);

    @Test
    public void testEchoClient() {
        Context.load(Wheel.wheel(), Wheel.class);
        Context.load(new LoggerConsumer(), LoggerConsumer.class);
        Net netClient = createEchoNetClient();
        Context.load(netClient, Net.class);
        Context.init();
        netClient.connect(new Loc("127.0.0.1", 8002), new EchoEncoder(), new EchoDecoder(), new EchoClientHandler(), new TcpConnector());
    }

    @Test
    public void testEchoServer() {
        Context.load(Wheel.wheel(), Wheel.class);
        Context.load(new LoggerConsumer(), LoggerConsumer.class);
        Net netServer = createEchoNetServer();
        Context.load(netServer, Net.class);
        Context.init();
    }

    private static Net createEchoNetClient() {
        NetworkConfig networkConfig = new NetworkConfig();
        Net net = new Net(networkConfig);
        MuxConfig muxConfig = new MuxConfig();
        net.addWorker(networkConfig, muxConfig);
        return net;
    }

    private static class EchoClientHandler implements Handler {
        private final AtomicInteger counter = new AtomicInteger(Constants.ZERO);
        @Override
        public void onConnected(Channel channel) {
            log.info("Client channel connected");
            Wheel.wheel().addPeriodicJob(() -> channel.send("Hello : " + counter.getAndIncrement()), 0, 1000, TimeUnit.MILLISECONDS);
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
            channel.send("Client going to shutdown");
        }

        @Override
        public void onRemoved(Channel channel) {
            log.info("Client channel removed");
        }
    }

    private static Net createEchoNetServer() {
        NetworkConfig networkConfig = new NetworkConfig();
        Net net = new Net(networkConfig);
        MuxConfig muxConfig = new MuxConfig();
        net.addMaster(EchoEncoder::new, EchoDecoder::new, EchoServerHandler::new, TcpConnector::new, new Loc("0.0.0.0", 8002), muxConfig);
        net.addWorker(networkConfig, muxConfig);
        return net;
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
                channel.send(str);
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
