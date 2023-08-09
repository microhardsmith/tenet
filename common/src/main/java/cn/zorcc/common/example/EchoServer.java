package cn.zorcc.common.example;

import cn.zorcc.common.Chain;
import cn.zorcc.common.Clock;
import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.LoggerConsumer;
import cn.zorcc.common.network.*;
import cn.zorcc.common.pojo.Loc;
import cn.zorcc.common.wheel.Wheel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

public class EchoServer {
    private static final Logger log = LoggerFactory.getLogger(EchoServer.class);
    private static final Loc SERVER_LOC = new Loc("0.0.0.0", 8002);

    public static void main(String[] args) {
        long nano = Clock.nano();
        Chain chain = Chain.chain();
        chain.add(Wheel.wheel());
        chain.add(new LoggerConsumer());
        Net net = createEchoNetServer();
        chain.add(net);
        chain.run();
        long jvmTime = ManagementFactory.getRuntimeMXBean().getUptime();
        log.info("Starting now, causing {} ms, JVM started for {} ms", TimeUnit.NANOSECONDS.toMillis(Clock.elapsed(nano)), jvmTime);
    }

    private static Net createEchoNetServer() {
        NetworkConfig networkConfig = new NetworkConfig();
        Net net = new Net(networkConfig);
        MuxConfig muxConfig = new MuxConfig();
        net.addMaster(EchoEncoder::new, EchoDecoder::new, EchoServerHandler::new, TcpConnector::new, SERVER_LOC, muxConfig);
        net.addWorker(networkConfig, muxConfig);
        return net;
    }

    private static class EchoServerHandler implements Handler {
        @Override
        public void onConnected(Channel channel) {
            log.info("Receiving client connected from : {}", channel.loc());
        }

        @Override
        public void onRecv(Channel channel, Object data) {
            if(data instanceof String str) {
                log.info("Msg received : [{}]", str);
                channel.send(str);
            }else {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
        }

        @Override
        public void onShutdown(Channel channel) {
            log.info("Detecting channel shutdown : {}", channel.loc());
        }

        @Override
        public void onRemoved(Channel channel) {
            log.info("Detecting channel removed : {}", channel.loc());
        }
    }
}
