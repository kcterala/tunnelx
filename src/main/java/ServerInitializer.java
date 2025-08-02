import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

public class ServerInitializer extends ChannelInitializer<SocketChannel> {
    private final TunnelManager tunnelManager;
    
    public ServerInitializer(final TunnelManager tunnelManager) {
        this.tunnelManager = tunnelManager;
    }
    
    @Override
    protected void initChannel(final SocketChannel ch) {
        final ChannelPipeline pipeline = ch.pipeline();
        
        // HTTP codec
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(1048576));
        pipeline.addLast(new ChunkedWriteHandler());
        
        // Custom handler for routing
        pipeline.addLast(new HttpRequestHandler(tunnelManager));
        
        // WebSocket handler for tunnel connections
        pipeline.addLast(new WebSocketServerProtocolHandler("/tunnel", null, true, 1048576));
        pipeline.addLast(new TunnelWebSocketHandler(tunnelManager));
    }
}