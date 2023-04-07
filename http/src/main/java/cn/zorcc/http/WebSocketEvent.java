package cn.zorcc.http;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

public class WebSocketEvent {
    /**
     *  WebSocket数据帧
     */
    private TextWebSocketFrame textWebSocketFrame;
    /**
     *  WebSocket通信channel
     */
    private Channel channel;

    public TextWebSocketFrame textWebSocketFrame() {
        return textWebSocketFrame;
    }

    public void setTextWebSocketFrame(TextWebSocketFrame textWebSocketFrame) {
        this.textWebSocketFrame = textWebSocketFrame;
    }

    public Channel channel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }
}
