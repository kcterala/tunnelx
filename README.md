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

## Usage

1. Start the tunnel server
2. Connect to the WebSocket endpoint at `/tunnel` with your client
3. Register a tunnel with a subdomain and authentication token
4. The server will forward HTTP requests to your client

## Security Considerations

- Always use a strong, unique `staticAuthToken` in production
- Consider using HTTPS in production by setting `TUNNEL_HTTP_SCHEME=https`
- Implement proper access controls and rate limiting for production use