// HTTP Request Handler
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(HttpRequestHandler.class);
    private final TunnelManager tunnelManager;
    
    public HttpRequestHandler(final TunnelManager tunnelManager) {
        this.tunnelManager = tunnelManager;
    }
    
    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest request) {
        final String host = request.headers().get("Host");
        final String path = request.uri();
        
        logger.info("Received request: {} {} from host: {}", request.method(), path, host);
        
        // Check if this is a WebSocket upgrade request
        if (request.headers().get("Upgrade") != null && 
            request.headers().get("Upgrade").equalsIgnoreCase("websocket")) {
            // Let WebSocket handler deal with it
            ctx.fireChannelRead(request.retain());
            return;
        }
        
        // Extract subdomain for tunnel routing
        if (host != null && host.contains(".")) {
            final String subdomain = extractSubdomain(host);
            final TunnelConnection tunnel = tunnelManager.getTunnel(subdomain);
            
            if (tunnel != null) {
                // Forward request to tunnel
                forwardToTunnel(ctx, request, tunnel);
                return;
            }
        }
        
        // Default response for non-tunnel requests
        sendDefaultResponse(ctx, request);
    }
    
    private String extractSubdomain(final String host) {
        final String[] parts = host.split("\\.");
        return parts[0];
    }
    
    private void forwardToTunnel(final ChannelHandlerContext ctx, final FullHttpRequest request, final TunnelConnection tunnel) {
        // Create tunnel request object
        final TunnelRequest tunnelRequest = new TunnelRequest(
            request.method().name(),
            request.uri(),
            request.headers(),
            request.content().copy()
        );
        
        // Send to tunnel connection
        tunnel.forwardRequest(tunnelRequest, response -> {
            // Send response back to client
            final FullHttpResponse httpResponse = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.valueOf(response.getStatusCode()),
                Unpooled.wrappedBuffer(response.getBody())
            );
            
            // Copy headers
            response.getHeaders().forEach((key, value) -> 
                httpResponse.headers().set(key, value));
            
            ctx.writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE);
        });
    }
    
    private void sendDefaultResponse(final ChannelHandlerContext ctx, final FullHttpRequest request) {
        final String content = "<!DOCTYPE html>" +
                "<html><head><title>Tunnel Server</title></head>" +
                "<body><h1>Tunnel Server Running</h1>" +
                "<p>Active tunnels: " + tunnelManager.getActiveTunnelCount() + "</p>" +
                "</body></html>";
        
        final FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK,
            Unpooled.copiedBuffer(content, CharsetUtil.UTF_8)
        );
        
        response.headers().set("Content-Type", "text/html; charset=UTF-8");
        response.headers().set("Content-Length", response.content().readableBytes());
        
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}