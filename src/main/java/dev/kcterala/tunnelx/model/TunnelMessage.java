package dev.kcterala.tunnelx.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TunnelMessage {
    private String type;
    private String subdomain;
    private String authToken;
    private String publicUrl;
    private String error;
    private String requestId;
    private String method;
    private String path;
    private Map<String, String> headers;
    private byte[] body;
    private int statusCode;
    
    // Getters and Setters
    public String getType() { return type; }
    public void setType(final String type) { this.type = type; }
    
    public String getSubdomain() { return subdomain; }
    public void setSubdomain(final String subdomain) { this.subdomain = subdomain; }
    
    public String getAuthToken() { return authToken; }
    public void setAuthToken(final String authToken) { this.authToken = authToken; }
    
    public String getPublicUrl() { return publicUrl; }
    public void setPublicUrl(final String publicUrl) { this.publicUrl = publicUrl; }
    
    public String getError() { return error; }
    public void setError(final String error) { this.error = error; }
    
    public String getRequestId() { return requestId; }
    public void setRequestId(final String requestId) { this.requestId = requestId; }
    
    public String getMethod() { return method; }
    public void setMethod(final String method) { this.method = method; }
    
    public String getPath() { return path; }
    public void setPath(final String path) { this.path = path; }
    
    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(final Map<String, String> headers) { this.headers = headers; }
    
    public byte[] getBody() { return body; }
    public void setBody(final byte[] body) { this.body = body; }
    
    public int getStatusCode() { return statusCode; }
    public void setStatusCode(final int statusCode) { this.statusCode = statusCode; }
}