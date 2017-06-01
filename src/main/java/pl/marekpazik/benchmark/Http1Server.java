package pl.marekpazik.benchmark;//package dzida.server.app.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.Collections;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpUtil.setContentLength;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class Http1Server {

    private static final int PORT = 8091;
    private static final int MAX_CONTENT_LENGTH = 1024 * 100;
    private final EventLoopGroup group;
    private Channel ch;

    public Http1Server() {
        this.group = new NioEventLoopGroup();
    }

    public void start() throws SSLException, InterruptedException, CertificateException {
        ServerBootstrap b = new ServerBootstrap();
        b.option(ChannelOption.SO_BACKLOG, 1024);
        b.group(group)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast("encoder", new HttpResponseEncoder())
                                .addLast("decoder", new HttpRequestDecoder())
                                .addLast("aggregator", new HttpObjectAggregator(MAX_CONTENT_LENGTH))
                                .addLast("handler", new RequestHandler());
                    }
                });

        ch = b.bind(PORT).sync().channel();
        System.err.println("Http1 server: http://127.0.0.1:" + PORT + '/');
    }

    public void waitUntilClosed() throws InterruptedException {
        ch.closeFuture().sync();
    }

    public void shootDown() {
        try {
            group.shutdownGracefully().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static class RequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
            if (req.method() == HttpMethod.OPTIONS) {
                sendOption(ctx);
                return;
            }
            ByteBuf content = copiedBuffer("Hello World", CharsetUtil.UTF_8);
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, content);
            response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            sendResponse(ctx, response);
        }

        private void sendResponse(ChannelHandlerContext ctx, FullHttpResponse response) {
            setContentLength(response, response.content().readableBytes());
            ctx.writeAndFlush(response);
        }

        private void sendOption(ChannelHandlerContext ctx) {
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            response.headers().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET");
            response.headers().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, Collections.singleton(HttpHeaderNames.CONTENT_TYPE));
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
