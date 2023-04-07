package cn.zorcc.http;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 *  Http服务器接受请求事件
 */
public class HttpEvent {
    /**
     *  Http请求体
     */
    private FullHttpRequest fullHttpRequest;
    /**
     *  Http通信Channel
     */
    private Channel channel;

    public FullHttpRequest fullHttpRequest() {
        return fullHttpRequest;
    }

    public void setFullHttpRequest(FullHttpRequest fullHttpRequest) {
        this.fullHttpRequest = fullHttpRequest;
    }

    public Channel channel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }
}
