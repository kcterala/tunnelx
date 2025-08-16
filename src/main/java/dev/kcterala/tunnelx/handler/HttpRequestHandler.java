package dev.kcterala.tunnelx.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.kcterala.tunnelx.model.TunnelRequest;
import dev.kcterala.tunnelx.tunnel.TunnelConnection;
import dev.kcterala.tunnelx.tunnel.TunnelManager;
import dev.kcterala.tunnelx.utils.SSEManager;
import dev.kcterala.tunnelx.utils.StaticFileServer;
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
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public HttpRequestHandler(final TunnelManager tunnelManager) {
        this.tunnelManager = tunnelManager;
    }
    
    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        SSEManager.removeClient(ctx);
        super.channelInactive(ctx);
    }
    
    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest request) {
        final String host = request.headers().get("Host");
        final String path = request.uri();
        
        logger.info("Received request: {} {} from host: {}", request.method(), path, host);
        
        // Health check endpoint
        if ("/ping".equals(path)) {
            sendPongResponse(ctx);
            return;
        }
        
        // Server-Sent Events endpoint for real-time updates
        if ("/events".equals(path)) {
            SSEManager.addClient(ctx);
            // Send initial stats
            SSEManager.broadcastStats(tunnelManager);
            return;
        }
        
        // API endpoints for dashboard
        if ("/api/stats".equals(path)) {
            sendStatsResponse(ctx);
            return;
        }
        
        if (path.startsWith("/api/check/")) {
            final String subdomain = path.substring("/api/check/".length());
            sendAvailabilityResponse(ctx, subdomain);
            return;
        }
        
        // Check if this is a WebSocket upgrade request
        if (request.headers().get("Upgrade") != null && 
            request.headers().get("Upgrade").equalsIgnoreCase("websocket")) {
            // Let WebSocket routing handler deal with it
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
        
        // Check if this is the main domain (no subdomain) - serve static files
        if (host != null && isMainDomain(host)) {
            StaticFileServer.serveStaticFile(ctx, path);
            return;
        }
        
        // Default response for non-tunnel requests
        sendDefaultResponse(ctx, request);
    }
    
    private String extractSubdomain(final String host) {
        final String[] parts = host.split("\\.");
        return parts[0];
    }
    
    private boolean isMainDomain(final String host) {
        // Check if host is the main domain (no subdomain)
        // For tunnel.name.dev, this would be exactly "tunnel.name.dev"
        // For localhost:8080, this would be exactly "localhost:8080"
        final String[] parts = host.split("\\.");
        return parts.length <= 2 || 
               (parts.length == 3 && (host.contains("localhost") || host.equals("tunnel")));
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
    
    private void sendPongResponse(final ChannelHandlerContext ctx) {
        final String content = "pong";
        
        final FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK,
            Unpooled.copiedBuffer(content, CharsetUtil.UTF_8)
        );
        
        response.headers().set("Content-Type", "text/plain; charset=UTF-8");
        response.headers().set("Content-Length", response.content().readableBytes());
        
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
    
    private void sendStatsResponse(final ChannelHandlerContext ctx) {
        try {
            final ObjectNode stats = objectMapper.createObjectNode();
            stats.put("totalTunnels", tunnelManager.getTotalTunnelCount());
            stats.put("activeTunnels", tunnelManager.getActiveTunnelCount());
            stats.put("uptime", tunnelManager.getUptimeSeconds());
            
            // Add tunnel details
            final var tunnelsArray = stats.putArray("tunnels");
            tunnelManager.getActiveTunnels().forEach((subdomain, tunnel) -> {
                final ObjectNode tunnelInfo = objectMapper.createObjectNode();
                tunnelInfo.put("subdomain", subdomain);
                tunnelInfo.put("connectedTime", formatTime(tunnel.getConnectedTime()));
                tunnelsArray.add(tunnelInfo);
            });
            
            sendJsonResponse(ctx, stats.toString());
        } catch (final Exception e) {
            logger.error("Error sending stats", e);
            sendJsonResponse(ctx, "{\"error\":\"Failed to get stats\"}");
        }
    }
    
    private void sendAvailabilityResponse(final ChannelHandlerContext ctx, final String subdomain) {
        try {
            final ObjectNode response = objectMapper.createObjectNode();
            response.put("subdomain", subdomain);
            response.put("available", !tunnelManager.isSubdomainTaken(subdomain));
            
            sendJsonResponse(ctx, response.toString());
        } catch (final Exception e) {
            logger.error("Error checking availability", e);
            sendJsonResponse(ctx, "{\"error\":\"Failed to check availability\"}");
        }
    }
    
    private void sendJsonResponse(final ChannelHandlerContext ctx, final String json) {
        final FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK,
            Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
        );
        
        response.headers().set("Content-Type", "application/json; charset=UTF-8");
        response.headers().set("Content-Length", response.content().readableBytes());
        response.headers().set("Access-Control-Allow-Origin", "*");
        
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
    
    private String formatTime(final long timestamp) {
        final java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, HH:mm:ss")
                .withZone(java.time.ZoneId.systemDefault());
        return formatter.format(java.time.Instant.ofEpochMilli(timestamp));
    }
}