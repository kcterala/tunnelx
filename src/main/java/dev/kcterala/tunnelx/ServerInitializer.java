package dev.kcterala.tunnelx;

import dev.kcterala.tunnelx.handler.HttpRequestHandler;
import dev.kcterala.tunnelx.handler.WebSocketHandler;
import dev.kcterala.tunnelx.tunnel.TunnelManager;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

public class ServerInitializer extends ChannelInitializer<SocketChannel> {
    /** Maximum payload size (8 MiB) allowed for HTTP aggregation and WebSocket frames. */
    private static final int MAX_MESSAGE_SIZE_BYTES = 8 * 1024 * 1024;
    
    public ServerInitializer() {
    }
    
    @Override
    protected void initChannel(final SocketChannel ch) {
        final ChannelPipeline pipeline = ch.pipeline();
        
        // HTTP codec
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(MAX_MESSAGE_SIZE_BYTES));
        pipeline.addLast(new ChunkedWriteHandler());
        
        // Custom handler for routing
        pipeline.addLast(new HttpRequestHandler());
        
        // WebSocket handler for tunnel connections
        pipeline.addLast(new WebSocketServerProtocolHandler("/tunnel", null, true, MAX_MESSAGE_SIZE_BYTES));
        pipeline.addLast(new WebSocketHandler());
    }
}
