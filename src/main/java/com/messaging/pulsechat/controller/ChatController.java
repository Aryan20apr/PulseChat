package com.messaging.pulsechat.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.messaging.pulsechat.model.TypingEvent;

@RestController
@RequestMapping("/api")
public class ChatController {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @PostMapping("/chat/{chatId}/typing")
    public ResponseEntity<?> triggerTyping(@PathVariable String chatId,
                                         @RequestParam String userId,
                                         @RequestParam String type) {
        TypingEvent event = new TypingEvent(chatId, userId, "Test User", type);
        redisTemplate.convertAndSend("chat:" + chatId + ":events", event);
        
        return ResponseEntity.ok("Typing event sent");
    }
}
