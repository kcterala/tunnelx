package dev.kcterala.tunnelx.tunnel;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kcterala.tunnelx.model.PendingRequest;
import dev.kcterala.tunnelx.model.TunnelMessage;
import dev.kcterala.tunnelx.model.TunnelRequest;
import dev.kcterala.tunnelx.model.TunnelResponse;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.UUID;
import java.util.function.Consumer;

public class TunnelConnection {
    private final String subDomain;
    private final Channel channel;
    private final ObjectMapper mapper = new ObjectMapper();

    public TunnelConnection(final String subDomain, final Channel channel) {
        this.subDomain = subDomain;
        this.channel = channel;
    }

    public String getSubDomain() { return subDomain; }
    public Channel getChannel() { return channel; }

    public String sendRequest(final TunnelRequest request) throws Exception {
        final String requestId = UUID.randomUUID().toString();

        // Create message to send to client
        final TunnelMessage message = new TunnelMessage();
        message.setType("request");
        message.setRequestId(requestId);
        message.setMethod(request.getMethod());
        message.setPath(request.getPath());
        message.setHeaders(request.getHeaders());
        message.setBody(request.getBody());

        final String json = mapper.writeValueAsString(message);
        channel.writeAndFlush(new TextWebSocketFrame(json));
        
        return requestId;
    }
}