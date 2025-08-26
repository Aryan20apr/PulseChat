package com.messaging.pulsechat.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class TypingEvent {
    
    private String chatId;
    private String userId;
    private String username;
    private long timestamp;
    private String type;

    public TypingEvent(String chatId, String userId, String username, String type) {
        this.chatId = chatId;
        this.userId = userId;
        this.username = username;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

}
