package dev.kcterala.tunnelx.model;

import java.util.function.Consumer;

public class PendingRequest {
    private final Consumer<TunnelResponse> callback;
    private final long timestamp;
    
    public PendingRequest(final Consumer<TunnelResponse> callback) {
        this.callback = callback;
        this.timestamp = System.currentTimeMillis();
    }
    
    public Consumer<TunnelResponse> getCallback() { return callback; }
    public long getTimestamp() { return timestamp; }
}