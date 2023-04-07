package cn.zorcc.http;

import cn.zorcc.common.Context;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.event.EventHandler;
import cn.zorcc.common.exception.FrameworkException;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  http处理器
 */
@Slf4j
@ChannelHandler.Sharable
public class HttpServerHandler extends SimpleChannelInboundHandler<Object> {
    private static final String WS = "ws://";
    private static final String UPGRADE = "Upgrade";
    private static final String WEBSOCKET = "websocket";
    private final boolean enableWebSocket;
    private final WebSocketServerHandshakerFactory wsFactory;
    private final Map<Channel, WebSocketServerHandshaker> shakerMap;
    private final EventHandler<HttpEvent> httpHandler;
    private final EventHandler<WebSocketEvent> websocketPipeline;

    public HttpServerHandler(boolean enableWebSocket) {
        this.enableWebSocket = enableWebSocket;
        this.httpHandler = Context.handler(HttpEvent.class);
        if(enableWebSocket) {
            this.wsFactory = new WebSocketServerHandshakerFactory(WS + Context.self().loc(), null, false);
            this.shakerMap = new ConcurrentHashMap<>();
            this.websocketPipeline = Context.handler(WebSocketEvent.class);
        }else {
            this.wsFactory = null;
            this.shakerMap = null;
            this.websocketPipeline = null;
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object obj) {
        switch (obj) {
            case FullHttpRequest fullHttpRequest -> handleHttpRequest(ctx, fullHttpRequest);
            case WebSocketFrame webSocketFrame -> handleWebSocketRequest(ctx, webSocketFrame);
            case null, default -> throw new FrameworkException(ExceptionType.HTTP, "Unexpected value: " + obj);
        }
    }

    /**
     *  处理Http请求
     */
    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) {
        Channel channel = ctx.channel();
        if (!fullHttpRequest.decoderResult().isSuccess()) {
            // 报文解析错误
            HttpTools.httpFail(channel, HttpResponseStatus.BAD_REQUEST);
        }else {
            if(enableWebSocket && WEBSOCKET.equals(fullHttpRequest.headers().get(UPGRADE))) {
                // websocket升级报文
                WebSocketServerHandshaker shaker = wsFactory.newHandshaker(fullHttpRequest);
                if (shaker == null) {
                    // websocket协议不匹配
                    WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(channel);
                } else {
                    shaker.handshake(channel, fullHttpRequest);
                    shakerMap.put(channel, shaker);
                }
            }else {
                // 触发HttpEvent事件
                HttpEvent httpEvent = new HttpEvent();
                httpEvent.setFullHttpRequest(fullHttpRequest);
                httpEvent.setChannel(channel);
                httpHandler.handle(httpEvent);
            }
        }
    }

    /**
     *  处理Websocket请求
     */
    private void handleWebSocketRequest(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if(enableWebSocket) {
            Channel channel = ctx.channel();
            switch (frame) {
                case CloseWebSocketFrame closeWebSocketFrame -> shakerMap.remove(channel).close(channel, closeWebSocketFrame.retain());
                case PingWebSocketFrame pingWebSocketFrame -> channel.writeAndFlush(new PongWebSocketFrame(pingWebSocketFrame.content().retain()));
                case TextWebSocketFrame textWebSocketFrame -> {
                    // 触发WebSocketEvent事件
                    WebSocketEvent webSocketEvent = new WebSocketEvent();
                    webSocketEvent.setTextWebSocketFrame(textWebSocketFrame);
                    webSocketEvent.setChannel(channel);
                    websocketPipeline.handle(webSocketEvent);
                }
                case null, default -> throw new FrameworkException(ExceptionType.HTTP, "Unsupported frame type");
            }
        }else {
            throw new FrameworkException(ExceptionType.HTTP, "WebSocket protocol not enabled");
        }
    }

}
