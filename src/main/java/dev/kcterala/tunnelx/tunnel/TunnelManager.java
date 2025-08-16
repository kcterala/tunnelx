package dev.kcterala.tunnelx.tunnel;

import dev.kcterala.tunnelx.model.PendingRequest;
import dev.kcterala.tunnelx.model.TunnelMessage;
import dev.kcterala.tunnelx.model.TunnelResponse;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class TunnelManager {
    private static final Logger log = LoggerFactory.getLogger(TunnelManager.class);
    private final Map<String, TunnelConnection> tunnels = new HashMap<>();
    private final Map<String, PendingRequest> pendingRequests = new HashMap<>();
    private final long startTime = System.currentTimeMillis();
    private final AtomicLong totalTunnelCount = new AtomicLong(0);


    public void registerTunnel(final String subdomain, final TunnelConnection tunnel) {
        tunnels.put(subdomain, tunnel);
        totalTunnelCount.incrementAndGet();
        log.info("Tunnel registered: {}", subdomain);
        // Push update to dashboard
        notifyDashboard();
    }

    public TunnelConnection getTunnel(final String subdomain) {
        return tunnels.get(subdomain);
    }

    public void removeChannel(final Channel channel) {
        boolean removed = tunnels.entrySet().removeIf(entry -> {
            if (entry.getValue().getChannel().equals(channel)) {
                log.info("Removing tunnel for closed channel: {}", entry.getKey());
                return true;
            }
            return false;
        });
        if (removed) {
            // Push update to dashboard
            notifyDashboard();
        }
    }

    public int getActiveTunnelCount() {
        return tunnels.size();
    }

    public void handleTunnelResponse(final TunnelMessage message) {
        final String requestId = message.getRequestId();
        final PendingRequest pending = pendingRequests.remove(requestId);

        if (pending != null) {
            final TunnelResponse response = new TunnelResponse(
                    message.getStatusCode(),
                    message.getHeaders(),
                    message.getBody()
            );
            pending.getCallback().accept(response);
        }
    }

    public void addPendingRequest(final String requestId, final PendingRequest request) {
        pendingRequests.put(requestId, request);
    }

    public void removePendingRequest(final String requestId) {
        pendingRequests.remove(requestId);
    }

    public long getTotalTunnelCount() {
        return totalTunnelCount.get();
    }

    public long getUptimeSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }


    public boolean isSubdomainTaken(final String subdomain) {
        return tunnels.containsKey(subdomain);
    }

    public Map<String, TunnelConnection> getActiveTunnels() {
        return new HashMap<>(tunnels);
    }

    private void notifyDashboard() {
        try {
            // Use reflection to avoid circular dependency
            Class<?> sseManagerClass = Class.forName("dev.kcterala.tunnelx.utils.SSEManager");
            java.lang.reflect.Method broadcastMethod = sseManagerClass.getMethod("broadcastStats", TunnelManager.class);
            broadcastMethod.invoke(null, this);
        } catch (Exception e) {
            // Silently ignore if SSEManager is not available
        }
    }

}