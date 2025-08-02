package dev.kcterala.tunnelx.model;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;

import java.util.HashMap;
import java.util.Map;

public class TunnelRequest {
    private final String method;
    private final String path;
    private final Map<String, String> headers;
    private final byte[] body;
    
    public TunnelRequest(final String method, final String path, final HttpHeaders httpHeaders, final ByteBuf content) {
        this.method = method;
        this.path = path;
        this.headers = new HashMap<>();
        
        // Convert Netty headers to Map
        httpHeaders.forEach(entry -> 
            this.headers.put(entry.getKey(), entry.getValue()));
        
        // Convert ByteBuf to byte array
        this.body = new byte[content.readableBytes()];
        content.readBytes(this.body);
    }
    
    public String getMethod() { return method; }
    public String getPath() { return path; }
    public Map<String, String> getHeaders() { return headers; }
    public byte[] getBody() { return body; }
}