package dev.kcterala.tunnelx.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kcterala.tunnelx.model.TunnelMessage;
import dev.kcterala.tunnelx.tunnel.TunnelConnection;
import dev.kcterala.tunnelx.tunnel.TunnelManager;
import dev.kcterala.tunnelx.utils.ResponseUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Environment variables with defaults
    private static final String DOMAIN = System.getenv("TUNNEL_DOMAIN") != null
            ? System.getenv("TUNNEL_DOMAIN")
            : "localhost";

    private static final String HTTP_SCHEME = System.getenv("TUNNEL_HTTP_SCHEME") != null
            ? System.getenv("TUNNEL_HTTP_SCHEME")
            : "http";

    private static final String PORT = System.getenv("TUNNEL_PORT") != null
            ? ":" + System.getenv("TUNNEL_PORT")
            : "";
    
    public WebSocketHandler() {
    }
    
    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final WebSocketFrame frame) {
        if (frame instanceof TextWebSocketFrame) {
            final String text = ((TextWebSocketFrame) frame).text();
            handleMessage(ctx, text);
        }
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        // Clean up tunnel when connection closes
        TunnelManager.getInstance().removeChannel(ctx.channel());
        super.channelInactive(ctx);
    }
    
    private void handleMessage(final ChannelHandlerContext ctx, final String message) {
        try {
            final TunnelMessage tunnelMessage = objectMapper.readValue(message, TunnelMessage.class);
            
            switch (tunnelMessage.getType()) {
                case "register" -> handleRegister(ctx, tunnelMessage);
                case "response" -> handleResponse(ctx, tunnelMessage);
                default -> logger.warn("Unknown message type: {}", tunnelMessage.getType());
            }
        } catch (final Exception e) {
            logger.error("Error handling message: {}", message, e);
        }
    }
    
    private void handleRegister(final ChannelHandlerContext ctx, final TunnelMessage message) {
        final String subdomain = message.getSubdomain();
        final String authToken = message.getAuthToken();

        if (!isValidAuthToken(authToken)) {
            ResponseUtils.sendError(ctx, "Invalid auth token");
            return;
        }
        
        // Register tunnel
        final TunnelConnection tunnel = new TunnelConnection(subdomain, ctx.channel());
        TunnelManager.getInstance().registerTunnel(subdomain, tunnel);
        
        logger.info("Registered tunnel for subdomain: {}", subdomain);
        
        // Send success response
        final TunnelMessage response = new TunnelMessage();
        response.setType("registered");
        response.setSubdomain(subdomain);
        response.setPublicUrl(HTTP_SCHEME + "://" + subdomain + "." + DOMAIN + PORT);
        
        ResponseUtils.sendMessage(ctx, response);
    }
    
    private void handleResponse(final ChannelHandlerContext ctx, final TunnelMessage message) {
        // Handle response from client back to original requester
        TunnelManager.getInstance().handleTunnelResponse(message);
    }
    
    private boolean isValidAuthToken(final String authToken) {
        final String staticAuthToken = System.getenv("staticAuthToken");
        return staticAuthToken.equals(authToken);
    }
    

}