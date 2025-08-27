package com.messaging.pulsechat.webSocketComponents;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.messaging.pulsechat.model.ChatMessage;
import com.messaging.pulsechat.model.Constants;
import com.messaging.pulsechat.model.TypingEvent;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Store active WebSocket sessions
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> userSessions = new ConcurrentHashMap<>(); // userId -> sessionId
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = getUserIdFromSession(session);
        String chatId = getChatIdFromSession(session);
        
        sessions.put(session.getId(), session);
        userSessions.put(userId, session.getId());
        
        System.out.println("User " + userId + " connected to chat " + chatId);
        
        // Subscribe to Redis channel for this chat
        subscribeToChat(chatId);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String messageType = (String) payload.get("type");
            String userId = getUserIdFromSession(session);
            String username = getUsernameFromSession(session);
            String chatId = getChatIdFromSession(session);
            
            switch (messageType) {
                case "typing_start":
                case "typing_stop":
                    handleTypingEvent(chatId, userId, username, messageType);
                    break;
                    
                case "message":
                    handleChatMessage(chatId, userId, username, (String) payload.get("content"));
                    break;
                    
                default:
                    System.out.println("Unknown message type: " + messageType);
            }
        } catch (Exception e) {
            System.err.println("Error handling message: " + e.getMessage());
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = getUserIdFromSession(session);
        String chatId = getChatIdFromSession(session);
        
        sessions.remove(session.getId());
        userSessions.remove(userId);
        
        // Send typing stop event when user disconnects
        handleTypingEvent(chatId, userId, getUsernameFromSession(session), "typing_stop");
        
        System.out.println("User " + userId + " disconnected from chat " + chatId);
    }
    
    private void handleTypingEvent(String chatId, String userId, String username, String type) {
        TypingEvent event = new TypingEvent(chatId, userId, username, type);
        
        // Publish to Redis - this is the magic that enables global scale
        String channel = "chat:" + chatId + ":events";
        redisTemplate.convertAndSend(channel, event);
        
        System.out.println("Published typing event: " + type + " for user " + userId + " in chat " + chatId);
    }
    
    private void handleChatMessage(String chatId, String userId, String username, String content) {
        ChatMessage message = new ChatMessage(chatId, userId, username, content);
        
        // Publish message to Redis
        String channel = "chat:" + chatId + ":events";
        redisTemplate.convertAndSend(channel, message);
        
        System.out.println("Published message from " + userId + " in chat " + chatId);
    }
    
    private void subscribeToChat(String chatId) {
        // This method would typically trigger Redis subscription
        // In our setup, the RedisMessageListenerContainer handles this
        System.out.println("Subscribed to chat: " + chatId);
    }
    
    // Helper methods to extract info from WebSocket session
    private String getUserIdFromSession(WebSocketSession session) {
        // First try to get from URI query parameters
        String query = session.getUri().getQuery();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2 && "userId".equals(keyValue[0])) {
                    try {
                        return java.net.URLDecoder.decode(keyValue[1], "UTF-8");
                    } catch (Exception e) {
                        System.err.println("Error decoding userId: " + e.getMessage());
                    }
                }
            }
        }
        
        // Fallback to session attributes
        Object userId = session.getAttributes().get("userId");
        return userId != null ? userId.toString() : "anonymous_" + session.getId();
    }
    
    private String getUsernameFromSession(WebSocketSession session) {
        // First try to get from URI query parameters
        String query = session.getUri().getQuery();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2 && "username".equals(keyValue[0])) {
                    try {
                        return java.net.URLDecoder.decode(keyValue[1], "UTF-8");
                    } catch (Exception e) {
                        System.err.println("Error decoding username: " + e.getMessage());
                    }
                }
            }
        }
        
        // Fallback to session attributes
        Object username = session.getAttributes().get("username");
        return username != null ? username.toString() : "Anonymous";
    }
    
    private String getChatIdFromSession(WebSocketSession session) {
        // First try to get from URI query parameters
        String query = session.getUri().getQuery();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2 && "chatId".equals(keyValue[0])) {
                    try {
                        return java.net.URLDecoder.decode(keyValue[1], "UTF-8");
                    } catch (Exception e) {
                        System.err.println("Error decoding chatId: " + e.getMessage());
                    }
                }
            }
        }
        
        // Fallback to session attributes
        Object chatId = session.getAttributes().get("chatId");
        return chatId != null ? chatId.toString() : "general";
    }
    
    public void broadcastToChat(String chatId, Object message) {
        // Send message to all users in this chat who are connected to THIS server
        sessions.values().forEach(session -> {
            if (session.isOpen() && getChatIdFromSession(session).equals(chatId)) {
                try {
                    String json = objectMapper.writeValueAsString(message);
                    session.sendMessage(new TextMessage(json));
                    System.out.println("Sent message to session: " + session.getId());
                } catch (Exception e) {
                    System.err.println("Error sending message to session: " + e.getMessage());
                }
            }
        });
    }
}