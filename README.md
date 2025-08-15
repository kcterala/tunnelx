# Tunnel Server

A server that creates HTTP tunnels with custom subdomains for local development.

## Overview

This tunnel server allows you to expose local services to the internet with custom subdomains. It uses WebSockets to establish a tunnel connection and forwards HTTP requests to the local service.

## Deployment

### Environment Variables

The following environment variables can be configured for deployment:

| Variable | Description | Default Value |
|----------|-------------|---------------|
| `TUNNEL_DOMAIN` | The domain name for the tunnel server | `localhost` |
| `TUNNEL_HTTP_SCHEME` | The HTTP scheme (http or https) | `http` |
| `TUNNEL_PORT` | The port number for the tunnel server | `8080` |
| `staticAuthToken` | Authentication token for tunnel registration | *Required* |

### Docker Deployment

```bash
# Build the Docker image
docker build -t tunnel-server .

# Run the Docker container with default settings
docker run -p 8080:8080 tunnel-server

# Run with custom environment variables
docker run -p 8080:8080 \
  -e TUNNEL_DOMAIN=yourdomain.com \
  -e TUNNEL_HTTP_SCHEME=https \
  -e TUNNEL_PORT=443 \
  -e staticAuthToken=your-secure-token \
  tunnel-server
```

### Manual Deployment

```bash
# Set environment variables
export TUNNEL_DOMAIN="yourdomain.com"
export TUNNEL_HTTP_SCHEME="https"
export TUNNEL_PORT="443"
export staticAuthToken="your-secure-token"

# Run the server
java -cp target/classes dev.kcterala.tunnelx.TunnelX
```

## Building from Source

```bash
# Clone the repository
git clone https://github.com/yourusername/tunnel-server.git
cd tunnel-server

# Build with Maven
mvn clean package

# Run the server
java -cp target/classes dev.kcterala.tunnelx.TunnelX
```

## How It Works

### Architecture Overview

The tunnel server uses a singleton `TunnelManager` to coordinate all tunnel operations. Here's the complete flow:

```
Client (Local Service) ←→ WebSocket ←→ Tunnel Server ←→ HTTP ←→ End User
```

### Detailed Flow

#### 1. Tunnel Registration
1. **Client connects** to WebSocket endpoint `/tunnel`
2. **Client sends registration message**:
   ```json
   {
     "type": "register",
     "subdomain": "myapp",
     "authToken": "your-auth-token"
   }
   ```
3. **Server validates** the auth token
4. **TunnelManager creates** a `TunnelConnection` instance
5. **Server responds** with public URL:
   ```json
   {
     "type": "registered",
     "subdomain": "myapp",
     "publicUrl": "http://myapp.yourdomain.com"
   }
   ```

#### 2. HTTP Request Forwarding
1. **End user makes HTTP request** to `http://myapp.yourdomain.com/api/data`
2. **HttpRequestHandler receives** the request
3. **Extracts subdomain** (`myapp`) from the Host header
4. **TunnelManager finds** the corresponding `TunnelConnection`
5. **Creates TunnelRequest** with method, path, headers, and body
6. **TunnelManager.forwardRequest()** handles the request lifecycle:
   - Generates unique `requestId`
   - Stores callback in `pendingRequests` map
   - Calls `TunnelConnection.sendRequest()`
7. **TunnelConnection sends** WebSocket message to client:
   ```json
   {
     "type": "request",
     "requestId": "uuid-123",
     "method": "GET",
     "path": "/api/data",
     "headers": {...},
     "body": "..."
   }
   ```

#### 3. Response Handling
1. **Client processes** the request locally
2. **Client sends response** back via WebSocket:
   ```json
   {
     "type": "response",
     "requestId": "uuid-123",
     "statusCode": 200,
     "headers": {...},
     "body": "..."
   }
   ```
3. **WebSocketHandler receives** the response
4. **TunnelManager.handleTunnelResponse()** processes it:
   - Finds pending request by `requestId`
   - Removes from `pendingRequests` map
   - Executes stored callback
5. **HttpRequestHandler** receives response via callback
6. **Converts to HTTP response** and sends to end user

#### 4. Connection Cleanup
1. **When WebSocket disconnects**, `channelInactive()` is triggered
2. **TunnelManager.removeChannel()** cleans up:
   - Removes tunnel from active tunnels
   - Logs tunnel removal

### Key Components

- **TunnelManager** (Singleton): Central coordinator for all tunnel operations
- **TunnelConnection**: Handles WebSocket communication with clients
- **HttpRequestHandler**: Processes incoming HTTP requests and routes to tunnels
- **WebSocketHandler**: Manages WebSocket connections and message parsing
- **ServerInitializer**: Sets up Netty pipeline with handlers

### Health Check
The server provides a health check endpoint at `/ping` that returns `pong`.

## Usage

1. Start the tunnel server
2. Connect to the WebSocket endpoint at `/tunnel` with your client
3. Register a tunnel with a subdomain and authentication token
4. The server will forward HTTP requests to your client

## Security Considerations

- Always use a strong, unique `staticAuthToken` in production
- Consider using HTTPS in production by setting `TUNNEL_HTTP_SCHEME=https`
- Implement proper access controls and rate limiting for production use