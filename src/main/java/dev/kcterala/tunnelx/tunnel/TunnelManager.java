package dev.kcterala.tunnelx.tunnel;

import dev.kcterala.tunnelx.model.PendingRequest;
import dev.kcterala.tunnelx.model.TunnelMessage;
import dev.kcterala.tunnelx.model.TunnelResponse;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class TunnelManager {
    private static final Logger log = LoggerFactory.getLogger(TunnelManager.class);
    private final Map<String, TunnelConnection> tunnels = new HashMap<>();
    private final Map<String, PendingRequest> pendingRequests = new HashMap<>();


    public void registerTunnel(final String subdomain, final TunnelConnection tunnel) {
        tunnels.put(subdomain, tunnel);
        log.info("Tunnel registered: {}", subdomain);
    }

    public TunnelConnection getTunnel(final String subdomain) {
        return tunnels.get(subdomain);
    }

    public void removeChannel(final Channel channel) {
        tunnels.entrySet().removeIf(entry -> {
            if (entry.getValue().getChannel().equals(channel)) {
                log.info("Removing tunnel for closed channel: {}", entry.getKey());
                return true;
            }
            return false;
        });
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

}