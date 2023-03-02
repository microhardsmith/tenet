package cn.zorcc.http;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;
import java.io.File;

/**
 *  Http服务器(http1.1版本,http2主要在请求静态资源上有较大提升,在动态请求上差距不大)
 *  Http服务器是独立于net通用服务器的实现,通过触发HttpEvent来实现业务逻辑
 */
@Slf4j
public class HttpServer {
    private final SslContext sslContext;
    private final HttpServerHandler httpServerHandler;

    public HttpServer(HttpConfig httpConfig) {
        this.httpServerHandler = new HttpServerHandler(httpConfig.isEnableWebSocket());
        boolean isEpollAvailable = Epoll.isAvailable();
        int httpWorkerThreads = httpConfig.getHttpServerThreads();
        EventLoopGroup bossGroup;
        EventLoopGroup workerGroup;
        if (isEpollAvailable) {
            log.info("Http server epoll available, Using Epoll instead of Java NIO selector");
            bossGroup = new EpollEventLoopGroup(1);
            workerGroup = httpWorkerThreads <= 0 ? bossGroup : new EpollEventLoopGroup(httpWorkerThreads);
        } else {
            log.info("Http server epoll unavailable, Using Java NIO selector, Cause : {}", Epoll.unavailabilityCause().getCause().getMessage());
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = httpWorkerThreads <= 0 ? bossGroup : new NioEventLoopGroup(httpWorkerThreads);
        }
        if (httpConfig.isUsingHttps()) {
            try {
                log.info("Loading Https server certificate");
                String httpSslCertFile = httpConfig.getHttpSslCertFile();
                String httpSslKeyFile = httpConfig.getHttpSslKeyFile();
                if (!httpSslCertFile.isBlank() && !httpSslKeyFile.isBlank()) {
                    SslContextBuilder sslContextBuilder = SslContextBuilder.forServer(new File(httpSslCertFile), new File(httpSslKeyFile));
                    String httpCaCertFile = httpConfig.getHttpCaCertFile();
                    if (!httpCaCertFile.isBlank()) {
                        sslContextBuilder.trustManager(new File(httpCaCertFile));
                    }
                    this.sslContext = sslContextBuilder.build();
                } else {
                    throw new FrameworkException(ExceptionType.HTTP, "SSL certificate not found");
                }
            } catch (SSLException e) {
                throw new FrameworkException(ExceptionType.HTTP, "Err loading SSL configuration");
            }
        } else {
            this.sslContext = null;
        }
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(isEpollAvailable ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO)) // 打印netty本身的日志
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        if (sslContext != null) {
                            pipeline.addLast(Constants.HTTP_SSL, sslContext.newHandler(socketChannel.alloc()));
                        }
                        // http编解码器
                        pipeline.addLast(Constants.HTTP_CODEC, new HttpServerCodec());
                        // http消息体压缩
                        pipeline.addLast(Constants.HTTP_COMPRESSOR, new HttpContentCompressor());
                        // 多个消息聚合成为单一的FullHttpRequest或FullHttpResponse
                        pipeline.addLast(Constants.HTTP_AGGREGATOR, new HttpObjectAggregator(512 * 1024));
                        // 支持异步发送大的码流
                        pipeline.addLast(Constants.HTTP_CHUNKED, new ChunkedWriteHandler());
                        // Http请求处理器
                        pipeline.addLast(Constants.HTTP_HANDLER, httpServerHandler);
                    }
                })
                //表示系统用于临时存放已完成三次握手的请求的队列的最大长度,如果连接建立频繁,服务器处理创建新连接较慢,可以适当调大这个参数
                .option(ChannelOption.SO_BACKLOG, 128)
                //是否可复用端口地址
                .option(ChannelOption.SO_REUSEADDR, true)
                // TCP默认开启了 Nagle 算法,该算法的作用是尽可能的将小包累计起来发送大数据块,减少网络传输。TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法
                .childOption(ChannelOption.TCP_NODELAY, true)
                // 是否开启 TCP 底层心跳机制,这里不推荐开启,用应用层心跳来检测连接是否断开
                .childOption(ChannelOption.SO_KEEPALIVE, false);
        int httpPort = httpConfig.getPort();
        serverBootstrap.bind(httpPort).syncUninterruptibly();
        log.info("Http server is now listening on port: [{}]", httpPort);
    }
}
