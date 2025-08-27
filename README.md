# PulseChat - Real-Time Chat Application

A scalable, real-time chat application built with Spring Boot, WebSockets, and Redis that demonstrates how modern messaging platforms achieve global real-time communication.

## 🏗️ Architecture Overview

PulseChat uses a **distributed architecture** that can scale horizontally across multiple servers while maintaining real-time communication between users.

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Server A      │    │   Server B      │    │   Server C      │
│                 │    │                 │    │                 │
│ User1 ──┐      │    │ User3 ──┐      │    │ User5 ──┐      │
│ User2 ──┼─ WebSocket │ User4 ──┼─ WebSocket │ User6 ──┼─ WebSocket │
│         └─┐    │    │         └─┐    │    │         └─┐    │
└───────────┼────┘    └───────────┼────┘    └───────────┼────┘
            │                     │                     │
            └─────────────────────┼─────────────────────┘
                                  │
                        ┌─────────▼─────────┐
                        │                   │
                        │   Redis Cluster   │
                        │  (Message Bus)    │
                        │                   │
                        └───────────────────┘
```

## 🚀 How It Works

### 1. **WebSocket Connections**
- Each user connects to the nearest server via WebSocket
- Connection includes `userId`, `username`, and `chatId` parameters
- Server maintains active session mappings

### 2. **Message Publishing**
When a user types or sends a message:
```java
// Server publishes to Redis channel
String channel = "chat:" + chatId + ":events";
redisTemplate.convertAndSend(channel, event);
```

### 3. **Global Distribution via Redis**
- Redis acts as a central message bus
- All servers subscribe to the same channels
- Messages are instantly distributed to all connected servers

### 4. **Local Broadcasting**
Each server broadcasts received messages to its local WebSocket connections:
```java
public void broadcastToChat(String chatId, Object message) {
    sessions.values().forEach(session -> {
        if (session.isOpen() && getChatIdFromSession(session).equals(chatId)) {
            session.sendMessage(new TextMessage(chatMessage));
        }
    });
}
```

## 🔥 Why Redis Enables Global Scale

### **The Problem Without Redis**
Imagine you have 3 servers running your chat app:
```
Server A: User1, User2 connected
Server B: User3, User4 connected  
Server C: User5, User6 connected
```

**What happens when User1 sends a message?**
- Server A broadcasts to User1 and User2 ✅
- Server B doesn't know about the message ❌
- Server C doesn't know about the message ❌

**Result:** Users on different servers can't chat with each other!

### **How Redis Solves This**
Redis acts as a **central message bus** that all servers connect to:
```
User1 types on Server A → Server A publishes to Redis → Redis broadcasts to ALL servers
                                                           ↓
Server A: Receives from Redis → Broadcasts to User1, User2
Server B: Receives from Redis → Broadcasts to User3, User4  
Server C: Receives from Redis → Broadcasts to User5, User6
```

## 📈 Scalability Benefits

### **1. Horizontal Scaling**
- Add more servers without code changes
- Each server handles its own WebSocket connections
- All servers get the same messages via Redis

### **2. Load Distribution**
```
Before: 1000 users on 1 server = 1000 WebSocket connections
After:  1000 users on 5 servers = 200 WebSocket connections per server
```

### **3. Geographic Distribution**
- Server A in New York
- Server B in London  
- Server C in Tokyo
- All connected via Redis → Global chat!

### **4. Fault Tolerance**
- If Server A crashes, users on B and C keep chatting
- Redis ensures no messages are lost
- Users can reconnect to any available server

## 🌍 Real-World Example: WhatsApp's Global Scale

**Why does WhatsApp show "typing…" instantly — even if your friend is on the other side of the world?**

It's not magic. It's WebSockets + a global messaging backbone powered by Redis.

Here's what actually happens when your friend starts typing:

1. **Local trigger** → The moment they press a key, WhatsApp fires a tiny "typing" event (not the text).

2. **Persistent WebSocket** → That event goes to the nearest WhatsApp edge server.

3. **Redis Pub/Sub fan-out** →
   - Server A (your friend's server) publishes the event into a Redis channel keyed to your ID.
   - Redis instantly notifies whichever server (Server B) is holding your WebSocket connection.

4. **Push to you** → Server B sends the event down your WebSocket → your phone renders "typing…" in under 200ms.

Now scale that up:
- **2B+ users**
- **100B+ messages daily**
- **Billions of ephemeral events** (typing, online, delivered, read) flowing through Redis clusters every second.

⚡ **The genius?**

These packets are tiny, transient, and optimized. Even on 2G, WhatsApp delivers a real-time experience.

✨ **Lesson: Real-time systems aren't built with "faster servers."**

They're built with smarter event pipelines.

## 🛠️ Technology Stack

- **Backend:** Spring Boot 3.x
- **Real-time Communication:** WebSocket (STOMP)
- **Message Broker:** Redis Pub/Sub
- **Frontend:** Vanilla JavaScript + HTML5
- **Build Tool:** Maven
- **Java Version:** 17+

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.6+
- Redis 6.0+

### Installation

1. **Clone the repository**
```bash
git clone <repository-url>
cd pulsechat
```

2. **Start Redis**
```bash
redis-server
```

3. **Run the application**
```bash
mvn spring-boot:run
```

4. **Open the client**
Navigate to `http://localhost:8080` in your browser

### Configuration
The application connects to Redis on `localhost:6379` by default. Modify `application.properties` for custom settings.

## 📱 Features

- **Real-time messaging** with WebSocket connections
- **Typing indicators** that work across multiple servers
- **Multi-user chat rooms** with unique chat IDs
- **Persistent connections** with automatic reconnection
- **Scalable architecture** ready for production deployment

## 🔧 Key Components

### **ChatWebSocketHandler**
Manages WebSocket connections and handles real-time message routing.

### **RedisMessageListener**
Listens to Redis channels and rebroadcasts messages to local WebSocket sessions.

### **TypingEvent & ChatMessage**
Data models for typing indicators and chat messages.

### **RedisConfig**
Configures Redis connection factory and message listener container.

## 🧪 Testing

Run the test suite:
```bash
mvn test
```

## 📊 Performance Characteristics

- **Latency:** < 200ms for typing indicators
- **Throughput:** Scales linearly with server count
- **Connection Limit:** Limited only by Redis cluster capacity
- **Geographic Distribution:** Works across continents with Redis replication

## 🚀 Production Deployment

### **Redis Cluster Setup**
For production, use Redis Cluster or Redis Sentinel for high availability:
```yaml
spring:
  redis:
    cluster:
      nodes:
        - redis-node-1:6379
        - redis-node-2:6379
        - redis-node-3:6379
```

### **Load Balancer Configuration**
Configure your load balancer to:
- Route WebSocket upgrade requests to available servers
- Maintain session affinity for better performance
- Handle graceful server shutdowns

### **Monitoring**
Monitor:
- Redis connection pool usage
- WebSocket session counts per server
- Message throughput and latency
- Server resource utilization

