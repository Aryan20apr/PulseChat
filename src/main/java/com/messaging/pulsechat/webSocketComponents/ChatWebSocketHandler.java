package com.messaging.pulsechat.webSocketComponents;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.messaging.pulsechat.model.ChatMessage;
import com.messaging.pulsechat.model.Constants;
import com.messaging.pulsechat.model.TypingEvent;

public class ChatWebSocketHandler extends TextWebSocketHandler{
    
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
                case Constants.TYPING_START:
                case Constants.TYPING_END:
                    handleTypingEvent(chatId, userId, username, messageType);
                    break;
                    
                case Constants.MESSAGE:
                    handleChatMessage(chatId, userId, username, (String) payload.get("content"));
                    break;
                    
                default:
                    System.out.println("Unknown message type: " + messageType);
            }
        } catch (Exception e) {
            System.err.println("Error handling message: " + e.getMessage());
        }
    }
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
        // In real app, extract from JWT token or session attributes
        return session.getAttributes().getOrDefault("userId", "anonymous_" + session.getId()).toString();
    }
    
    private String getUsernameFromSession(WebSocketSession session) {
        return session.getAttributes().getOrDefault("username", "Anonymous").toString();
    }
    
    private String getChatIdFromSession(WebSocketSession session) {
        // Extract from query params or path
        return session.getAttributes().getOrDefault("chatId", "general").toString();
    }
    public void broadcastToChat(String chatId, Object message) {
        // Send message to all users in this chat who are connected to THIS server
        sessions.values().forEach(session -> {
            if (getChatIdFromSession(session).equals(chatId)) {
                try {
                    String json = objectMapper.writeValueAsString(message);
                    session.sendMessage(new TextMessage(json));
                } catch (Exception e) {
                    System.err.println("Error sending message to session: " + e.getMessage());
                }
            }
        });
    }


}
