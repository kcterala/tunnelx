import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TunnelWebSocketHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    private static final Logger logger = LoggerFactory.getLogger(TunnelWebSocketHandler.class);
    private final TunnelManager tunnelManager;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public TunnelWebSocketHandler(final TunnelManager tunnelManager) {
        this.tunnelManager = tunnelManager;
    }
    
    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final WebSocketFrame frame) {
        if (frame instanceof TextWebSocketFrame) {
            final String text = ((TextWebSocketFrame) frame).text();
            handleMessage(ctx, text);
        }
    }
    
    private void handleMessage(final ChannelHandlerContext ctx, final String message) {
        try {
            final TunnelMessage tunnelMessage = objectMapper.readValue(message, TunnelMessage.class);
            
            switch (tunnelMessage.getType()) {
                case "register":
                    handleRegister(ctx, tunnelMessage);
                    break;
                case "response":
                    handleResponse(ctx, tunnelMessage);
                    break;
                default:
                    logger.warn("Unknown message type: {}", tunnelMessage.getType());
            }
        } catch (final Exception e) {
            logger.error("Error handling message: {}", message, e);
        }
    }
    
    private void handleRegister(final ChannelHandlerContext ctx, final TunnelMessage message) {
        final String subdomain = message.getSubdomain();
        final String authToken = message.getAuthToken();
        
        // Validate auth token (implement your auth logic)
        if (!isValidAuthToken(authToken)) {
            sendError(ctx, "Invalid auth token");
            return;
        }
        
        // Register tunnel
        final TunnelConnection tunnel = new TunnelConnection(subdomain, ctx.channel(), tunnelManager);
        tunnelManager.registerTunnel(subdomain, tunnel);
        
        logger.info("Registered tunnel for subdomain: {}", subdomain);
        
        // Send success response
        final TunnelMessage response = new TunnelMessage();
        response.setType("registered");
        response.setSubdomain(subdomain);
        response.setPublicUrl("http://" + subdomain + ".localhost:8080");
        
        sendMessage(ctx, response);
    }
    
    private void handleResponse(final ChannelHandlerContext ctx, final TunnelMessage message) {
        // Handle response from client back to original requester
        tunnelManager.handleTunnelResponse(message);
    }
    
    private boolean isValidAuthToken(final String authToken) {
        // Implement your authentication logic
        return authToken != null && authToken.length() > 10;
    }
    
    private void sendMessage(final ChannelHandlerContext ctx, final TunnelMessage message) {
        try {
            final String json = objectMapper.writeValueAsString(message);
            ctx.writeAndFlush(new TextWebSocketFrame(json));
        } catch (final Exception e) {
            logger.error("Error sending message", e);
        }
    }
    
    private void sendError(final ChannelHandlerContext ctx, final String error) {
        final TunnelMessage message = new TunnelMessage();
        message.setType("error");
        message.setError(error);
        sendMessage(ctx, message);
    }
    
    @Override
    public void channelInactive(final ChannelHandlerContext ctx) {
        // Clean up tunnel when connection closes
        tunnelManager.removeChannel(ctx.channel());
        try {
            super.channelInactive(ctx);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}