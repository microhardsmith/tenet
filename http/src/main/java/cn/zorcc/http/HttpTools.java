package cn.zorcc.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.nio.charset.StandardCharsets;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 *  http协议工具类,避免与Netty本身的HttpUtil重名
 */
public class HttpTools {
    private static final String FAILURE = "Failure: ";
    private static final String TEXT_TYPE = "text/plain; charset=UTF-8";

    private HttpTools() {
        throw new UnsupportedOperationException();
    }

    /**
     * 返回http状态消息
     * @param channel http通信channel
     * @param httpResponseStatus http状态码
     */
    public static void httpFail(Channel channel, HttpResponseStatus httpResponseStatus) {
        String msg =  FAILURE + httpResponseStatus.reasonPhrase();
        ByteBuf buf = Unpooled.wrappedBuffer(msg.getBytes(StandardCharsets.UTF_8));
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, httpResponseStatus, buf);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, TEXT_TYPE);
        channel.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }


}
