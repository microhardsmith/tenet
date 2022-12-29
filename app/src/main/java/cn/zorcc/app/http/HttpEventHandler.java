package cn.zorcc.app.http;

import cn.zorcc.common.Context;
import cn.zorcc.common.event.EventHandler;
import cn.zorcc.common.event.EventPipeline;
import cn.zorcc.common.util.ThreadUtil;
import cn.zorcc.http.HttpEvent;
import cn.zorcc.http.HttpReq;

import java.util.concurrent.atomic.AtomicInteger;

/**
 *  App默认使用的HttpEvent处理器,将其转化为AppHttpEvent后在虚拟线程中执行
 */
public class HttpEventHandler implements EventHandler<HttpEvent> {
    private static final String name = "http-";
    private static final AtomicInteger counter = new AtomicInteger(0);
    private EventPipeline<AppHttpEvent> pipeline;
    @Override
    public void init() {
        this.pipeline = Context.pipeline(AppHttpEvent.class);
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void handle(HttpEvent event) {
        final AppHttpEvent appHttpEvent = new AppHttpEvent();
        appHttpEvent.setHttpReq(HttpReq.from(event.fullHttpRequest()));
        appHttpEvent.setChannel(event.channel());
        ThreadUtil.virtual(name + counter.getAndIncrement(), () -> pipeline.fireEvent(appHttpEvent));
    }
}
