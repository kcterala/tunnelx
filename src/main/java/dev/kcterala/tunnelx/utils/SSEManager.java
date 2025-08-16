package dev.kcterala.tunnelx.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.kcterala.tunnelx.tunnel.TunnelManager;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SSEManager {
    private static final Logger logger = LoggerFactory.getLogger(SSEManager.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Set<ChannelHandlerContext> sseClients = ConcurrentHashMap.newKeySet();
    private static final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
    
    static {
        // Send heartbeat every 30 seconds to keep connections alive
        heartbeatExecutor.scheduleWithFixedDelay(SSEManager::sendHeartbeat, 30, 30, TimeUnit.SECONDS);
    }
    
    public static void addClient(ChannelHandlerContext ctx) {
        sseClients.add(ctx);
        logger.info("SSE client connected: {}", ctx.channel().remoteAddress());
        
        // Send initial SSE headers
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/event-stream");
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "no-cache");
        response.headers().set(HttpHeaderNames.CONNECTION, "keep-alive");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        
        ctx.write(response);
        ctx.flush();
    }
    
    public static void removeClient(ChannelHandlerContext ctx) {
        sseClients.remove(ctx);
        logger.info("SSE client disconnected: {}", ctx.channel().remoteAddress());
    }
    
    public static void broadcastStats(TunnelManager tunnelManager) {
        if (sseClients.isEmpty()) return;
        
        try {
            ObjectNode stats = objectMapper.createObjectNode();
            stats.put("totalTunnels", tunnelManager.getTotalTunnelCount());
            stats.put("activeTunnels", tunnelManager.getActiveTunnelCount());
            stats.put("uptime", tunnelManager.getUptimeSeconds());
            
            // Add tunnel details
            var tunnelsArray = stats.putArray("tunnels");
            tunnelManager.getActiveTunnels().forEach((subdomain, tunnel) -> {
                ObjectNode tunnelInfo = objectMapper.createObjectNode();
                tunnelInfo.put("subdomain", subdomain);
                tunnelInfo.put("connectedTime", formatTime(tunnel.getConnectedTime()));
                tunnelsArray.add(tunnelInfo);
            });
            
            String data = "data: " + objectMapper.writeValueAsString(stats) + "\n\n";
            broadcastToAll(data);
        } catch (Exception e) {
            logger.error("Error broadcasting stats", e);
        }
    }
    
    private static void broadcastToAll(String data) {
        sseClients.removeIf(ctx -> {
            if (!ctx.channel().isActive()) {
                return true; // Remove inactive clients
            }
            
            try {
                ctx.writeAndFlush(Unpooled.copiedBuffer(data, CharsetUtil.UTF_8));
                return false; // Keep active clients
            } catch (Exception e) {
                logger.warn("Failed to send SSE data to client", e);
                return true; // Remove clients that failed
            }
        });
    }
    
    private static void sendHeartbeat() {
        if (sseClients.isEmpty()) return;
        
        // Send SSE comment as heartbeat (comments are ignored by browser but keep connection alive)
        String heartbeat = ": heartbeat\n\n";
        broadcastToAll(heartbeat);
    }
    
    private static String formatTime(long timestamp) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm:ss")
                .withZone(ZoneId.systemDefault());
        return formatter.format(Instant.ofEpochMilli(timestamp));
    }
}