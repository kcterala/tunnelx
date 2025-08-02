import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.UUID;
import java.util.function.Consumer;

public class TunnelConnection {
    private final String subDomain;
    private final Channel channel;
    private final TunnelManager tunnelManager;
    private final ObjectMapper mapper = new ObjectMapper();

    public TunnelConnection(final String subDomain, final Channel channel, final TunnelManager tunnelManager) {
        this.subDomain = subDomain;
        this.channel = channel;
        this.tunnelManager = tunnelManager;
    }

    public String getSubDomain() { return subDomain; }
    public Channel getChannel() { return channel; }

    public void forwardRequest(final TunnelRequest request, final Consumer<TunnelResponse> callback) {
        final String requestId = UUID.randomUUID().toString();

        // Create message to send to client
        final TunnelMessage message = new TunnelMessage();
        message.setType("request");
        message.setRequestId(requestId);
        message.setMethod(request.getMethod());
        message.setPath(request.getPath());
        message.setHeaders(request.getHeaders());
        message.setBody(request.getBody());

        try {
            final String json = mapper.writeValueAsString(message);
            channel.writeAndFlush(new TextWebSocketFrame(json));

            // Store callback for when response comes back
            // This would typically be handled by TunnelManager
            final PendingRequest pending = new PendingRequest(callback);
             tunnelManager.addPendingRequest(requestId, pending);

        } catch (final Exception e) {
            // Handle error
            tunnelManager.removePendingRequest(requestId);
            final TunnelResponse errorResponse = new TunnelResponse(500, null, "Internal server error".getBytes());
            callback.accept(errorResponse);
        }
    }
}
