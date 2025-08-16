package dev.kcterala.tunnelx.utils;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Paths;

public class StaticFileServer {
    private static final Logger logger = LoggerFactory.getLogger(StaticFileServer.class);
    private static final String STATIC_ROOT = "/static/";
    
    public static void serveStaticFile(ChannelHandlerContext ctx, String path) {
        // Default to index.html for root path or client-side routing
        if (path.equals("/") || !path.contains(".")) {
            path = "/index.html";
        }
        
        // Ensure path starts with /
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        
        String resourcePath = STATIC_ROOT + path.substring(1);
        
        try (InputStream inputStream = StaticFileServer.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                // File not found, serve index.html for client-side routing
                serveIndexHtml(ctx);
                return;
            }
            
            byte[] content = inputStream.readAllBytes();
            String contentType = getContentType(path);
            
            FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(content)
            );
            
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.length);
            response.headers().set(HttpHeaderNames.CACHE_CONTROL, "public, max-age=3600");
            
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            
        } catch (Exception e) {
            logger.error("Error serving static file: " + path, e);
            sendNotFound(ctx);
        }
    }
    
    private static void serveIndexHtml(ChannelHandlerContext ctx) {
        try (InputStream inputStream = StaticFileServer.class.getResourceAsStream(STATIC_ROOT + "index.html")) {
            if (inputStream == null) {
                sendNotFound(ctx);
                return;
            }
            
            byte[] content = inputStream.readAllBytes();
            
            FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(content)
            );
            
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.length);
            
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            
        } catch (Exception e) {
            logger.error("Error serving index.html", e);
            sendNotFound(ctx);
        }
    }
    
    private static String getContentType(String path) {
        String contentType = URLConnection.guessContentTypeFromName(path);
        if (contentType == null) {
            if (path.endsWith(".js")) {
                return "application/javascript; charset=UTF-8";
            } else if (path.endsWith(".css")) {
                return "text/css; charset=UTF-8";
            } else if (path.endsWith(".html")) {
                return "text/html; charset=UTF-8";
            } else if (path.endsWith(".json")) {
                return "application/json; charset=UTF-8";
            } else if (path.endsWith(".ico")) {
                return "image/x-icon";
            } else if (path.endsWith(".svg")) {
                return "image/svg+xml";
            } else if (path.endsWith(".woff2")) {
                return "font/woff2";
            } else if (path.endsWith(".woff")) {
                return "font/woff";
            } else if (path.endsWith(".ttf")) {
                return "font/ttf";
            }
            return "application/octet-stream";
        }
        return contentType;
    }
    
    private static void sendNotFound(ChannelHandlerContext ctx) {
        String content = "<!DOCTYPE html><html><head><title>404 Not Found</title></head>" +
                        "<body><h1>404 Not Found</h1><p>The requested resource was not found.</p></body></html>";
        
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.NOT_FOUND,
            Unpooled.copiedBuffer(content, CharsetUtil.UTF_8)
        );
        
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}