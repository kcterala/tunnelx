package dev.kcterala.tunnelx;

import dev.kcterala.tunnelx.tunnel.TunnelManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TunnelX {
    private static final Logger logger = LoggerFactory.getLogger(TunnelX.class);
    private final int port;
    private final TunnelManager tunnelManager;

    public TunnelX(final int port) {
        this.port = port;
        this.tunnelManager = new TunnelManager();
    }

    public void start() throws InterruptedException {
        final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        final EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            final ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ServerInitializer(tunnelManager));

            final Channel ch = b.bind(port).sync().channel();
            logger.info("Tunnel server started on port {}", port);

            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(final String[] args) throws InterruptedException {
        final int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        new TunnelX(port).start();
    }
}