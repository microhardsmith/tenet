package cn.zorcc.log;

import cn.zorcc.common.network.Net;
import cn.zorcc.common.network.http.HttpCodec;
import cn.zorcc.common.util.ThreadUtil;
import cn.zorcc.common.wheel.Wheel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NetTest {
    public static void main(String[] args) {
        Wheel.wheel().init();
        LoggerConsumer loggerConsumer = new LoggerConsumer();
        loggerConsumer.init();
        Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(loggerConsumer::shutdown));
        Net net = new Net(HttpTestHandler::new, HttpCodec::new);
        Runtime.getRuntime().addShutdownHook(ThreadUtil.virtual("shutdown", net::shutdown));
        net.init();
        log.info("Start now");
    }
}
