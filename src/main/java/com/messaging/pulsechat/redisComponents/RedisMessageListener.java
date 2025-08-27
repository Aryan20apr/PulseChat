package com.messaging.pulsechat.redisComponents;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.messaging.pulsechat.webSocketComponents.ChatWebSocketHandler;

@Component
public class RedisMessageListener implements MessageListener {

    @Autowired
    private ChatWebSocketHandler chatHandler;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
     
        try{
            String channel = new String(message.getChannel());
            String messageBody = new String(message.getBody());

            // Extract chatId from channel name (chat:12345:events)
            String chatId = channel.split(":")[1];

            // Parse the message
            Map<String, Object> eventData = objectMapper.readValue(messageBody, Map.class);

            // Broadcast to all WebSocket connections on THIS server
            chatHandler.broadcastToChat(chatId, eventData);
            
            System.out.println("Received Redis message for chat " + chatId + ": " + eventData.get("type"));

        } catch(Exception e){
            System.err.println("Error processing Redis message: " + e.getMessage());
        }
       
    }
    
}
