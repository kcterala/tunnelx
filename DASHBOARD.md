# Real-time Tunnel Dashboard

This branch adds a comprehensive real-time dashboard for monitoring tunnel server activity.

## Features

### 📊 Live Statistics
- **Total Tunnels**: Count of all tunnels created since server start
- **Active Tunnels**: Currently connected tunnel connections
- **Server Uptime**: Time since server was started
- **Request Count**: Total HTTP requests processed

### 🔍 Subdomain Availability Check
- Real-time search to check if a subdomain is available
- Instant feedback on subdomain availability
- Enter subdomain name and press Enter or click Check

### 📋 Active Tunnel List
- Live list of all currently active tunnels
- Shows subdomain name and connection time
- Updates in real-time as tunnels connect/disconnect

### 🔗 Real-time Updates
- WebSocket connection for live data updates
- Auto-refresh every 5 seconds
- Connection status indicator
- Automatic reconnection on disconnection

## How it Works

1. **Main Domain Access**: Visit `tunnel.name.dev` to access the dashboard
2. **WebSocket Connection**: Dashboard connects to `/dashboard-ws` endpoint
3. **Real-time Data**: Server pushes updates via WebSocket messages
4. **Responsive Design**: Works on desktop and mobile devices

## API Endpoints

### WebSocket: `/dashboard-ws`

**Request Types:**
```json
// Get current statistics
{"type": "get_stats"}

// Check subdomain availability  
{"type": "check_availability", "subdomain": "test"}
```

**Response Types:**
```json
// Statistics response
{
  "type": "stats",
  "totalTunnels": 5,
  "activeTunnels": 3,
  "uptime": 3600,
  "requestCount": 1250,
  "tunnels": [
    {
      "subdomain": "api",
      "connectedTime": "Aug 16, 19:30:15"
    }
  ]
}

// Availability check response
{
  "type": "availability_check", 
  "subdomain": "test",
  "available": true
}
```

## Dashboard Features

- **Modern UI**: Clean, professional dashboard design
- **Mobile Responsive**: Works on all screen sizes
- **Live Connection Status**: Shows WebSocket connection state
- **Automatic Updates**: Refreshes data every 5 seconds
- **Search Functionality**: Check subdomain availability instantly

## Access

- **URL**: `tunnel.name.dev` (main domain)
- **Local**: `localhost:8080` (for development)
- **Real-time**: Updates automatically without page refresh
- **No Authentication**: Public dashboard for tunnel status

The dashboard provides complete visibility into your tunnel server's operation in real-time.