import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Values.KEEP_ALIVE;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
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
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;


public class TestServer {

    public static void main(String[] args) throws Exception {
        new TestServer(4000).start();
    }

    private final int port;
    private final int availableProcessors = Runtime.getRuntime().availableProcessors();

    public TestServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        getServerBootstrap().bind(port).sync();
        System.out.printf("Netty Server started at port [%s]\n", port);
    }

    public ServerBootstrap getServerBootstrap() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        final EventLoopGroup workerGroup = new NioEventLoopGroup(availableProcessors);

        bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new HttpServerCodec());
                        ch.pipeline().addLast(new HttpObjectAggregator(1048576));
                        ch.pipeline().addLast(new EchoHandler());
                    }
                });

        bootstrap.childOption(ChannelOption.SO_LINGER, 1000);
        bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

        return bootstrap;
    }

    public static class EchoHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        @Override
        public void channelRead0(final ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
            if (ctx.channel().isActive()) {
                FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                        req.content().copy());
                HttpHeaders.setIntHeader(response, CONTENT_LENGTH, req.content().readableBytes());
                HttpHeaders.setHeader(response, CONNECTION, KEEP_ALIVE);
                ctx.channel().write(response);
                ctx.flush();
            }
        }
    }
}