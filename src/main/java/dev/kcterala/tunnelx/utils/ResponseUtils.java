package dev.kcterala.tunnelx.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kcterala.tunnelx.model.TunnelMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResponseUtils {

    public static ObjectMapper objectMapper = new ObjectMapper();
    public static Logger logger = LoggerFactory.getLogger(ResponseUtils.class);

    public static void sendMessage(final ChannelHandlerContext ctx, final TunnelMessage message) {
        try {
            final String json = objectMapper.writeValueAsString(message);
            ctx.writeAndFlush(new TextWebSocketFrame(json));
        } catch (final Exception e) {
            logger.error("Error sending message", e);
        }
    }

    public static void sendError(final ChannelHandlerContext ctx, final String error) {
        final TunnelMessage message = new TunnelMessage();
        message.setType("error");
        message.setError(error);
        sendMessage(ctx, message);
    }
}
