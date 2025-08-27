package com.messaging.pulsechat.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class ChatMessage {
    
    private String chatId;
    private String userId;
    private String username;
    private String content;
    private long timestamp;
    private String type;

    public ChatMessage(String chatId, String userId, String username, String content) {
        this.chatId = chatId;
        this.userId = userId;
        this.username = username;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.type = "message";
    }
}
