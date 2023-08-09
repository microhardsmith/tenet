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
import java.util.concurrent.atomic.AtomicInteger;

public class EchoClient {
    private static final Logger log = LoggerFactory.getLogger(EchoClient.class);
    private static final Loc SERVER_LOC = new Loc("127.0.0.1", 8002);

    public static void main(String[] args) {
        long nano = Clock.nano();
        Chain chain = Chain.chain();
        chain.add(Wheel.wheel());
        chain.add(new LoggerConsumer());
        Net net = createEchoNetClient();
        chain.add(net);
        chain.run();
        long jvmTime = ManagementFactory.getRuntimeMXBean().getUptime();
        log.info("Starting now, causing {} ms, JVM started for {} ms", TimeUnit.NANOSECONDS.toMillis(Clock.elapsed(nano)), jvmTime);
        net.connect(SERVER_LOC, new EchoEncoder(), new EchoDecoder(), new EchoClientHandler(), new TcpConnector());
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
            Wheel.wheel().addPeriodicJob(() -> {
                channel.send("Hello : " + counter.getAndIncrement());
            }, 0, 1000, TimeUnit.MILLISECONDS);
        }

        @Override
        public void onRecv(Channel channel, Object data) {
            if(data instanceof String str) {
                log.info("Client receiving msg : {}", str);
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
}
